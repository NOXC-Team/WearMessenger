package top.noxc.wmessenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
fun WelcomeToWearMessengerScreen(
    onGetStarted: () -> Unit,
    onSettings: () -> Unit,
    onSwipeBack: () -> Unit,
    isEnglish: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onSwipeBack()
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2AABEE)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.get_started),
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = if (isEnglish) Arrangement.SpaceBetween else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEnglish) {
                Text(
                    text = stringResource(R.string.tap_to_change_language),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onSettings() }
                )
            }

            Text(
                text = "\u2699",
                color = Color.Gray,
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onSettings() }
                    .padding(8.dp)
            )
        }
    }
}
