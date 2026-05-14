package top.noxc.wmessenger.ui

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import top.noxc.wmessenger.LanguageItem
import top.noxc.wmessenger.R

@Composable
fun SettingsScreen(
    currentLanguage: String,
    availableLanguages: List<LanguageItem>,
    experimentsUnlocked: Boolean = false,
    onLanguageChange: (String) -> Unit,
    onProxy: () -> Unit,
    onStorage: () -> Unit,
    onSecurity: () -> Unit,
    onAbout: () -> Unit,
    onExperiments: (() -> Unit)? = null,
    onBack: () -> Unit,
    hideSecurity: Boolean = false
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

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
            text = stringResource(R.string.settings),
            color = WmTheme.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = WmTheme.dividerStrong)

        val menuItems = listOfNotNull(
            stringResource(R.string.proxy) to onProxy,
            stringResource(R.string.storage) to onStorage,
            if (!hideSecurity) stringResource(R.string.security) to onSecurity else null,
            if (experimentsUnlocked && onExperiments != null) stringResource(R.string.experiments) to onExperiments else null,
            stringResource(R.string.about) to onAbout
        )
        menuItems.forEach { (label, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { action() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = WmTheme.onBackground,
                    fontSize = 13.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLanguageDialog = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.language),
                color = WmTheme.onBackground,
                fontSize = 13.sp
            )
            Text(
                text = availableLanguages.find { it.code == currentLanguage }?.displayName ?: currentLanguage,
                color = WmTheme.textSecondary,
                fontSize = 12.sp
            )
        }
        Divider(color = WmTheme.divider)
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            languages = availableLanguages,
            currentLanguage = currentLanguage,
            onSelect = { code ->
                onLanguageChange(code)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun LanguageSelectorDialog(
    languages: List<LanguageItem>,
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1A2A3A),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.language),
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(lang.code) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lang.code == currentLanguage,
                                onClick = { onSelect(lang.code) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF2AABEE),
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = lang.displayName,
                                color = if (lang.code == currentLanguage) Color.White else Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
