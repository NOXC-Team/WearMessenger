package top.noxc.wmessenger.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MediaItem(
    val uri: String,
    val isVideo: Boolean,
    val displayName: String
)

@Composable
fun GalleryPickerScreen(
    mediaList: List<MediaItem>,
    onMediaSelect: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    var filterMode by remember { mutableStateOf(0) }

    val filtered = when (filterMode) {
        0 -> mediaList
        1 -> mediaList.filter { !it.isVideo }
        2 -> mediaList.filter { it.isVideo }
        else -> mediaList
    }

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
            text = "Gallery",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All" to 0, "Photos" to 1, "Videos" to 2).forEach { (label, mode) ->
                TextButton(
                    onClick = { filterMode = mode },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        color = if (filterMode == mode) Color(0xFF2AABEE) else Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Divider(color = Color(0xFF333333))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No media found", color = Color.LightGray, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered) { item ->
                    MediaThumbnail(
                        item = item,
                        onClick = { onMediaSelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(item.uri) {
        if (isLoading) return@LaunchedEffect
        isLoading = true
        bitmap = null

        try {
            val uri = android.net.Uri.parse(item.uri)
            if (item.isVideo) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    bitmap = retriever.getFrameAtTime(
                        0,
                        android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                } catch (_: Exception) {
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    bitmap = BitmapFactory.decodeStream(input, null, options)
                }
            }
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Surface(
                color = Color(0xFF2A2A2A),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.isVideo) "🎬" else "🖼️",
                        color = Color.Gray,
                        fontSize = 24.sp
                    )
                }
            }
        }
        if (item.isVideo) {
            Surface(
                color = Color(0x80000000),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
