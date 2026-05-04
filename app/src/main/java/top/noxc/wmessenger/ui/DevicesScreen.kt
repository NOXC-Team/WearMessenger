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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import top.noxc.wmessenger.R

data class SessionItem(
    val id: Long,
    val isCurrent: Boolean,
    val deviceModel: String,
    val platform: String,
    val applicationName: String,
    val lastActiveDate: String,
    val canTerminate: Boolean
)

@Composable
fun DevicesScreen(
    sessions: List<SessionItem>,
    inactiveSessionTtlDays: Int,
    onTerminateSession: (Long) -> Unit,
    onTerminateAllOther: () -> Unit,
    onSetInactiveSessionTtl: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showTtlDialog by remember { mutableStateOf(false) }

    val thisDeviceLabel = stringResource(R.string.this_device)
    val logoutLabel = stringResource(R.string.logout)
    val logoutAllLabel = stringResource(R.string.terminate_all_other)
    val autoTerminatePrefix = stringResource(R.string.auto_terminate_prefix)
    val autoTerminateSuffix = stringResource(R.string.auto_terminate_suffix)
    val oneWeekLabel = stringResource(R.string.one_week)
    val threeMonthsLabel = stringResource(R.string.three_months)
    val sixMonthsLabel = stringResource(R.string.six_months)
    val oneYearLabel = stringResource(R.string.one_year)

    val ttlDisplay = when (inactiveSessionTtlDays) {
        7 -> oneWeekLabel
        90 -> threeMonthsLabel
        180 -> sixMonthsLabel
        365 -> oneYearLabel
        else -> oneWeekLabel
    }

    val currentSession = sessions.find { it.isCurrent }
    val otherSessions = sessions.filter { !it.isCurrent }

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
            text = stringResource(R.string.devices),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Divider(color = Color(0xFF333333))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTtlDialog = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = autoTerminatePrefix,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = ttlDisplay,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = autoTerminateSuffix,
                color = Color.White,
                fontSize = 13.sp
            )
        }
        Divider(color = Color(0xFF222222))

        if (currentSession != null) {
            Text(
                text = thisDeviceLabel,
                color = Color(0xFF2AABEE),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            SessionRow(
                session = currentSession,
                onTerminate = null
            )
            Divider(color = Color(0xFF222222))
        }

        if (otherSessions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.other_devices),
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            otherSessions.forEach { session ->
                SessionRow(
                    session = session,
                    onTerminate = { onTerminateSession(session.id) }
                )
                Divider(color = Color(0xFF222222))
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onTerminateAllOther,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = logoutAllLabel,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp
                )
            }
        }

        if (sessions.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.loading),
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showTtlDialog) {
        TtlDialog(
            currentTtl = inactiveSessionTtlDays,
            onSelect = {
                onSetInactiveSessionTtl(it)
                showTtlDialog = false
            },
            onDismiss = { showTtlDialog = false }
        )
    }
}

@Composable
fun SessionRow(
    session: SessionItem,
    onTerminate: (() -> Unit)?
) {
    val logoutLabel = stringResource(R.string.logout)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.deviceModel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = session.applicationName,
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = session.lastActiveDate,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        if (onTerminate != null) {
            Text(
                text = logoutLabel,
                color = Color(0xFFFF6B6B),
                fontSize = 11.sp,
                modifier = Modifier.clickable { onTerminate() }
            )
        }
    }
}

@Composable
fun TtlDialog(
    currentTtl: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val autoTerminatePrefix = stringResource(R.string.auto_terminate_prefix)
    val oneWeekLabel = stringResource(R.string.one_week)
    val threeMonthsLabel = stringResource(R.string.three_months)
    val sixMonthsLabel = stringResource(R.string.six_months)
    val oneYearLabel = stringResource(R.string.one_year)

    val options = listOf(
        7 to oneWeekLabel,
        90 to threeMonthsLabel,
        180 to sixMonthsLabel,
        365 to oneYearLabel
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF1A2A3A),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = autoTerminatePrefix,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == currentTtl,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF2AABEE),
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (value == currentTtl) Color.White else Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
