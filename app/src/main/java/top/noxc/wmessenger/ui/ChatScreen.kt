package top.noxc.wmessenger.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.animation.core.*
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.BotCommandItem
import top.noxc.wmessenger.core.InlineButtonItem
import top.noxc.wmessenger.core.KeyboardButtonItem
import top.noxc.wmessenger.core.MessageItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    chatTitle: String,
    messages: List<MessageItem>,
    canSend: Boolean,
    botCommands: List<BotCommandItem>,
    replyKeyboard: List<List<KeyboardButtonItem>>,
    inlineKeyboard: Triple<Long, Long, List<List<InlineButtonItem>>>,
    isOnline: Boolean,
    isTyping: Boolean,
    isLoadingHistory: Boolean,
    quickReplyEnabled: Boolean = false,
    translationEnabled: Boolean = false,
    messageMenuEnabled: Boolean = false,
    translationProvider: String = "google",
    translationConsentGiven: Boolean = false,
    pendingReplyText: String? = null,
    onClearPendingReply: () -> Unit = {},
    onTranslationProviderChange: (String) -> Unit = {},
    onTranslationConsentChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendMessageReply: (Long, String) -> Unit = { _, _ -> },
    onLoadMore: () -> Unit,
    onInlineButtonClick: (Long, Long, ByteArray) -> Unit,
    onTyping: () -> Unit,
    onAttachCamera: () -> Unit = {},
    onAttachPhoto: () -> Unit = {},
    onAttachVideo: () -> Unit = {},
    onTranslateMessage: (Long, String) -> Unit = { _, _ -> },
    onUndoTranslation: (Long) -> Unit = {},
    onCopyMessage: (String) -> Unit = {},
    onDeleteMessage: (Long) -> Unit = {}
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var previousSize by remember(chatTitle) { mutableStateOf(0) }
    var hasScrolledToBottom by remember(chatTitle) { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var showBotCommands by remember { mutableStateOf(false) }
    var showReplyKeyboard by remember(chatTitle) { mutableStateOf(false) }
    var firstVisibleIndexBeforeLoad by remember { mutableStateOf(0) }
    var sizeAtLoadRequest by remember { mutableStateOf(-1) }

    var lastTypingSentTime by remember { mutableStateOf(0L) }

    var replyingTo by remember { mutableStateOf<MessageItem?>(null) }
    var messageKeyboardMsg by remember { mutableStateOf<MessageItem?>(null) }
    var messageKeyboardTimer by remember { mutableStateOf<Long>(0L) }
    var translatedMessages by remember(chatTitle) { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var translatingMessageId by remember { mutableStateOf<Long?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var pendingTranslationMsgId by remember { mutableStateOf<Long?>(null) }
    var pendingTranslationText by remember { mutableStateOf<String?>(null) }
    var countdownSeconds by remember { mutableStateOf(5) }

    LaunchedEffect(chatTitle) {
        translatedMessages = emptyMap()
        translatingMessageId = null
    }

    LaunchedEffect(translatingMessageId) {
        if (translatingMessageId != null) {
            val msg = messages.find { it.id == translatingMessageId }
            if (msg != null && msg.text.isNotEmpty()) {
                val result = top.noxc.wmessenger.core.TranslationService.translate(
                    msg.text, translationProvider
                )
                translatedMessages = translatedMessages + (translatingMessageId!! to result)
                translatingMessageId = null
            }
        }
    }

    LaunchedEffect(showPrivacyDialog) {
        if (showPrivacyDialog) {
            countdownSeconds = 5
            for (i in 4 downTo 0) {
                kotlinx.coroutines.delay(1000)
                countdownSeconds = i
            }
        }
    }

    LaunchedEffect(pendingReplyText) {
        if (!pendingReplyText.isNullOrEmpty()) {
            onSendMessage(pendingReplyText)
            onClearPendingReply()
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 监听列表滚动状态，当接近顶部时自动加载更多
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex <= 2 && sizeAtLoadRequest == -1 && isLoadingHistory.not()) {
            firstVisibleIndexBeforeLoad = listState.firstVisibleItemIndex
            sizeAtLoadRequest = messages.size
            onLoadMore()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (!hasScrolledToBottom) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }
                    .first { it >= messages.size }
                listState.scrollToItem(messages.size - 1)
                hasScrolledToBottom = true
            } else if (sizeAtLoadRequest >= 0 && messages.size > sizeAtLoadRequest) {
                val diff = messages.size - sizeAtLoadRequest
                listState.scrollToItem(firstVisibleIndexBeforeLoad + diff)
                sizeAtLoadRequest = -1
            } else if (messages.size > previousSize && previousSize > 0) {
                if (isLoadingHistory) {
                    val diff = messages.size - previousSize
                    listState.scrollToItem(listState.firstVisibleItemIndex + diff)
                } else {
                    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleIndex >= previousSize - 2) {
                        listState.scrollToItem(messages.size - 1)
                    }
                }
            } else if (sizeAtLoadRequest >= 0 && messages.size == sizeAtLoadRequest && !isLoadingHistory) {
                // No new messages loaded, reset the request
                sizeAtLoadRequest = -1
            }
            previousSize = messages.size
        }
    }

    LaunchedEffect(chatTitle) {
        // Reset state when opening a new chat
        hasScrolledToBottom = false
        sizeAtLoadRequest = -1
        previousSize = 0
        
        // Wait a bit and if still no messages, try to load more
        kotlinx.coroutines.delay(1000)
        if (messages.isEmpty()) {
            sizeAtLoadRequest = 0
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleColor = when {
                    isTyping -> if (WmTheme.isLight) Color(0xFF0FB297).copy(alpha = blinkAlpha) else Color(0xFF2AABEE).copy(alpha = blinkAlpha)
                    isOnline -> WmTheme.onBackground
                    else -> WmTheme.textHint
                }
                Text(
                    text = chatTitle,
                    color = titleColor,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }

            Divider(color = WmTheme.dividerStrong)

            if (messageKeyboardMsg != null && messageMenuEnabled) {
                MessageKeyboard(
                    msg = messageKeyboardMsg!!,
                    translationEnabled = translationEnabled,
                    isTranslated = translatedMessages.containsKey(messageKeyboardMsg!!.id),
                    onCopy = { onCopyMessage(it) },
                    onDelete = { onDeleteMessage(it) },
                    onTranslate = { msgId, text ->
                        if (translatedMessages.containsKey(msgId)) {
                            onUndoTranslation(msgId)
                            translatedMessages = translatedMessages - msgId
                        } else if (translationConsentGiven) {
                            translatingMessageId = msgId
                        } else {
                            pendingTranslationMsgId = msgId
                            pendingTranslationText = text
                            showPrivacyDialog = true
                        }
                    },
                    onDismiss = { messageKeyboardMsg = null }
                )
                Divider(color = WmTheme.dividerStrong)
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet", color = WmTheme.textSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (isLoadingHistory) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                        val nextMsg = if (index < messages.size - 1) messages[index + 1] else null
                        val showTime = shouldShowTime(msg, nextMsg)
                        val displayText = translatedMessages[msg.id] ?: msg.text
                        val isShowingTranslation = translatedMessages.containsKey(msg.id)
                        MessageBubble(
                            msg = msg,
                            showTime = showTime,
                            quickReplyEnabled = quickReplyEnabled,
                            translationEnabled = translationEnabled,
                            displayText = displayText,
                            isShowingTranslation = isShowingTranslation,
                            onDoubleTap = {
                                if (quickReplyEnabled) {
                                    replyingTo = msg
                                }
                            },
                            onTap = {
                                messageKeyboardMsg = msg
                                messageKeyboardTimer = System.currentTimeMillis()
                            }
                        )
                    }
                }
            }

            Divider(color = WmTheme.dividerStrong)

            if (canSend) {
                if (replyingTo != null) {
                    Surface(
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(Color(0xFF2AABEE))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = replyingTo?.senderName ?: "",
                                    color = Color(0xFF2AABEE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = replyingTo?.text ?: "",
                                    color = WmTheme.textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingTo = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("\u2715", color = WmTheme.textHint, fontSize = 12.sp)
                            }
                        }
                    }
                    Divider(color = WmTheme.dividerStrong)
                }

                if (showBotCommands && botCommands.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            itemsIndexed(botCommands) { _, cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSendMessage("/${cmd.command}")
                                            showBotCommands = false
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "/${cmd.command}",
                                        color = Color(0xFF2AABEE),
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = cmd.description,
                                        color = WmTheme.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Divider(color = WmTheme.dividerStrong)
                }

                if (showReplyKeyboard && replyKeyboard.isNotEmpty()) {
                    BotReplyKeyboard(
                        keyboard = replyKeyboard,
                        onButtonClick = { text ->
                            onSendMessage(text)
                            showReplyKeyboard = false
                        },
                        onDismiss = { showReplyKeyboard = false }
                    )
                }

                if (replyKeyboard.isNotEmpty() && !showReplyKeyboard) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReplyKeyboard = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF2AABEE))
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (botCommands.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showBotCommands = !showBotCommands
                                showReplyKeyboard = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = "/",
                                color = if (showBotCommands) Color(0xFF2AABEE) else WmTheme.textHint,
                                fontSize = 16.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = {
                            messageInput = it
                            val now = System.currentTimeMillis()
                            if (now - lastTypingSentTime > 5000) {
                                lastTypingSentTime = now
                                onTyping()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 40.dp),
                        placeholder = { Text("Message", color = WmTheme.textHint, fontSize = 11.sp) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = WmTheme.onBackground,
                            focusedBorderColor = Color(0xFF2AABEE),
                            unfocusedBorderColor = WmTheme.textHint,
                            cursorColor = Color(0xFF2AABEE)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    val replyMsg = replyingTo
                                    if (replyMsg != null) {
                                        onSendMessageReply(replyMsg.id, messageInput.trim())
                                        replyingTo = null
                                    } else {
                                        onSendMessage(messageInput.trim())
                                    }
                                    messageInput = ""
                                }
                            }
                        ),
                        maxLines = 1,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAttachDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wm_ic_attach),
                            contentDescription = "Attach",
                            tint = Color(0xFF2AABEE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showAttachDialog) {
                        AlertDialog(
                            onDismissRequest = { showAttachDialog = false },
                            title = { Text("Attach", color = Color.White, fontSize = 14.sp) },
                            buttons = {
                                Column {
                                    Divider(color = Color(0xFF333333))
                                    listOf(
                                        "Camera" to { onAttachCamera(); showAttachDialog = false },
                                        //"Gallery" to { onAttachPhoto(); showAttachDialog = false }
                                    ).forEach { (label, action) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { action() }
                                                .padding(vertical = 10.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, color = Color.White, fontSize = 13.sp)
                                        }
                                        Divider(color = WmTheme.divider)
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val replyMsg = replyingTo
                                if (replyMsg != null) {
                                    onSendMessageReply(replyMsg.id, messageInput.trim())
                                    replyingTo = null
                                } else {
                                    onSendMessage(messageInput.trim())
                                }
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wm_ic_send),
                            contentDescription = "Send",
                            tint = if (messageInput.isNotBlank()) Color(0xFF2AABEE) else WmTheme.textHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Read-only", color = WmTheme.textHint, fontSize = 11.sp)
                }
            }
        }
    }

    if (showPrivacyDialog) {
        TranslationPrivacyDialog(
            countdownSeconds = countdownSeconds,
            onConfirm = {
                onTranslationConsentChange(true)
                showPrivacyDialog = false
                pendingTranslationMsgId?.let { msgId ->
                    translatingMessageId = msgId
                }
                pendingTranslationMsgId = null
                pendingTranslationText = null
            },
            onDismiss = {
                showPrivacyDialog = false
                pendingTranslationMsgId = null
                pendingTranslationText = null
            }
        )
    }
}

@Composable
fun TranslationPrivacyDialog(
    countdownSeconds: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val res = context.resources
    var offsetX by remember { mutableStateOf(0f) }
    val dismissThreshold = 100f

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > dismissThreshold) {
                                onDismiss()
                            } else {
                                offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        if (dragAmount > 0) {
                            offsetX = (offsetX + dragAmount).coerceAtLeast(0f)
                        }
                    }
                }
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        res.getString(R.string.translation_privacy_title),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = Color(0xFF333333))

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .heightIn(max = 200.dp)
                ) {
                    Text(
                        res.getString(R.string.translation_privacy_desc),
                        color = Color(0xFFCCCCCC),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        res.getString(R.string.translation_privacy_policies),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            QrCodeImage(
                                data = "https://policies.google.com/privacy",
                                size = 80.dp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Google", color = Color(0xFF2AABEE), fontSize = 9.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            QrCodeImage(
                                data = "https://privacy.microsoft.com/privacystatement",
                                size = 80.dp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Microsoft Bing", color = Color(0xFF2AABEE), fontSize = 9.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        res.getString(R.string.translation_privacy_acknowledge),
                        color = Color(0xFFFFB74D),
                        fontSize = 9.sp,
                        lineHeight = 13.sp
                    )
                }

                Divider(color = Color(0xFF333333))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (countdownSeconds > 0) {
                        Text(
                            res.getString(R.string.translation_privacy_wait, countdownSeconds),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            res.getString(R.string.translation_privacy_proceed),
                            color = Color(0xFF81C784),
                            fontSize = 11.sp
                        )
                    }
                }

                Divider(color = Color(0xFF333333))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (countdownSeconds <= 0) onConfirm()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = res.getString(R.string.translation_privacy_enable),
                        color = if (countdownSeconds <= 0) Color(0xFFFF4444) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QrCodeImage(data: String, size: Dp) {
    val bitmap = remember(data) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }
            android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code",
            modifier = Modifier.size(size)
        )
    } else {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text("QR", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun MessageBubble(
    msg: MessageItem,
    showTime: Boolean,
    quickReplyEnabled: Boolean = false,
    translationEnabled: Boolean = false,
    displayText: String = "",
    isShowingTranslation: Boolean = false,
    onDoubleTap: () -> Unit,
    onTap: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.8f

    val alignment = if (msg.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (msg.isOutgoing) WmTheme.bubbleOutgoing else WmTheme.bubbleIncoming

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .wrapContentWidth(if (msg.isOutgoing) Alignment.End else Alignment.Start)
                .clickable { onTap() }
                .then(
                    if (quickReplyEnabled) {
                        Modifier.pointerInput(Unit) {
                            var lastTapTime = 0L
                            detectTapGestures(
                                onDoubleTap = {
                                    onDoubleTap()
                                    lastTapTime = 0L
                                },
                                onTap = {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime < 300) {
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = now
                                        onTap()
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier.clickable { onTap() }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!msg.isOutgoing && msg.senderName.isNotEmpty()) {
                    Text(
                        text = msg.senderName,
                        color = Color(0xFF2AABEE),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                }

                if (msg.photoLocalPath != null) {
                    val file = File(msg.photoLocalPath)
                    if (file.exists()) {
                        val bitmap = remember(msg.photoLocalPath) {
                            BitmapFactory.decodeFile(msg.photoLocalPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .widthIn(max = maxBubbleWidth - 16.dp)
                                    .heightIn(max = 180.dp),
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    } else {
                        Surface(
                            color = WmTheme.dividerStrong,
                            modifier = Modifier
                                .widthIn(max = maxBubbleWidth - 16.dp)
                                .height(120.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Loading photo...", color = WmTheme.textHint, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (msg.videoLocalPath != null) {
                    val file = File(msg.videoLocalPath)
                    if (file.exists()) {
                        VideoThumbnail(msg, file, maxBubbleWidth - 16.dp)
                    } else {
                        VideoPlaceholder(msg, maxBubbleWidth - 16.dp)
                    }
                } else if (msg.videoFileId != null && msg.photoLocalPath == null) {
                    VideoPlaceholder(msg, maxBubbleWidth - 16.dp)
                }

                if (msg.text.isNotEmpty()) {
                    val textColor = if (isShowingTranslation) Color(0xFF81C784) else WmTheme.bubbleText
                    if (msg.isOutgoing) {
                        Column(modifier = Modifier.wrapContentWidth()) {
                            SelectionContainer {
                                Text(
                                    text = displayText,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                if (showTime) {
                                    Text(
                                        text = formatMessageTime(msg.date),
                                        color = WmTheme.textHint,
                                        fontSize = 9.sp
                                    )
                                    Spacer(Modifier.width(2.dp))
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.wm_ic_double_check),
                                    contentDescription = if (msg.isRead) "Read" else "Sent",
                                    tint = if (msg.isRead) Color(0xFF2AABEE) else WmTheme.textHint,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.wrapContentWidth()) {
                            SelectionContainer {
                                Text(
                                    text = displayText,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                            if (showTime) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = formatMessageTime(msg.date),
                                        color = WmTheme.textHint,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (showTime) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Spacer(Modifier.weight(1f))
                        if (msg.isOutgoing) {
                            Text(
                                text = formatMessageTime(msg.date),
                                color = WmTheme.textHint,
                                fontSize = 9.sp
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.wm_ic_double_check),
                                contentDescription = if (msg.isRead) "Read" else "Sent",
                                tint = if (msg.isRead) Color(0xFF2AABEE) else WmTheme.textHint,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = formatMessageTime(msg.date),
                                color = WmTheme.textHint,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shouldShowTime(current: MessageItem, next: MessageItem?): Boolean {
    if (next == null) return true
    val currentTime = current.date / 60
    val nextTime = next.date / 60
    return currentTime != nextTime
}

private fun formatMessageTime(timestamp: Int): String {
    val date = Date(timestamp.toLong() * 1000)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

@Composable
fun InlineKeyboardPanel(
    chatId: Long,
    messageId: Long,
    rows: List<List<InlineButtonItem>>,
    onInlineButtonClick: (Long, Long, ByteArray) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { btn ->
                    OutlinedButton(
                        onClick = {
                            btn.callbackData?.let { data ->
                                onInlineButtonClick(chatId, messageId, data)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = Color(0xFF2AABEE)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2AABEE))
                    ) {
                        Text(btn.text, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun VideoThumbnail(msg: MessageItem, file: File, maxWidth: Dp) {
    val context = LocalContext.current
    val bitmap = remember(msg.videoLocalPath) {
        try {
            BitmapFactory.decodeFile(msg.videoLocalPath)
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        color = WmTheme.dividerStrong,
        modifier = Modifier
            .widthIn(max = maxWidth)
            .heightIn(max = 200.dp)
            .clickable {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open video with"))
            },
        shape = MaterialTheme.shapes.small
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Video",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0x80000000),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("▶", color = Color.White, fontSize = 32.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun VideoPlaceholder(msg: MessageItem, maxWidth: Dp) {
    Surface(
        color = Color(0xFF333333),
        modifier = Modifier
            .widthIn(max = maxWidth)
            .height(120.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▶", color = Color(0xFF2AABEE), fontSize = 24.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Video ${msg.videoWidth}x${msg.videoHeight}",
                    color = WmTheme.textHint,
                    fontSize = 11.sp
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageKeyboard(
    msg: MessageItem,
    translationEnabled: Boolean,
    isTranslated: Boolean,
    onCopy: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onTranslate: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFF1C1C1C),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { onDismiss() }
                    .padding(bottom = 6.dp, top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF555555))
                )
            }

            val buttons = mutableListOf<String>()
            buttons.add("Copy")
            buttons.add("Delete")
            if (translationEnabled && msg.text.isNotEmpty()) {
                buttons.add(if (isTranslated) "Undo Translation" else "Translate")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                buttons.forEach { btn ->
                    Surface(
                        onClick = {
                            when (btn) {
                                "Copy" -> onCopy(msg.text)
                                "Delete" -> onDelete(msg.id)
                                "Translate", "Undo Translation" -> onTranslate(msg.id, msg.text)
                            }
                            onDismiss()
                        },
                        color = when (btn) {
                            "Delete" -> Color(0xFF3A1A1A)
                            "Undo Translation" -> Color(0xFF1A2A1A)
                            else -> Color(0xFF2A2A2A)
                        },
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                btn,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = when (btn) {
                                    "Delete" -> Color(0xFFFF6B6B)
                                    "Undo Translation" -> Color(0xFF81C784)
                                    else -> Color.White
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
