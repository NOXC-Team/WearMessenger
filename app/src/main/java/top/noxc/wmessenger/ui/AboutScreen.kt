package top.noxc.wmessenger.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import top.noxc.wmessenger.BuildConfig
import top.noxc.wmessenger.R
import java.util.Hashtable

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onUnlockExperiments: (() -> Unit)? = null
) {
    val tdLibVersion = "1.8.63"
    var devQrTapCount by remember { mutableStateOf(0) }

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
            text = stringResource(R.string.about),
            color = WmTheme.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = WmTheme.dividerStrong)

        Spacer(Modifier.height(16.dp))

        AboutInfoRow(stringResource(R.string.app_name), "WearMessenger")
        AboutInfoRow(stringResource(R.string.version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        AboutInfoRow(stringResource(R.string.build_number), BuildConfig.BUILD_TYPE)
        AboutInfoRow(stringResource(R.string.tdlib_version), tdLibVersion)
        AboutInfoRow(stringResource(R.string.platform), "WearOS (Android ${Build.VERSION.RELEASE})")
        AboutInfoRow(stringResource(R.string.min_sdk), "API 25")
        AboutInfoRow(stringResource(R.string.target_sdk), "API 35")
        AboutInfoRow(stringResource(R.string.protocol), "TDLib")

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.repository),
                color = WmTheme.onBackground,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val repoQrBitmap = remember {
                generateQRCode("https://github.com/NOXC-Team/WearMessenger", 200, 200)
            }
            if (repoQrBitmap != null) {
                Image(
                    bitmap = repoQrBitmap.asImageBitmap(),
                    contentDescription = "Repository QR Code",
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.Black)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "NOXC-Team",
                color = WmTheme.onBackground,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val devQrBitmap = remember {
                generateQRCode("https://noxc.top", 200, 200)
            }
            if (devQrBitmap != null) {
                Image(
                    bitmap = devQrBitmap.asImageBitmap(),
                    contentDescription = "Developer QR Code",
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.Black)
                        .clickable {
                            devQrTapCount++
                            if (devQrTapCount >= 10) {
                                onUnlockExperiments?.invoke()
                                devQrTapCount = 0
                            }
                        }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = WmTheme.onBackground, fontSize = 13.sp)
        Text(text = value, color = WmTheme.textSecondary, fontSize = 12.sp)
    }
    Divider(color = WmTheme.divider)
}

private fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 1

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
