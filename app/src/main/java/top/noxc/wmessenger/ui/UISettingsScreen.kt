package top.noxc.wmessenger.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.SeekBar
import top.noxc.wmessenger.R

@Composable
fun UISettingsScreen(
    densityScale: Float,
    onDensityChangeAndRestart: (Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val baseDensity = context.resources.displayMetrics.density
    val minDpi = 180f
    val minScale = minDpi / baseDensity
    
    val minProgress = (minScale * 100).toInt()
    val maxProgress = 200
    
    var tempProgress by remember { mutableStateOf((densityScale * 100).toInt()) }
    val hasChanged = tempProgress != (densityScale * 100).toInt()
    
    val currentDpi = (baseDensity * densityScale * 160).toInt()
    val previewDpi = (baseDensity * (tempProgress / 100f) * 160).toInt()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
            .padding(8.dp)
    ) {
        Text(
            text = stringResource(R.string.ui_settings),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Divider(color = Color(0xFF333333))

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dpi),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "当前: ${currentDpi}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "预览: ${previewDpi}",
                    color = Color(0xFF2AABEE),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            AndroidView(
                factory = { ctx ->
                    SeekBar(ctx).apply {
                        max = maxProgress
                        progress = tempProgress
                    }
                },
                update = { seekBar ->
                    seekBar.progress = tempProgress
                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val newProgress = progress.coerceAtLeast(minProgress)
                                tempProgress = newProgress
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (hasChanged) {
                Button(
                    onClick = {
                        val newScale = tempProgress / 100f
                        onDensityChangeAndRestart(newScale)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2AABEE))
                ) {
                    Text(
                        text = "应用并重启",
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF444444))
            ) {
                Text(
                    text = stringResource(R.string.back),
                    color = Color.White
                )
            }
        }
    }
}
