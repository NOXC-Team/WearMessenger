package top.noxc.wmessenger.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import top.noxc.wmessenger.MainActivity
import top.noxc.wmessenger.R

class WearNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "wearmessenger_messages"
        const val REPLY_ACTION = "top.noxc.wmessenger.REPLY"
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_ACCOUNT_INDEX = "account_index"
        const val EXTRA_REPLY_TEXT = "reply_text"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifyMessage(
        notificationId: Int,
        chatId: Long,
        accountIndex: Int,
        chatTitle: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean,
        isLocked: Boolean = false
    ) {
        Log.d("WearNotification", "notifyMessage called: id=$notificationId, chat=$chatTitle, text=$messageText")
        try {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_chat_id", chatId)
                putExtra("open_account", accountIndex)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (isGroup && senderName.isNotEmpty()) "$chatTitle: $senderName" else chatTitle
            val displayText = if (isLocked) "[Message]" else messageText

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(displayText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            if (!isLocked) {
                val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
                    action = REPLY_ACTION
                    putExtra(EXTRA_CHAT_ID, chatId)
                    putExtra(EXTRA_ACCOUNT_INDEX, accountIndex)
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 1000000,
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                val remoteInput = RemoteInput.Builder(EXTRA_REPLY_TEXT).run {
                    setLabel("Reply")
                    build()
                }

                val replyAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    "Reply",
                    replyPendingIntent
                ).addRemoteInput(remoteInput).build()

                builder.addAction(replyAction)
            }

            val notification = builder.build()

            notificationManager.notify(notificationId, notification)
            Log.d("WearNotification", "Notification posted with id=$notificationId")
        } catch (e: Exception) {
            Log.e("WearNotification", "Failed to create notification", e)
        }
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}

class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WearNotificationManager.REPLY_ACTION) return

        val chatId = intent.getLongExtra(WearNotificationManager.EXTRA_CHAT_ID, 0L)
        val accountIndex = intent.getIntExtra(WearNotificationManager.EXTRA_ACCOUNT_INDEX, 0)

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(WearNotificationManager.EXTRA_REPLY_TEXT)?.toString() ?: return

        val prefs = context.getSharedPreferences("pending_reply", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("text", replyText)
            .putLong("chat_id", chatId)
            .putInt("account_index", accountIndex)
            .putLong("timestamp", System.currentTimeMillis())
            .apply()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(chatId.toInt())

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", chatId)
            putExtra("open_account", accountIndex)
            putExtra("send_reply", true)
        }
        context.startActivity(openIntent)
    }
}
