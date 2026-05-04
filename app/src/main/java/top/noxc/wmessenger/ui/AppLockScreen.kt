package top.noxc.wmessenger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.R

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    BackHandler(enabled = true) {}

    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val enterPinLabel = stringResource(R.string.enter_pin)
    val wrongPasswordLabel = stringResource(R.string.wrong_password)

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

        Spacer(Modifier.height(24.dp))

        PinDots(pinLength = pin.length, totalLength = 4)

        Spacer(Modifier.weight(1f))

        PinKeyboard(
            onDigit = {
                if (pin.length < 4) {
                    pin += it
                    showError = false
                    if (pin.length == 4) {
                        if (onVerifyPin(pin)) {
                            onUnlock()
                        } else {
                            showError = true
                            pin = ""
                        }
                    }
                }
            },
            onDelete = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    showError = false
                }
            }
        )

        Spacer(Modifier.height(16.dp))
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
                onClick = { onDigit("0") }
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌫",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
