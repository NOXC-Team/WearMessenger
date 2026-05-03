package top.noxc.wmessenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.noxc.wmessenger.R
import top.noxc.wmessenger.core.ChatItem

@Composable
fun SearchScreen(
    userSearchResults: List<ChatItem>,
    onSearchUsers: (String) -> Unit,
    onChatClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        onSearchUsers(query)
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
            text = stringResource(R.string.search),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder), color = Color(0xFF888888), fontSize = 12.sp) },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color(0xFFB0B0B0),
                focusedBorderColor = Color(0xFF2AABEE),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFF2AABEE)
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )

        Divider(color = Color(0xFF333333))

        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.search_placeholder), color = Color.LightGray, fontSize = 13.sp)
            }
        } else if (userSearchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_proxies), color = Color.LightGray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(userSearchResults) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(item.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Divider(color = Color(0xFF222222))
                }
            }
        }
    }
}
