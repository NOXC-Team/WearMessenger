package top.noxc.wmessenger.ui

import android.text.format.Formatter
import androidx.compose.foundation.clickable
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
import top.noxc.wmessenger.R

@Composable
fun StorageScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cacheDir = context.cacheDir
    val filesDir = context.filesDir

    var cacheSize by remember { mutableStateOf(calculateSize(cacheDir)) }
    var filesSize by remember { mutableStateOf(calculateSize(filesDir)) }
    var clearing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Text(
            text = stringResource(R.string.storage),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = Color(0xFF333333))

        Spacer(Modifier.height(12.dp))

        StorageInfoRow(stringResource(R.string.storage) + " - Cache", formatSize(cacheSize))
        StorageInfoRow(stringResource(R.string.storage) + " - Files", formatSize(filesSize))
        StorageInfoRow(stringResource(R.string.storage) + " - Total", formatSize(cacheSize + filesSize))

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                clearing = true
                clearCache(cacheDir)
                cacheSize = calculateSize(cacheDir)
                clearing = false
            },
            enabled = !clearing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (clearing) stringResource(R.string.loading) else "Clear Cache",
                color = if (clearing) Color.Gray else Color(0xFFFF6B6B),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StorageInfoRow(label: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 13.sp)
        Text(text = size, color = Color.LightGray, fontSize = 12.sp)
    }
    Divider(color = Color(0xFF222222))
}

private fun calculateSize(dir: java.io.File): Long {
    var size = 0L
    if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateSize(file) else file.length()
        }
    }
    return size
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

private fun clearCache(dir: java.io.File) {
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            clearCache(file)
        }
        file.delete()
    }
}
