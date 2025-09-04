package com.example.bloodsugar.features.notifications

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloodsugar.R
import com.example.bloodsugar.database.NotificationSetting
import com.example.bloodsugar.database.NotificationType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(notificationsViewModel: NotificationsViewModel = viewModel()) {
    val uiState by notificationsViewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNotification by remember { mutableStateOf<NotificationSetting?>(null) }

    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    )

    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.notifications_add))
            }
        }
    ) { padding ->
        val groupedNotifications = uiState.notifications.groupBy { it.type }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(stringResource(id = R.string.notifications_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            groupedNotifications.forEach { (type, notifications) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillParentMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (type == NotificationType.DAILY) stringResource(id = R.string.notifications_daily_reminders) else stringResource(id = R.string.notifications_interval_reminders),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onDelete = { notificationsViewModel.deleteNotification(notification) },
                        onToggle = { isEnabled -> notificationsViewModel.toggleNotification(notification, isEnabled) },
                        onEdit = { editingNotification = notification }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        NotificationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                notificationsViewModel.addNotification(it)
                showAddDialog = false
            }
        )
    }

    editingNotification?.let { notification ->
        NotificationDialog(
            notificationSetting = notification,
            onDismiss = { editingNotification = null },
            onConfirm = {
                notificationsViewModel.addNotification(it)
                editingNotification = null
            }
        )
    }
}

@Composable
fun NotificationItem(
    notification: NotificationSetting,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = notification.message, style = MaterialTheme.typography.bodyLarge)
                val description = if (notification.type == NotificationType.DAILY) {
                    stringResource(id = R.string.notifications_daily_at, notification.time)
                } else {
                    val interval = notification.intervalMinutes
                    val unit = if (interval >= 60 && interval % 60 == 0) stringResource(id = R.string.notifications_hours) else stringResource(id = R.string.notifications_minutes)
                    val value = if (unit == stringResource(id = R.string.notifications_hours)) interval / 60 else interval
                    stringResource(id = R.string.notifications_interval_description, value, unit, notification.startTime ?: "", notification.endTime ?: "")
                }
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }

            Switch(checked = notification.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_food))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDialog(
    notificationSetting: NotificationSetting? = null,
    onDismiss: () -> Unit,
    onConfirm: (NotificationSetting) -> Unit
) {
    var step by remember { mutableStateOf(1) }

    var message by remember { mutableStateOf(notificationSetting?.message ?: "") }
    var type by remember { mutableStateOf(notificationSetting?.type ?: NotificationType.DAILY) }
    var time by remember { mutableStateOf(notificationSetting?.time ?: "08:00") }
    var intervalValue by remember { mutableStateOf("1") }
    var intervalUnit by remember { mutableStateOf("Hours") }
    var startTime by remember { mutableStateOf(notificationSetting?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(notificationSetting?.endTime ?: "22:00") }
    var isEnabled by remember { mutableStateOf(notificationSetting?.isEnabled ?: true) }

    if (notificationSetting != null) {
        LaunchedEffect(notificationSetting) {
            val totalMinutes = notificationSetting.intervalMinutes
            if (totalMinutes >= 60 && totalMinutes % 60 == 0) {
                intervalUnit = "Hours"
                intervalValue = (totalMinutes / 60).toString()
            } else {
                intervalUnit = "Minutes"
                intervalValue = totalMinutes.toString()
            }
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        title = { Text(if (notificationSetting == null) stringResource(id = R.string.notifications_add) else stringResource(id = R.string.notifications_edit)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    1 -> Step1Message(message = message, onMessageChange = { message = it })
                    2 -> Step2TypeAndConfig(
                        type = type,
                        onTypeChange = { type = it },
                        time = time,
                        onTimeSelectorClick = {
                            timePickerTarget = "daily"
                            showTimePicker = true
                        },
                        intervalValue = intervalValue,
                        onIntervalValueChange = { intervalValue = it },
                        intervalUnit = intervalUnit,
                        onIntervalUnitChange = { intervalUnit = it },
                        startTime = startTime,
                        onStartTimeSelectorClick = {
                            timePickerTarget = "start"
                            showTimePicker = true
                        },
                        endTime = endTime,
                        onEndTimeSelectorClick = {
                            timePickerTarget = "end"
                            showTimePicker = true
                        }
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(onClick = { step = 2 }, enabled = message.isNotBlank()) {
                    Text(stringResource(id = R.string.next))
                }
            } else {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val intervalInMinutes = (intervalValue.toIntOrNull() ?: 1).let {
                            if (intervalUnit == "Hours") it * 60 else it
                        }
                        val setting = (notificationSetting ?: NotificationSetting()).copy(
                            type = type,
                            time = time,
                            intervalMinutes = intervalInMinutes,
                            message = message,
                            isEnabled = isEnabled,
                            startTime = if (type == NotificationType.INTERVAL) startTime else null,
                            endTime = if (type == NotificationType.INTERVAL) endTime else null
                        )
                        onConfirm(setting)
                    },
                    enabled = message.isNotBlank() && (type == NotificationType.DAILY || intervalValue.toIntOrNull() != null)
                ) {
                    Text(if (notificationSetting == null) stringResource(id = R.string.add) else stringResource(id = R.string.save))
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                Button(onClick = { step = 1 }) {
                    Text(stringResource(id = R.string.back))
                }
            } else {
                Button(onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    )

    if (showTimePicker) {
        val (initialHour, initialMinute) = parseTime(
            when (timePickerTarget) {
                "daily" -> time
                "start" -> startTime
                "end" -> endTime
                else -> "08:00"
            }
        )

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(id = R.string.notifications_select_time)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selectedTime = formatTime(timePickerState.hour, timePickerState.minute)
                    when (timePickerTarget) {
                        "daily" -> time = selectedTime
                        "start" -> startTime = selectedTime
                        "end" -> endTime = selectedTime
                    }
                    showTimePicker = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { showTimePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun Step1Message(message: String, onMessageChange: (String) -> Unit) {
    OutlinedTextField(
        value = message,
        onValueChange = onMessageChange,
        label = { Text(stringResource(id = R.string.notifications_reminder_message)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Step2TypeAndConfig(
    type: NotificationType,
    onTypeChange: (NotificationType) -> Unit,
    time: String,
    onTimeSelectorClick: () -> Unit,
    intervalValue: String,
    onIntervalValueChange: (String) -> Unit,
    intervalUnit: String,
    onIntervalUnitChange: (String) -> Unit,
    startTime: String,
    onStartTimeSelectorClick: () -> Unit,
    endTime: String,
    onEndTimeSelectorClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(selected = type == NotificationType.DAILY, onClick = { onTypeChange(NotificationType.DAILY) })
            Text(stringResource(id = R.string.notifications_type_daily))
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = type == NotificationType.INTERVAL, onClick = { onTypeChange(NotificationType.INTERVAL) })
            Text(stringResource(id = R.string.notifications_type_interval))
        }

        if (type == NotificationType.DAILY) {
            TimeSelector(label = stringResource(id = R.string.notifications_time), time = time, onClick = onTimeSelectorClick)
        } else {
            OutlinedTextField(
                value = intervalValue,
                onValueChange = onIntervalValueChange,
                label = { Text(stringResource(id = R.string.notifications_interval)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = intervalUnit == "Minutes", onClick = { onIntervalUnitChange("Minutes") })
                Text(stringResource(id = R.string.notifications_minutes))
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = intervalUnit == "Hours", onClick = { onIntervalUnitChange("Hours") })
                Text(stringResource(id = R.string.notifications_hours))
            }
            TimeSelector(label = stringResource(id = R.string.notifications_start_time), time = startTime, onClick = onStartTimeSelectorClick)
            TimeSelector(label = stringResource(id = R.string.notifications_end_time), time = endTime, onClick = onEndTimeSelectorClick)
        }
    }
}

@Composable
fun TimeSelector(label: String, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = time, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun parseTime(time: String): Pair<Int, Int> {
    return try {
        val parts = time.split(":").map { it.toInt() }
        if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            8 to 0
        }
    } catch (e: Exception) {
        8 to 0
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}