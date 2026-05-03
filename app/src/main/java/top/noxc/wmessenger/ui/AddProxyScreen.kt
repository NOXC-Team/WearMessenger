package top.noxc.wmessenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddProxyScreen(
    onAddSocks5: (String, Int, String, String) -> Unit,
    onAddHttp: (String, Int, String, String) -> Unit,
    onAddMtproto: (String, Int, String) -> Unit,
    onBack: () -> Unit
) {
    var proxyType by remember { mutableStateOf("SOCKS5") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 30f) onBack()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Add Proxy",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("SOCKS5", "HTTP", "MTProto").forEach { type ->
                    TextButton(
                        onClick = { proxyType = type },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = if (proxyType == type) Color(0xFF2AABEE) else Color(0xFF2A2A2A)
                        )
                    ) {
                        Text(type, color = if (proxyType == type) Color.Black else Color.LightGray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Server", color = Color(0xFF888888)) },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFB0B0B0),
                        focusedBorderColor = Color(0xFF2AABEE),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF2AABEE)
                    )
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port", color = Color(0xFF888888)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFB0B0B0),
                        focusedBorderColor = Color(0xFF2AABEE),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF2AABEE)
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            if (proxyType != "MTProto") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username", color = Color(0xFF888888)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFB0B0B0),
                            focusedBorderColor = Color(0xFF2AABEE),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF2AABEE)
                        )
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color(0xFF888888)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFB0B0B0),
                            focusedBorderColor = Color(0xFF2AABEE),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF2AABEE)
                        )
                    )
                }
            } else {
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret (hex)", color = Color(0xFF888888)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFB0B0B0),
                        focusedBorderColor = Color(0xFF2AABEE),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF2AABEE)
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: return@Button
                    when (proxyType) {
                        "SOCKS5" -> onAddSocks5(server, p, username, password)
                        "HTTP" -> onAddHttp(server, p, username, password)
                        "MTProto" -> onAddMtproto(server, p, secret)
                    }
                    onBack()
                },
                enabled = server.isNotEmpty() && port.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2AABEE),
                    disabledBackgroundColor = Color(0xFF1A5276)
                )
            ) {
                Text("Add & Enable", fontSize = 13.sp)
            }
        }
    }
}
