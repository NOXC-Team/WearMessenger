package top.noxc.wmessenger.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.BuildConfig
import top.noxc.wmessenger.R

@Composable
fun ExperimentsScreen(
    quickReplyEnabled: Boolean,
    lightModeEnabled: Boolean,
    notificationsEnabled: Boolean,
    muteAllEnabled: Boolean,
    avatarClearEnabled: Boolean,
    doubleSwipeExitEnabled: Boolean,
    httpAssistantEnabled: Boolean,
    translationEnabled: Boolean,
    messageMenuEnabled: Boolean,
    translationProvider: String,
    onQuickReplyChange: (Boolean) -> Unit,
    onLightModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onMuteAllToggle: (Boolean) -> Unit,
    onAvatarClearChange: (Boolean) -> Unit,
    onDoubleSwipeExitChange: (Boolean) -> Unit,
    onHttpAssistantChange: (Boolean) -> Unit,
    onTranslationChange: (Boolean) -> Unit,
    onMessageMenuChange: (Boolean) -> Unit,
    onTranslationProviderChange: (String) -> Unit,
    onBack: () -> Unit,
    onDisableExperiments: () -> Unit
) {
    val context = LocalContext.current
    var showProviderDialog by remember { mutableStateOf(false) }

    val providerDisplay = when (translationProvider) {
        "google" -> "Google"
        "bing" -> "Bing"
        else -> "Google"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Text(
            text = stringResource(R.string.experiments),
            color = WmTheme.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = WmTheme.dividerStrong)

        if (!BuildConfig.DEBUG) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDisableExperiments()
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        context.startActivity(intent)
                        (context as? Activity)?.finishAffinity()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.disable_experiments),
                    color = WmTheme.errorColor,
                    fontSize = 12.sp
                )
                Text(
                    text = "✕",
                    color = WmTheme.errorColor,
                    fontSize = 14.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.experiments_warning),
            color = WmTheme.warningColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        ExperimentItem(
            title = stringResource(R.string.exp_quick_reply),
            description = stringResource(R.string.exp_quick_reply_desc),
            checked = quickReplyEnabled,
            onCheckedChange = onQuickReplyChange
        )

        ExperimentItem(
            title = stringResource(R.string.exp_light_mode),
            description = stringResource(R.string.exp_light_mode_desc),
            checked = lightModeEnabled,
            onCheckedChange = onLightModeChange
        )

        ExperimentItem(
            title = stringResource(R.string.exp_notifications),
            description = stringResource(R.string.exp_notifications_desc),
            checked = notificationsEnabled,
            onCheckedChange = onNotificationsChange
        )

        if (notificationsEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMuteAllToggle(!muteAllEnabled) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.mute_all_toggle),
                    color = WmTheme.onBackground,
                    fontSize = 12.sp
                )
                Switch(
                    checked = muteAllEnabled,
                    onCheckedChange = onMuteAllToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF2AABEE),
                        checkedThumbColor = Color.White
                    )
                )
            }
            Divider(color = WmTheme.divider)
        }

        ExperimentItem(
            title = stringResource(R.string.exp_avatar_clear),
            description = stringResource(R.string.exp_avatar_clear_desc),
            checked = avatarClearEnabled,
            onCheckedChange = onAvatarClearChange
        )

        ExperimentItem(
            title = stringResource(R.string.exp_double_swipe_exit),
            description = stringResource(R.string.exp_double_swipe_exit_desc),
            checked = doubleSwipeExitEnabled,
            onCheckedChange = onDoubleSwipeExitChange
        )

        ExperimentItem(
            title = "HTTP Assistant",
            description = "Enable HTTP server for remote input (Cloud Password, Proxy config)",
            checked = httpAssistantEnabled,
            onCheckedChange = onHttpAssistantChange
        )

        ExperimentItem(
            title = "Translation",
            description = "Translate messages using Bing or Google (free, no API key)",
            checked = translationEnabled,
            onCheckedChange = onTranslationChange
        )

        if (translationEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProviderDialog = true }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Translation Provider",
                    color = WmTheme.onBackground,
                    fontSize = 12.sp
                )
                Text(
                    text = providerDisplay,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        ExperimentItem(
            title = "Message Menu",
            description = "Tap message to show Copy/Delete/Translate menu",
            checked = messageMenuEnabled,
            onCheckedChange = onMessageMenuChange
        )
    }

    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = { Text("Translation Provider", color = Color.White, fontSize = 14.sp) },
            buttons = {
                Column {
                    Divider(color = Color(0xFF333333))
                    listOf("google" to "Google", "bing" to "Bing").forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTranslationProviderChange(value)
                                    showProviderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, fontSize = 13.sp)
                            if (translationProvider == value) {
                                Text("✓", color = Color(0xFF2AABEE), fontSize = 14.sp)
                            }
                        }
                        Divider(color = WmTheme.divider)
                    }
                }
            }
        )
    }
}

@Composable
private fun ExperimentItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = WmTheme.onBackground,
                fontSize = 13.sp
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF2AABEE),
                    checkedThumbColor = Color.White
                )
            )
        }
        Text(
            text = description,
            color = WmTheme.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    Divider(color = WmTheme.divider)
}
