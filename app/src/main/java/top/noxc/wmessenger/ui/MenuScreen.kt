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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import top.noxc.wmessenger.AccountInfo
import top.noxc.wmessenger.R

@Composable
fun MenuScreen(
    accounts: List<AccountInfo>,
    currentAccountIndex: Int,
    isAppLockEnabled: Boolean,
    showLightModeToggle: Boolean,
    isLightMode: Boolean,
    showMuteToggle: Boolean,
    isMuteAllActive: Boolean,
    doubleSwipeExitEnabled: Boolean,
    onSwitchAccount: (Int) -> Unit,
    onAddAccount: () -> Unit,
    onLogoutAccount: (Int) -> Unit,
    onContacts: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onLockNow: () -> Unit,
    onToggleLightMode: () -> Unit,
    onToggleMuteAll: (Boolean) -> Unit,
    onSwipeUp: () -> Unit,
    onExit: () -> Unit
) {
    var showAccountDialog by remember { mutableStateOf(false) }
    val swipeCountState = remember { mutableStateOf(0) }
    var accumulatedSwipeDistance by remember { mutableStateOf(0f) }
    val swipeThreshold = 200f

    val swipeUpConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0f) {
                    if (doubleSwipeExitEnabled) {
                        accumulatedSwipeDistance += kotlin.math.abs(available.y)
                        if (accumulatedSwipeDistance >= swipeThreshold) {
                            swipeCountState.value++
                            accumulatedSwipeDistance = 0f
                            if (swipeCountState.value >= 2) {
                                onExit()
                                swipeCountState.value = 0
                            }
                        }
                    } else {
                        onSwipeUp()
                    }
                }
                return available
            }
        }
    }

    LaunchedEffect(swipeCountState.value) {
        if (swipeCountState.value == 1 && doubleSwipeExitEnabled) {
            delay(2000)
            swipeCountState.value = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .nestedScroll(swipeUpConnection)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.menu),
            color = WmTheme.onBackground,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Divider(color = WmTheme.dividerStrong)
        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { showAccountDialog = true },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            val current = accounts.find { it.index == currentAccountIndex }
            Text(
                current?.name ?: stringResource(R.string.app_name),
                color = Color(0xFF2AABEE),
                fontSize = 14.sp
            )
        }

        Divider(color = WmTheme.divider)

        listOf(
            stringResource(R.string.contacts) to onContacts,
            stringResource(R.string.search) to onSearch,
            stringResource(R.string.settings) to onSettings
        ).forEach { (label, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { action() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = WmTheme.onBackground,
                    fontSize = 15.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        if (isAppLockEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLockNow() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.wm_ic_lock),
                    contentDescription = null,
                    tint = Color(0xFF2AABEE),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.lock_now),
                    color = WmTheme.onBackground,
                    fontSize = 15.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        if (showLightModeToggle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleLightMode() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLightMode) "\u263E" else "\u2600",
                    color = Color(0xFFFFC107),
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLightMode) stringResource(R.string.dark_mode) else stringResource(R.string.light_mode),
                    color = WmTheme.onBackground,
                    fontSize = 15.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        if (showMuteToggle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleMuteAll(!isMuteAllActive) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMuteAllActive) "\uD83D\uDD07" else "\uD83D\uDD0A",
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isMuteAllActive) stringResource(R.string.unmute_all) else stringResource(R.string.mute_all),
                    color = WmTheme.onBackground,
                    fontSize = 15.sp
                )
            }
            Divider(color = WmTheme.divider)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                doubleSwipeExitEnabled && swipeCountState.value >= 1 -> stringResource(R.string.double_swipe_hint_exit)
                doubleSwipeExitEnabled -> "↑↑ ${stringResource(R.string.double_swipe_hint)}"
                else -> "↑ ${stringResource(R.string.back)}"
            },
            color = WmTheme.textHint,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showAccountDialog) {
        AccountSelectorDialog(
            accounts = accounts,
            currentIndex = currentAccountIndex,
            onSwitch = { onSwitchAccount(it); showAccountDialog = false },
            onAdd = { onAddAccount(); showAccountDialog = false },
            onLogout = { onLogoutAccount(it); showAccountDialog = false },
            onDismiss = { showAccountDialog = false }
        )
    }
}

@Composable
fun AccountSelectorDialog(
    accounts: List<AccountInfo>,
    currentIndex: Int,
    onSwitch: (Int) -> Unit,
    onAdd: () -> Unit,
    onLogout: (Int) -> Unit,
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
                    text = stringResource(R.string.switch_account),
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitch(account.index) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = account.index == currentIndex,
                                    onClick = { onSwitch(account.index) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF2AABEE),
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = account.name,
                                    color = if (account.index == currentIndex) Color.White else Color.LightGray,
                                    fontSize = 14.sp
                                )
                                if (account.loggedIn) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("✓", color = Color(0xFF81C784), fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = stringResource(R.string.logout),
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onLogout(account.index) }
                            )
                        }
                    }
                }

                if (accounts.size < 99) {
                    TextButton(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ ${stringResource(R.string.app_name)}", color = Color(0xFF2AABEE), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
