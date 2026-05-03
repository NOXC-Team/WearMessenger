package top.noxc.wmessenger.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import top.noxc.wmessenger.AuthType

@Composable
fun LoginScreen(
    authType: AuthType,
    qrLink: String,
    phoneNumber: String,
    status: String,
    isLoading: Boolean,
    onPasswordSubmit: (String) -> Unit,
    onCodeSubmit: (String) -> Unit,
    onPhoneNumberSubmit: (String) -> Unit,
    onShowPhoneInput: () -> Unit,
    onShowQrCode: () -> Unit,
    onProxySettings: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onResetLogin: (() -> Unit)? = null,
    hasLoggedInAccount: Boolean = false,
    onSwipeBack: (() -> Unit)? = null,
    onEmailSubmit: ((String) -> Unit)? = null,
    onEmailCodeSubmit: ((String) -> Unit)? = null,
    onRegisterSubmit: ((String, String) -> Unit)? = null
) {
    var passwordInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var emailCodeInput by remember { mutableStateOf("") }
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("86") }
    var phoneInput by remember { mutableStateOf("") }
    var tapCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) {
                        onSwipeBack?.invoke()
                    }
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null && hasLoggedInAccount) {
                TextButton(onClick = onCancel) {
                    Text("✕", color = Color(0xFFFF6B6B), fontSize = 16.sp)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(
                text = "Login to Telegram",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.clickable {
                    tapCount++
                    if (tapCount >= 5) {
                        tapCount = 0
                        onProxySettings?.invoke()
                    } else {
                        scope.launch {
                            kotlinx.coroutines.delay(2000)
                            tapCount = 0
                        }
                    }
                }
            )
            Spacer(Modifier.width(1.dp))
        }
        Spacer(Modifier.height(20.dp))

        if (status.startsWith("Error:") && authType != AuthType.ERROR) {
            Text(
                text = status,
                color = Color(0xFFFF6B6B),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        when (authType) {
            AuthType.LOADING -> {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            AuthType.QR_CODE -> {
                if (qrLink.isNotEmpty()) {
                    val qrBitmap = remember(qrLink) { generateQrCode(qrLink, 256, 256) }
                    Surface(
                        color = Color(0xFF2A2A2A),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan QR Code:",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(160.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onShowPhoneInput) {
                    Text(
                        text = "Login using number",
                        color = Color(0xFF2AABEE),
                        fontSize = 12.sp
                    )
                }
            }

            AuthType.PHONE_INPUT -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter Phone Number",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "We'll send you a verification code",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = { countryCode = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("+") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.3f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF2AABEE),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF2AABEE)
                                )
                            )
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it.filter { c -> c.isDigit() }.take(15) },
                                label = { Text("Phone number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(0.7f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF2AABEE),
                                    unfocusedBorderColor = Color.Gray,
                                    cursorColor = Color(0xFF2AABEE)
                                )
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { onPhoneNumberSubmit("+${countryCode}${phoneInput}") },
                            enabled = phoneInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Send Code")
                        }

                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onShowQrCode) {
                            Text(
                                text = "Back to QR Code",
                                color = Color(0xFF2AABEE),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            AuthType.PASSWORD -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Two-Step Verification",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Enter your cloud password",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { onPasswordSubmit(passwordInput) },
                            enabled = passwordInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Submit")
                        }

                        if (onResetLogin != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onResetLogin) {
                                Text(
                                    text = "Edit phone number",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            AuthType.VERIFICATION_CODE -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Verification Code",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Code sent to: $phoneNumber",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { onCodeSubmit(codeInput) },
                            enabled = codeInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Submit")
                        }

                        if (onResetLogin != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onResetLogin) {
                                Text(
                                    text = "Edit phone number",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            AuthType.READY -> {
                Surface(
                    color = Color(0xFF1A3A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✓ Logged in!",
                            color = Color(0xFF81C784),
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Welcome to WearMessenger",
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AuthType.EMAIL_INPUT -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Email Address",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Enter your email address to continue",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onEmailSubmit?.invoke(emailInput) },
                            enabled = emailInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Submit")
                        }
                        if (onResetLogin != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onResetLogin) {
                                Text(
                                    text = "Edit phone number",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            AuthType.EMAIL_CODE -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Email Code",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Enter the code sent to your email",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = emailCodeInput,
                            onValueChange = { emailCodeInput = it.filter { c -> c.isDigit() || c.isLetter() }.take(8) },
                            label = { Text("Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onEmailCodeSubmit?.invoke(emailCodeInput) },
                            enabled = emailCodeInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Submit")
                        }
                        if (onResetLogin != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onResetLogin) {
                                Text(
                                    text = "Edit phone number",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            AuthType.REGISTRATION -> {
                Surface(
                    color = Color(0xFF2A2A2A),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Register",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Enter your name to create an account",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = firstNameInput,
                            onValueChange = { firstNameInput = it },
                            label = { Text("First Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = lastNameInput,
                            onValueChange = { lastNameInput = it },
                            label = { Text("Last Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF2AABEE),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2AABEE)
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onRegisterSubmit?.invoke(firstNameInput, lastNameInput) },
                            enabled = firstNameInput.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2AABEE),
                                disabledBackgroundColor = Color(0xFF1A5276)
                            )
                        ) {
                            Text("Register")
                        }
                    }
                }
            }

            AuthType.ERROR -> {
                Surface(
                    color = Color(0xFF3D1F1F),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ Error",
                            color = Color(0xFFFF6B6B),
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = status,
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "WearMessenger",
            color = Color.LightGray,
            fontSize = 10.sp
        )
    }
}

fun generateQrCode(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#2A2A2A"))
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
