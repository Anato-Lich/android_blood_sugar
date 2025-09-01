package com.example.bloodsugar.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
    onActivityIntensityChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when(dialogType) {
            DialogType.SUGAR -> "Log Blood Sugar"
            DialogType.EVENT -> "Log Event"
            DialogType.ACTIVITY -> "Log Activity"
        }) },
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
                        ExposedDropdownMenuBox(expanded = activityTypeExpanded, onExpandedChange = {activityTypeExpanded = !activityTypeExpanded}) {
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
                        ExposedDropdownMenuBox(expanded = intensityExpanded, onExpandedChange = {intensityExpanded = !intensityExpanded}) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = when(dialogType) {
                    DialogType.SUGAR -> onSaveRecord
                    DialogType.EVENT -> onSaveEvent
                    DialogType.ACTIVITY -> onSaveActivity
                },
                enabled = when(dialogType) {
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
