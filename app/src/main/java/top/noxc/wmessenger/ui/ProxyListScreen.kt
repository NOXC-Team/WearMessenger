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
                        ProxyDisableItem(onDisable = onDisable)
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
fun ProxyDisableItem(onDisable: () -> Unit) {
    Surface(
        color = Color(0xFF2A2A2A),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onDisable)
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
                fontSize = 13.sp
            )
            Text(
                text = "→",
                color = Color.Gray,
                fontSize = 13.sp
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${proxy.server}:${proxy.port}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = proxy.type,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                if (pingMs > 0) {
                    Text(
                        text = "${pingMs}ms",
                        color = if (pingMs < 200) Color(0xFF81C784) else if (pingMs < 500) Color(0xFFFFB74D) else Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else if (isPinging) {
                    Text(
                        text = "...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        if (proxy.isEnabled) {
                            onRemove()
                        } else {
                            onEnable()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (proxy.isEnabled) Color(0xFFFF6B6B) else Color(0xFF2AABEE)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (proxy.isEnabled) stringResource(R.string.remove) else stringResource(R.string.enable),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        isPinging = true
                        onPing { ms ->
                            pingMs = ms
                            isPinging = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.ping), color = Color(0xFF2AABEE), fontSize = 11.sp)
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove), color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
            }
        }
    }
}
