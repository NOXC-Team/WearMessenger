package top.noxc.wmessenger.ui

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
fun SecurityScreen(
    onBack: () -> Unit,
    onAppLockSettings: () -> Unit,
    onDevicesClick: () -> Unit
) {
    val appLockLabel = stringResource(R.string.app_lock)
    val devicesLabel = stringResource(R.string.devices)

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
            text = stringResource(R.string.security),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = Color(0xFF333333))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDevicesClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = devicesLabel,
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
                .clickable { onAppLockSettings() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appLockLabel,
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
    }
}
