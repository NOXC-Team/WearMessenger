package top.noxc.wmessenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.R

@Composable
fun AppLockSetScreen(
    onPinSet: (String) -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(0) }
    var showError by remember { mutableStateOf(false) }

    val setPinLabel = stringResource(R.string.set_pin)
    val confirmPinLabel = stringResource(R.string.confirm_pin)
    val pinMismatchLabel = stringResource(R.string.pin_mismatch)

    val title = when (phase) {
        0 -> setPinLabel
        else -> confirmPinLabel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) onBack()
                }
            },
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
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (showError) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = pinMismatchLabel,
                color = Color(0xFFFF5252),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        PinDots(
            pinLength = if (phase == 0) pin.length else confirmPin.length,
            totalLength = 4
        )

        Spacer(Modifier.weight(1f))

        PinKeyboard(
            onDigit = {
                if (phase == 0) {
                    if (pin.length < 4) {
                        pin += it
                        showError = false
                        if (pin.length == 4) {
                            phase = 1
                        }
                    }
                } else {
                    if (confirmPin.length < 4) {
                        confirmPin += it
                        showError = false
                        if (confirmPin.length == 4) {
                            if (pin == confirmPin) {
                                onPinSet(pin)
                            } else {
                                showError = true
                                confirmPin = ""
                                phase = 0
                                pin = ""
                            }
                        }
                    }
                }
            },
            onDelete = {
                if (phase == 0) {
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                        showError = false
                    }
                } else {
                    if (confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                        showError = false
                    } else {
                        phase = 0
                    }
                }
            }
        )

        Spacer(Modifier.height(16.dp))
    }
}
