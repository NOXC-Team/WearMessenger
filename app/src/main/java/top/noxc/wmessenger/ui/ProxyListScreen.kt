package top.noxc.wmessenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.ProxyItem

@Composable
fun ProxyListScreen(
    proxies: List<ProxyItem>,
    connectionState: String?,
    onEnable: (Int) -> Unit,
    onDisable: () -> Unit,
    onRemove: (Int) -> Unit,
    onPing: (Int, (Int) -> Unit) -> Unit,
    onAddProxy: () -> Unit,
    onBack: () -> Unit
) {
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
            Text(
                text = stringResource(R.string.proxies),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            if (connectionState != null) {
                Surface(
                    color = Color(0xFF1A2A3A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = connectionState,
                        color = Color(0xFF2AABEE),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (proxies.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_proxies),
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    item {
                        ProxyDisableItem(
                            proxies = proxies,
                            onDisable = onDisable
                        )
                    }
                    items(proxies) { proxy ->
                        ProxyListItem(
                            proxy = proxy,
                            onEnable = { onEnable(proxy.id) },
                            onRemove = { onRemove(proxy.id) },
                            onPing = { callback -> onPing(proxy.id, callback) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAddProxy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2AABEE))
            ) {
                Text(
                    text = stringResource(R.string.add_proxy),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProxyDisableItem(
    proxies: List<ProxyItem>,
    onDisable: () -> Unit
) {
    val anyEnabled = proxies.any { it.isEnabled }
    Surface(
        color = Color(0xFF2A2A2A),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.disable_proxy),
                color = Color(0xFF2AABEE),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            RadioButton(
                selected = !anyEnabled,
                onClick = onDisable,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2AABEE),
                    unselectedColor = Color(0xFF2AABEE)
                )
            )
        }
    }
}

@Composable
fun ProxyListItem(
    proxy: ProxyItem,
    onEnable: () -> Unit,
    onRemove: () -> Unit,
    onPing: ((Int) -> Unit) -> Unit
) {
    var pingMs by remember { mutableStateOf(proxy.pingMs) }
    var isPinging by remember { mutableStateOf(false) }

    Surface(
        color = if (proxy.isEnabled) Color(0xFF1A3A2A) else Color(0xFF2A2A2A),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${proxy.server}:${proxy.port}",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = proxy.type,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    if (pingMs != 0) {
                        Text(
                            text = if (pingMs == -1) "超时" else "${pingMs}ms",
                            color = if (pingMs == -1 || pingMs > 1000) Color(0xFFFF6B6B) else Color(0xFF81C784),
                            fontSize = 10.sp
                        )
                    } else if (isPinging) {
                        Text(
                            text = "ping中...",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Text(
                text = "📡",
                fontSize = 16.sp,
                color = Color(0xFF2AABEE),
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        isPinging = true
                        onPing { ms ->
                            pingMs = ms
                            isPinging = false
                        }
                    }
                    .wrapContentSize(Alignment.Center)
            )
            Text(
                text = "🗑",
                fontSize = 16.sp,
                color = Color(0xFFFF6B6B),
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onRemove)
                    .wrapContentSize(Alignment.Center)
            )
            RadioButton(
                selected = proxy.isEnabled,
                onClick = {
                    if (proxy.isEnabled) {
                        onRemove()
                    } else {
                        onEnable()
                    }
                },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2AABEE),
                    unselectedColor = Color(0xFF2AABEE)
                )
            )
        }
    }
}

@Composable
fun EmojiIconButton(
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = emoji,
        fontSize = 18.sp,
        color = color,
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center)
    )
}
