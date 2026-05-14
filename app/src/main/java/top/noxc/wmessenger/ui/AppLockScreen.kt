package top.noxc.wmessenger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.noxc.wmessenger.R

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    wrongAttemptCount: Int = 0,
    onSaveWrongAttemptCount: (Int) -> Unit = {},
    lockUntilTimestamp: Long = 0,
    onSaveLockUntilTimestamp: (Long) -> Unit = {},
    clearDataOn10Wrong: Boolean = false,
    onClearData: () -> Unit = {},
    recoveryCooldownEnd: Long = 0,
    logoutCooldownEnd: Long = 0,
    onSendRecoveryToBot: () -> Unit = {},
    onVerifyRecoveryPin: (String) -> Boolean = { false },
    onResetRecoveryAttempts: () -> Unit = {},
    onLogoutAllAccounts: () -> Boolean = { false }
) {
    BackHandler(enabled = true) {}

    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var penaltySeconds by remember { mutableStateOf(0) }
    var isPenaltyActive by remember { mutableStateOf(false) }
    var currentAttemptCount by remember { mutableStateOf(wrongAttemptCount) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showRecoveryInputDialog by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf("") }

    val enterPinLabel = stringResource(R.string.enter_pin)
    val wrongPasswordLabel = stringResource(R.string.wrong_password)
    val lockedLabel = stringResource(R.string.app_lock_locked)
    val secondsLabel = stringResource(R.string.seconds_short)
    val forgotPasswordLabel = stringResource(R.string.forgot_password)
    val forgotPasswordTitle = stringResource(R.string.forgot_password_title)
    val forgotPasswordDesc = stringResource(R.string.forgot_password_desc)
    val recoverViaBot = stringResource(R.string.recover_via_bot)
    val recoverViaBotDesc = stringResource(R.string.recover_via_bot_desc)
    val logoutAllAccounts = stringResource(R.string.logout_all_accounts)
    val logoutAllAccountsDesc = stringResource(R.string.logout_all_accounts_desc)
    val enterRecoveryPin = stringResource(R.string.enter_recovery_pin)
    val recoveryPinHint = stringResource(R.string.recovery_pin_hint)
    val recoverySuccess = stringResource(R.string.recovery_success)
    val recoveryFailed = stringResource(R.string.recovery_failed)
    val recoveryAttemptsExhausted = stringResource(R.string.recovery_attempts_exhausted)
    val recoveryCooldownActive = stringResource(R.string.recovery_cooldown_active)
    val logoutCooldownActive = stringResource(R.string.logout_cooldown_active)
    val logoutConfirmed = stringResource(R.string.logout_confirmed)
    val cancelLabel = stringResource(R.string.cancel)
    val confirmLabel = stringResource(R.string.confirm)

    fun calculatePenalty(attempts: Int): Int {
        return when {
            attempts <= 3 -> 0
            attempts <= 5 -> 5
            attempts <= 7 -> 30
            else -> {
                val n = attempts - 8
                var penalty = 30
                for (i in 0 until n) {
                    penalty *= 2
                    if (penalty >= 21600) {
                        penalty = 21600
                        break
                    }
                }
                penalty
            }
        }
    }

    val now = System.currentTimeMillis()
    val initialRemaining = if (lockUntilTimestamp > now) ((lockUntilTimestamp - now) / 1000).toInt() else 0

    LaunchedEffect(initialRemaining) {
        if (initialRemaining > 0) {
            isPenaltyActive = true
            var remaining = initialRemaining
            while (remaining > 0) {
                penaltySeconds = remaining
                delay(1000)
                remaining--
                onSaveLockUntilTimestamp(System.currentTimeMillis() + remaining * 1000)
            }
            isPenaltyActive = false
            penaltySeconds = 0
            onSaveLockUntilTimestamp(0)
        }
    }

    var triggerPenalty by remember { mutableStateOf(0) }

    LaunchedEffect(triggerPenalty) {
        if (triggerPenalty > 0) {
            val penalty = calculatePenalty(triggerPenalty)
            if (penalty > 0) {
                val lockUntil = System.currentTimeMillis() + penalty * 1000
                onSaveLockUntilTimestamp(lockUntil)
                isPenaltyActive = true
                var remaining = penalty
                while (remaining > 0) {
                    penaltySeconds = remaining
                    delay(1000)
                    remaining--
                    onSaveLockUntilTimestamp(System.currentTimeMillis() + remaining * 1000)
                }
                isPenaltyActive = false
                penaltySeconds = 0
                onSaveLockUntilTimestamp(0)
            }
        }
    }

    fun startPenalty(attempts: Int) {
        triggerPenalty = attempts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            painter = androidx.compose.ui.res.painterResource(R.drawable.wm_ic_lock),
            contentDescription = null,
            tint = Color(0xFF2AABEE),
            modifier = Modifier.size(36.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = enterPinLabel,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (showError) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = wrongPasswordLabel,
                color = Color(0xFFFF5252),
                fontSize = 12.sp
            )
        }

        if (isPenaltyActive) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$lockedLabel ${penaltySeconds}$secondsLabel",
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))

        PinDots(pinLength = pin.length, totalLength = 4)

        Spacer(Modifier.weight(1f))

        PinKeyboard(
            enabled = !isPenaltyActive,
            onDigit = {
                if (pin.length < 4 && !isPenaltyActive) {
                    pin += it
                    showError = false
                    if (pin.length == 4) {
                        if (onVerifyPin(pin)) {
                            onSaveWrongAttemptCount(0)
                            onSaveLockUntilTimestamp(0)
                            onUnlock()
                        } else {
                            currentAttemptCount++
                            onSaveWrongAttemptCount(currentAttemptCount)
                            showError = true
                            pin = ""

                            startPenalty(currentAttemptCount)

                            if (clearDataOn10Wrong && currentAttemptCount >= 10) {
                                onClearData()
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (pin.isNotEmpty() && !isPenaltyActive) {
                    pin = pin.dropLast(1)
                    showError = false
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = forgotPasswordLabel,
            color = Color(0xFF2AABEE),
            fontSize = 12.sp,
            modifier = Modifier.clickable { showForgotPasswordDialog = true }
        )

        Spacer(Modifier.height(16.dp))
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordScreen(
            recoveryCooldownEnd = recoveryCooldownEnd,
            logoutCooldownEnd = logoutCooldownEnd,
            onRecoverViaBot = {
                showForgotPasswordDialog = false
                showRecoveryInputDialog = true
                onSendRecoveryToBot()
            },
            onLogoutAllAccounts = {
                val success = onLogoutAllAccounts()
                if (success) {
                    showForgotPasswordDialog = false
                }
            },
            onDismiss = { showForgotPasswordDialog = false },
            recoverViaBot = recoverViaBot,
            recoverViaBotDesc = recoverViaBotDesc,
            logoutAllAccounts = logoutAllAccounts,
            logoutAllAccountsDesc = logoutAllAccountsDesc,
            recoveryCooldownActive = recoveryCooldownActive,
            logoutCooldownActive = logoutCooldownActive,
            cancelLabel = cancelLabel
        )
    }

    if (showRecoveryInputDialog) {
        RecoveryInputDialog(
            recoveryError = recoveryError,
            onConfirm = { pin ->
                if (onVerifyRecoveryPin(pin)) {
                    showRecoveryInputDialog = false
                    onResetRecoveryAttempts()
                    onSaveWrongAttemptCount(0)
                    onSaveLockUntilTimestamp(0)
                    onUnlock()
                } else {
                    recoveryError = "PIN 错误"
                }
            },
            onDismiss = {
                showRecoveryInputDialog = false
                recoveryError = ""
            },
            enterRecoveryPin = enterRecoveryPin,
            recoveryPinHint = recoveryPinHint,
            cancelLabel = cancelLabel,
            confirmLabel = confirmLabel
        )
    }
}

@Composable
fun ForgotPasswordScreen(
    recoveryCooldownEnd: Long,
    logoutCooldownEnd: Long,
    onRecoverViaBot: () -> Unit,
    onLogoutAllAccounts: () -> Unit,
    onDismiss: () -> Unit,
    recoverViaBot: String,
    recoverViaBotDesc: String,
    logoutAllAccounts: String,
    logoutAllAccountsDesc: String,
    recoveryCooldownActive: String,
    logoutCooldownActive: String,
    cancelLabel: String
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val recoveryCooldownRemaining = if (recoveryCooldownEnd > currentTime) recoveryCooldownEnd - currentTime else 0
    val logoutCooldownRemaining = if (logoutCooldownEnd > currentTime) logoutCooldownEnd - currentTime else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = recoverViaBotDesc,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            if (recoveryCooldownRemaining > 0) {
                val hours = recoveryCooldownRemaining / 3600000
                val minutes = (recoveryCooldownRemaining % 3600000) / 60000
                Text(
                    text = recoveryCooldownActive.format(hours, minutes),
                    color = Color(0xFFFF9800),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { if (recoveryCooldownRemaining == 0L) onRecoverViaBot() },
                enabled = recoveryCooldownRemaining == 0L,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2AABEE),
                    disabledBackgroundColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = recoverViaBot, color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            Divider(color = Color(0xFF333333))
            Spacer(Modifier.height(24.dp))

            Text(
                text = logoutAllAccountsDesc,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            if (logoutCooldownRemaining > 0) {
                val days = logoutCooldownRemaining / 86400000
                val hours = (logoutCooldownRemaining % 86400000) / 3600000
                Text(
                    text = logoutCooldownActive.format(days, hours),
                    color = Color(0xFFFF9800),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { if (logoutCooldownRemaining == 0L) onLogoutAllAccounts() },
                enabled = logoutCooldownRemaining == 0L,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFF5252),
                    disabledBackgroundColor = Color(0xFF333333)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = logoutAllAccounts, color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = cancelLabel,
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun RecoveryInputDialog(
    recoveryError: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    enterRecoveryPin: String,
    recoveryPinHint: String,
    cancelLabel: String,
    confirmLabel: String
) {
    var recoveryPin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = enterRecoveryPin,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PinDots(pinLength = recoveryPin.length, totalLength = 6)

            if (recoveryError.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = recoveryError,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            PinKeyboard(
                onDigit = {
                    if (recoveryPin.length < 6) {
                        recoveryPin += it
                    }
                },
                onDelete = {
                    if (recoveryPin.isNotEmpty()) {
                        recoveryPin = recoveryPin.dropLast(1)
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = cancelLabel, color = Color.White, fontSize = 13.sp)
                }
                Button(
                    onClick = { onConfirm(recoveryPin) },
                    enabled = recoveryPin.length == 6,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2AABEE),
                        disabledBackgroundColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = confirmLabel, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PinDots(pinLength: Int, totalLength: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalLength) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (i < pinLength) Color(0xFF2AABEE) else Color(0xFF333333))
            )
        }
    }
}

@Composable
fun PinKeyboard(
    enabled: Boolean = true,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { digit ->
                    KeyboardKey(
                        text = digit,
                        enabled = enabled,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(56.dp))

            KeyboardKey(
                text = "0",
                enabled = enabled,
                onClick = { onDigit("0") }
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFF1A1A1A) else Color(0xFF0A0A0A))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
