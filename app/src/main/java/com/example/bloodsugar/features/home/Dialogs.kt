package com.example.bloodsugar.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val datePickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedStartDateMillis?.let { start ->
                        datePickerState.selectedEndDateMillis?.let { end ->
                            onConfirm(start, end)
                        }
                    }
                },
                enabled = datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onConfirm(it)
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    timestamp: Long,
    onTimestampChange: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = { showDatePicker = true }) {
            Text(dateFormatter.format(Date(timestamp)))
        }
        OutlinedButton(onClick = { showTimePicker = true }) {
            Text(timeFormatter.format(Date(timestamp)))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = timestamp
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val minute = cal.get(Calendar.MINUTE)

                            val newCal = Calendar.getInstance()
                            newCal.timeInMillis = selectedDate
                            newCal.set(Calendar.HOUR_OF_DAY, hour)
                            newCal.set(Calendar.MINUTE, minute)

                            onTimestampChange(newCal.timeInMillis)
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("OK") }
            },
            dismissButton = { Button(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newCal = Calendar.getInstance()
                    newCal.timeInMillis = timestamp
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    onTimestampChange(newCal.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { Button(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInputDialog(
    dialogType: DialogType,
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onSaveRecord: () -> Unit,
    onSaveEvent: () -> Unit,
    onSaveActivity: () -> Unit,
    onSugarChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onInsulinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onActivityTypeChange: (String) -> Unit,
    onActivityDurationChange: (String) -> Unit,
    onActivityIntensityChange: (String) -> Unit,
    onTimestampChange: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(when (dialogType) {
                DialogType.SUGAR -> "Log Blood Sugar"
                DialogType.EVENT -> "Log Event"
                DialogType.ACTIVITY -> "Log Activity"
            })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (dialogType) {
                    DialogType.SUGAR -> {
                        OutlinedTextField(
                            value = uiState.sugarValue,
                            onValueChange = onSugarChange,
                            label = { Text("Value (mmol/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = uiState.comment,
                            onValueChange = onCommentChange,
                            label = { Text("Comment") }
                        )
                    }
                    DialogType.EVENT -> {
                        OutlinedTextField(
                            value = uiState.carbsValue,
                            onValueChange = onCarbsChange,
                            label = { Text("Carbohydrates (grams)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = uiState.insulinValue,
                            onValueChange = onInsulinChange,
                            label = { Text("Insulin (units)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    DialogType.ACTIVITY -> {
                        var activityTypeExpanded by remember { mutableStateOf(false) }
                        val activityTypes = listOf("Walking", "Running", "Cycling", "Gym", "Other")
                        ExposedDropdownMenuBox(expanded = activityTypeExpanded, onExpandedChange = { activityTypeExpanded = !activityTypeExpanded }) {
                            OutlinedTextField(
                                value = uiState.activityType,
                                onValueChange = {},
                                label = { Text("Type") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityTypeExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = activityTypeExpanded, onDismissRequest = { activityTypeExpanded = false }) {
                                activityTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = {
                                        onActivityTypeChange(type)
                                        activityTypeExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.activityDuration,
                            onValueChange = onActivityDurationChange,
                            label = { Text("Duration (minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        var intensityExpanded by remember { mutableStateOf(false) }
                        val intensityLevels = listOf("Low", "Medium", "High")
                        ExposedDropdownMenuBox(expanded = intensityExpanded, onExpandedChange = { intensityExpanded = !intensityExpanded }) {
                            OutlinedTextField(
                                value = uiState.activityIntensity,
                                onValueChange = {},
                                label = { Text("Intensity") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intensityExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = intensityExpanded, onDismissRequest = { intensityExpanded = false }) {
                                intensityLevels.forEach { intensity ->
                                    DropdownMenuItem(text = { Text(intensity) }, onClick = {
                                        onActivityIntensityChange(intensity)
                                        intensityExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }
                DateTimePicker(
                    timestamp = uiState.newRecordTimestamp ?: System.currentTimeMillis(),
                    onTimestampChange = onTimestampChange
                )
            }
        },
        confirmButton = {
            Button(
                onClick = when (dialogType) {
                    DialogType.SUGAR -> onSaveRecord
                    DialogType.EVENT -> onSaveEvent
                    DialogType.ACTIVITY -> onSaveActivity
                },
                enabled = when (dialogType) {
                    DialogType.SUGAR -> uiState.sugarValue.isNotBlank()
                    DialogType.EVENT -> uiState.insulinValue.isNotBlank() || uiState.carbsValue.isNotBlank()
                    DialogType.ACTIVITY -> uiState.activityDuration.isNotBlank()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
