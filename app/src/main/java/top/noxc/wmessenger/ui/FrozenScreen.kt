package top.noxc.wmessenger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.FreezeInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FrozenScreen(
    freezeInfo: FreezeInfo?,
    onOpenSpamBot: () -> Unit,
    onUnderstood: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "\u2744\uFE0F",
            fontSize = 36.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.account_frozen),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.account_frozen_desc),
            color = Color(0xFFB0B0B0),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        if (freezeInfo != null) {
            Spacer(Modifier.height(12.dp))

            if (freezeInfo.freezingDate > 0) {
                val frozenDate = formatDate(freezeInfo.freezingDate * 1000L)
                Text(
                    text = stringResource(R.string.frozen_on, frozenDate),
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }

            if (freezeInfo.deletionDate > 0) {
                val deleteDate = formatDate(freezeInfo.deletionDate * 1000L)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.will_be_deleted_on, deleteDate),
                    color = Color(0xFFCC4444),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpenSpamBot,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2AABEE)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.search), color = Color.White, fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onUnderstood,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFF444444))
        ) {
            Text(stringResource(R.string.ok), fontSize = 13.sp)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Unknown"
    }
}
