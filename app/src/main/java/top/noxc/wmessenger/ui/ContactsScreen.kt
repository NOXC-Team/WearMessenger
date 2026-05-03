package top.noxc.wmessenger.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.ContactItem
import java.io.File

@Composable
fun ContactItemRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    val avatarBitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        contact.avatarLocalPath
    ) {
        val path = contact.avatarLocalPath
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarModifier = Modifier
            .size(32.dp)
            .clip(CircleShape)

        val bitmap = avatarBitmap
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contact.name,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            ContactAvatarPlaceholder(contact.name, avatarModifier)
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = contact.name,
            color = if (contact.isOnline) Color.White else Color.LightGray,
            fontSize = 13.sp
        )
    }
    Divider(color = Color(0xFF222222))
}

@Composable
fun ContactsScreen(
    contacts: List<ContactItem>,
    onContactClick: (Long) -> Unit,
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
        Text(
            text = stringResource(R.string.contacts),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = Color(0xFF333333))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.loading), color = Color.LightGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts) { contact ->
                    ContactItemRow(
                        contact = contact,
                        onClick = { onContactClick(contact.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactAvatarPlaceholder(name: String, modifier: Modifier) {
    Surface(
        color = Color(0xFF2AABEE),
        shape = CircleShape,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.Black,
                fontSize = 13.sp
            )
        }
    }
}
