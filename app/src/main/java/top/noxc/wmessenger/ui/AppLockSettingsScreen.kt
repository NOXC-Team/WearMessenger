package top.noxc.wmessenger.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.R

@Composable
fun AppLockSettingsScreen(
    isAppLockEnabled: Boolean,
    autoLockTimeout: Int,
    onBack: () -> Unit,
    onAppLockToggle: (Boolean) -> Unit,
    onAppLockSet: () -> Unit,
    onAutoLockTimeoutChange: (Int) -> Unit
) {
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val appLockLabel = stringResource(R.string.app_lock)
    val timeoutDescLabel = stringResource(R.string.app_lock_timeout_desc)
    val changePinLabel = stringResource(R.string.change_pin)
    val disablePinLabel = stringResource(R.string.disable_pin)
    val immediatelyLabel = stringResource(R.string.immediately)
    val secondsLabel = stringResource(R.string.seconds)
    val minuteLabel = stringResource(R.string.minute)
    val minutesLabel = stringResource(R.string.minutes)
    val secLabel = stringResource(R.string.seconds_short)
    val minLabel = stringResource(R.string.minutes_short)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) onBack()
                }
            }
    ) {
        Text(
            text = appLockLabel,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = Color(0xFF333333))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isAppLockEnabled) {
                        onAppLockToggle(false)
                    } else {
                        onAppLockSet()
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appLockLabel,
                color = Color.White,
                fontSize = 13.sp
            )
            Switch(
                checked = isAppLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onAppLockSet()
                    } else {
                        onAppLockToggle(false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF2AABEE),
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF333333),
                    uncheckedThumbColor = Color.Gray
                ),
                modifier = Modifier.height(24.dp)
            )
        }
        Divider(color = Color(0xFF222222))

        if (isAppLockEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimeoutDialog = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeoutDescLabel,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    text = formatTimeout(autoLockTimeout, immediatelyLabel, secLabel, minLabel),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Divider(color = Color(0xFF222222))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppLockSet() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = changePinLabel,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    text = "→",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Divider(color = Color(0xFF222222))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppLockToggle(false) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = disablePinLabel,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp
                )
            }
            Divider(color = Color(0xFF222222))
        }
    }

    if (showTimeoutDialog) {
        TimeoutDialog(
            currentTimeout = autoLockTimeout,
            onSelect = {
                onAutoLockTimeoutChange(it)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false },
            immediatelyLabel = immediatelyLabel,
            secondsLabel = secondsLabel,
            minuteLabel = minuteLabel,
            minutesLabel = minutesLabel
        )
    }
}

@Composable
fun TimeoutDialog(
    currentTimeout: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    immediatelyLabel: String,
    secondsLabel: String,
    minuteLabel: String,
    minutesLabel: String
) {
    val autoLockTimeoutLabel = stringResource(R.string.auto_lock_timeout)

    val options = listOf(
        0 to immediatelyLabel,
        5 to "5 $secondsLabel",
        15 to "15 $secondsLabel",
        30 to "30 $secondsLabel",
        60 to "1 $minuteLabel",
        300 to "5 $minutesLabel"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1A2A3A),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = autoLockTimeoutLabel,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == currentTimeout,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF2AABEE),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (value == currentTimeout) Color.White else Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeout(seconds: Int, immediately: String, sec: String, min: String): String {
    return when {
        seconds == 0 -> immediately
        seconds < 60 -> "$seconds $sec"
        seconds == 60 -> "1 $min"
        seconds % 60 == 0 -> "${seconds / 60} $min"
        else -> "${seconds / 60} $min ${seconds % 60} $sec"
    }
}
