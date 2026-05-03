package top.noxc.wmessenger.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.animation.core.*
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.BotCommandItem
import top.noxc.wmessenger.core.InlineButtonItem
import top.noxc.wmessenger.core.KeyboardButtonItem
import top.noxc.wmessenger.core.MessageItem
import java.io.File

@Composable
fun ChatScreen(
    chatTitle: String,
    messages: List<MessageItem>,
    canSend: Boolean,
    botCommands: List<BotCommandItem>,
    replyKeyboard: List<List<KeyboardButtonItem>>,
    inlineKeyboard: Triple<Long, Long, List<List<InlineButtonItem>>>,
    isOnline: Boolean,
    isTyping: Boolean,
    isLoadingHistory: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLoadMore: () -> Unit,
    onInlineButtonClick: (Long, Long, ByteArray) -> Unit,
    onTyping: () -> Unit,
    onAttachCamera: () -> Unit = {},
    onAttachPhoto: () -> Unit = {},
    onAttachVideo: () -> Unit = {}
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var previousSize by remember(chatTitle) { mutableStateOf(0) }
    var hasScrolledToBottom by remember(chatTitle) { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var showBotCommands by remember { mutableStateOf(false) }
    var showReplyKeyboard by remember(chatTitle) { mutableStateOf(false) }
    var firstVisibleIndexBeforeLoad by remember { mutableStateOf(0) }
    var sizeAtLoadRequest by remember { mutableStateOf(-1) }

    var lastTypingSentTime by remember { mutableStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 监听列表滚动状态，当接近顶部时自动加载更多
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex <= 2 && sizeAtLoadRequest == -1 && isLoadingHistory.not()) {
            firstVisibleIndexBeforeLoad = listState.firstVisibleItemIndex
            sizeAtLoadRequest = messages.size
            onLoadMore()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (!hasScrolledToBottom) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }
                    .first { it >= messages.size }
                listState.scrollToItem(messages.size - 1)
                hasScrolledToBottom = true
            } else if (sizeAtLoadRequest >= 0 && messages.size > sizeAtLoadRequest) {
                val diff = messages.size - sizeAtLoadRequest
                listState.scrollToItem(firstVisibleIndexBeforeLoad + diff)
                sizeAtLoadRequest = -1
            } else if (messages.size > previousSize && previousSize > 0) {
                if (isLoadingHistory) {
                    val diff = messages.size - previousSize
                    listState.scrollToItem(listState.firstVisibleItemIndex + diff)
                } else {
                    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleIndex >= previousSize - 2) {
                        listState.scrollToItem(messages.size - 1)
                    }
                }
            } else if (sizeAtLoadRequest >= 0 && messages.size == sizeAtLoadRequest && !isLoadingHistory) {
                // No new messages loaded, reset the request
                sizeAtLoadRequest = -1
            }
            previousSize = messages.size
        }
    }

    LaunchedEffect(chatTitle) {
        // Reset state when opening a new chat
        hasScrolledToBottom = false
        sizeAtLoadRequest = -1
        previousSize = 0
        
        // Wait a bit and if still no messages, try to load more
        kotlinx.coroutines.delay(1000)
        if (messages.isEmpty()) {
            sizeAtLoadRequest = 0
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleColor = when {
                    isTyping -> Color(0xFF2AABEE).copy(alpha = blinkAlpha)
                    isOnline -> Color.White
                    else -> Color.Gray
                }
                Text(
                    text = chatTitle,
                    color = titleColor,
                    fontSize = 16.sp,
                    maxLines = 1
                )
            }

            Divider(color = Color(0xFF333333))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet", color = Color.LightGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    if (isLoadingHistory) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }

            Divider(color = Color(0xFF333333))

            if (canSend) {
                if (showBotCommands && botCommands.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            items(botCommands) { cmd ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSendMessage("/${cmd.command}")
                                            showBotCommands = false
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "/${cmd.command}",
                                        color = Color(0xFF2AABEE),
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = cmd.description,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    Divider(color = Color(0xFF333333))
                }

                if (showReplyKeyboard && replyKeyboard.isNotEmpty()) {
                    BotReplyKeyboard(
                        keyboard = replyKeyboard,
                        onButtonClick = { text ->
                            onSendMessage(text)
                            showReplyKeyboard = false
                        },
                        onDismiss = { showReplyKeyboard = false }
                    )
                }

                if (replyKeyboard.isNotEmpty() && !showReplyKeyboard) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReplyKeyboard = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF2AABEE))
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (botCommands.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showBotCommands = !showBotCommands
                                showReplyKeyboard = false
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                text = "/",
                                color = if (showBotCommands) Color(0xFF2AABEE) else Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = {
                            messageInput = it
                            val now = System.currentTimeMillis()
                            if (now - lastTypingSentTime > 5000) {
                                lastTypingSentTime = now
                                onTyping()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 40.dp),
                        placeholder = { Text("Message", color = Color(0xFF888888), fontSize = 11.sp) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFB0B0B0),
                            focusedBorderColor = Color(0xFF2AABEE),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF2AABEE)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInput.isNotBlank()) {
                                    onSendMessage(messageInput.trim())
                                    messageInput = ""
                                }
                            }
                        ),
                        maxLines = 1,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAttachDialog = true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wm_ic_attach),
                            contentDescription = "Attach",
                            tint = Color(0xFF2AABEE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showAttachDialog) {
                        AlertDialog(
                            onDismissRequest = { showAttachDialog = false },
                            title = { Text("Attach", color = Color.White, fontSize = 14.sp) },
                            buttons = {
                                Column {
                                    Divider(color = Color(0xFF333333))
                                    listOf(
                                        "Camera" to { onAttachCamera(); showAttachDialog = false },
                                        //"Gallery" to { onAttachPhoto(); showAttachDialog = false }
                                    ).forEach { (label, action) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { action() }
                                                .padding(vertical = 10.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, color = Color.White, fontSize = 13.sp)
                                        }
                                        Divider(color = Color(0xFF222222))
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                onSendMessage(messageInput.trim())
                                messageInput = ""
                            }
                        },
                        enabled = messageInput.isNotBlank()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.wm_ic_send),
                            contentDescription = "Send",
                            tint = if (messageInput.isNotBlank()) Color(0xFF2AABEE) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Read-only", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: MessageItem) {
    val alignment = if (msg.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (msg.isOutgoing) Color(0xFF1A5276) else Color(0xFF2A2A2A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 150.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!msg.isOutgoing && msg.senderName.isNotEmpty()) {
                    Text(
                        text = msg.senderName,
                        color = Color(0xFF2AABEE),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(2.dp))
                }

                if (msg.photoLocalPath != null) {
                    val file = File(msg.photoLocalPath)
                    if (file.exists()) {
                        val bitmap = remember(msg.photoLocalPath) {
                            BitmapFactory.decodeFile(msg.photoLocalPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                contentScale = ContentScale.FillWidth
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    } else {
                        Surface(
                            color = Color(0xFF333333),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Loading photo...", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (msg.videoLocalPath != null) {
                    val file = File(msg.videoLocalPath)
                    if (file.exists()) {
                        VideoThumbnail(msg, file)
                    } else {
                        VideoPlaceholder(msg)
                    }
                } else if (msg.videoFileId != null && msg.photoLocalPath == null) {
                    VideoPlaceholder(msg)
                }

                if (msg.text.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (msg.isOutgoing) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = if (msg.isOutgoing) TextAlign.End else TextAlign.Start
                        )
                        if (msg.isOutgoing) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.wm_ic_double_check),
                                contentDescription = if (msg.isRead) "Read" else "Sent",
                                tint = if (msg.isRead) Color(0xFF2AABEE) else Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineKeyboardPanel(
    chatId: Long,
    messageId: Long,
    rows: List<List<InlineButtonItem>>,
    onInlineButtonClick: (Long, Long, ByteArray) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { btn ->
                    OutlinedButton(
                        onClick = {
                            btn.callbackData?.let { data ->
                                onInlineButtonClick(chatId, messageId, data)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = Color.Transparent,
                            contentColor = Color(0xFF2AABEE)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2AABEE))
                    ) {
                        Text(btn.text, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun VideoThumbnail(msg: MessageItem, file: File) {
    val context = LocalContext.current
    val bitmap = remember(msg.videoLocalPath) {
        try {
            BitmapFactory.decodeFile(msg.videoLocalPath)
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        color = Color(0xFF333333),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .clickable {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open video with"))
            },
        shape = MaterialTheme.shapes.small
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Video",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0x80000000),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("▶", color = Color.White, fontSize = 32.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun VideoPlaceholder(msg: MessageItem) {
    Surface(
        color = Color(0xFF333333),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▶", color = Color(0xFF2AABEE), fontSize = 24.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Video ${msg.videoWidth}x${msg.videoHeight}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
