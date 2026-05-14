package top.noxc.wmessenger.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.ChatItem
import java.io.File

@Composable
fun ChatListScreen(
    chats: List<ChatItem>,
    archivedChatsCount: Int,
    savedScrollIndex: Int,
    savedScrollOffset: Int,
    avatarClearEnabled: Boolean,
    onChatClick: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onExit: () -> Unit,
    onOpenArchivedChats: () -> Unit,
    onSaveScrollPosition: (Int, Int) -> Unit,
    onScrollToTop: () -> Unit = {},
    scrollToTopTrigger: Int = 0
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollIndex,
        initialFirstVisibleItemScrollOffset = savedScrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        onSaveScrollPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }

    LaunchedEffect(scrollToTopTrigger) {
        listState.animateScrollToItem(0)
    }

    var overscrollY by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    overscrollY += available.y
                    if (overscrollY > 80f) {
                        overscrollY = 0f
                        onOpenMenu()
                    }
                } else {
                    overscrollY = 0f
                }
                return available
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) {
                        onExit()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onScrollToTop() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = WmTheme.onBackground,
                fontSize = 14.sp
            )
        }

        Divider(color = WmTheme.dividerStrong)

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.loading), color = WmTheme.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(chats) { chat ->
                    ChatListItem(
                        chat = chat,
                        avatarClearEnabled = avatarClearEnabled,
                        onClick = { onChatClick(chat.id) }
                    )
                }
                if (archivedChatsCount > 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenArchivedChats() },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (WmTheme.isLight) Color(0xFFBDBDBD) else Color(0xFF555555),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "\u2B9E",
                                            color = WmTheme.onBackground,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.archived_chats),
                                    color = WmTheme.onBackground,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = archivedChatsCount.toString(),
                                    color = WmTheme.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Divider(color = WmTheme.divider)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatItem,
    avatarClearEnabled: Boolean,
    onClick: () -> Unit
) {
    val avatarBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        chat.avatarLocalPath
    ) {
        val path = chat.avatarLocalPath
        if (path == null) {
            value = null
            return@produceState
        }
        var attempts = 0
        while (attempts < 20) {
            val file = File(path)
            if (file.exists() && file.length() > 0) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    value = bmp
                    return@produceState
                }
            }
            attempts++
            delay(300)
        }
        value = null
    }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarModifier = Modifier
                .size(40.dp)
                .clip(CircleShape)

            val bitmap = avatarBitmap
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = chat.title,
                    modifier = if (avatarClearEnabled || chat.isOnline) avatarModifier else avatarModifier.blur(6.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                AvatarPlaceholder(chat.title, avatarModifier)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    color = WmTheme.onBackground,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (chat.isTyping) stringResource(R.string.typing) else chat.lastMessage,
                    color = if (chat.isTyping) Color(0xFF0FB297).copy(alpha = blinkAlpha) else WmTheme.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (chat.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF2AABEE),
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    Divider(color = WmTheme.divider)
}

@Composable
private fun AvatarPlaceholder(title: String, modifier: Modifier) {
    Surface(
        color = Color(0xFF2AABEE),
        shape = CircleShape,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "?",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ArchivedChatsScreen(
    archivedChats: List<ChatItem>,
    onChatClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.archived_chats),
                color = WmTheme.onBackground,
                fontSize = 14.sp
            )
        }

        Divider(color = WmTheme.dividerStrong)

        if (archivedChats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.loading), color = WmTheme.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(archivedChats) { chat ->
                    ChatListItem(
                        chat = chat,
                        avatarClearEnabled = false,
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }
        }
    }
}
