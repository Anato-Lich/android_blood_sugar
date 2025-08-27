package com.example.bloodsugar.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.viewmodel.ChartData
import com.example.bloodsugar.viewmodel.DialogType
import com.example.bloodsugar.viewmodel.FilterType
import com.example.bloodsugar.viewmodel.HomeUiState
import com.example.bloodsugar.viewmodel.HomeViewModel
import com.example.bloodsugar.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    sharedViewModel: SharedViewModel
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var showCustomDateRangePicker by remember { mutableStateOf(false) }
    val sharedState by sharedViewModel.uiState.collectAsState()

    if (uiState.shownDialog != null) {
        LogInputDialog(
            dialogType = uiState.shownDialog!!,
            uiState = uiState,
            onDismiss = { homeViewModel.onDialogDismiss() },
            onSaveRecord = {
                homeViewModel.saveRecord()
                homeViewModel.onDialogDismiss()
            },
            onSaveEvent = {
                homeViewModel.saveInsulinCarbEvent()
                homeViewModel.onDialogDismiss()
            },
            onSugarChange = { homeViewModel.setSugarValue(it) },
            onCommentChange = { homeViewModel.setComment(it) },
            onInsulinChange = { homeViewModel.setInsulinValue(it) },
            onCarbsChange = { homeViewModel.setCarbsValue(it) }
        )
    }

    LaunchedEffect(sharedState) {
        homeViewModel.setInsulinValue(sharedState.latestInsulinDose)
        homeViewModel.setCarbsValue(sharedState.latestCarbs)
    }

    if (showCustomDateRangePicker) {
        CustomDateRangePicker(
            onDismiss = { showCustomDateRangePicker = false },
            onConfirm = { start, end ->
                homeViewModel.setCustomDateRange(start, end)
                showCustomDateRangePicker = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Glucose History", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BloodSugarChart(
                        chartData = uiState.chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.chartData?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Metric("Avg", it.avg)
                            Metric("Min", it.min)
                            Metric("Max", it.max)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { homeViewModel.setFilter(FilterType.TODAY) }) { Text("Today") }
                        Button(onClick = { homeViewModel.setFilter(FilterType.THREE_DAYS) }) { Text("3 Days") }
                        Button(onClick = { homeViewModel.setFilter(FilterType.SEVEN_DAYS) }) { Text("7 Days") }
                        Button(onClick = { showCustomDateRangePicker = true }) { Text("Custom") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("History", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(uiState.historyItems) { item ->
            when (item) {
                is BloodSugarRecord -> {
                    RecordItem(
                        record = item,
                        onDelete = { homeViewModel.deleteRecord(item) }
                    )
                }
                is EventRecord -> {
                    EventHistoryItem(
                        event = item,
                        onDelete = { homeViewModel.deleteEvent(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun Metric(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = "%.1f".format(value), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun BloodSugarChart(
    chartData: ChartData?,
    selectedRecord: BloodSugarRecord?,
    onRecordClick: (BloodSugarRecord) -> Unit,
    onDismissTooltip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val records = chartData?.records ?: emptyList()
    val events = chartData?.events?.sortedBy { it.timestamp } ?: emptyList()
    val density = LocalDensity.current
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12.sp.value * density.density
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val recordPoints = remember { mutableStateMapOf<Long, Offset>() }

    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = { tapOffset ->
                var tappedRecordId: Long? = null
                val tapRadius = 16.dp.toPx()

                recordPoints.forEach { (id, pointOffset) ->
                    if ((tapOffset - pointOffset).getDistance() < tapRadius) {
                        tappedRecordId = id
                    }
                }

                if (tappedRecordId != null) {
                    val record = records.first { it.id == tappedRecordId }
                    onRecordClick(record)
                } else {
                    if (selectedRecord != null) {
                        onDismissTooltip()
                    }
                }
            }
        )
    }) {
        if (chartData == null || (records.isEmpty() && events.isEmpty())) {
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "Not enough data to display chart",
                    center.x,
                    center.y,
                    textPaint
                )
            }
            return@Canvas
        }

        val padding = 20.dp.toPx()
        val chartWidth = size.width - 2 * padding
        val chartHeight = size.height - 2 * padding

        val minTime = chartData.rangeStart
        val maxTime = chartData.rangeEnd
        val timeRange = (maxTime - minTime).toFloat()

        val minValue = 0f
        val maxValue = 15f
        val valueRange = max(maxValue - minValue, 1f)

        // Draw zone backgrounds
        val yFor10 = padding + chartHeight - ((10f - minValue) / valueRange) * chartHeight
        val yFor4 = padding + chartHeight - ((4f - minValue) / valueRange) * chartHeight
        val chartTopY = padding
        val chartBottomY = padding + chartHeight
        val chartLeftX = padding

        // Red zone (> 10)
        val redTop = chartTopY
        val redBottom = yFor10.coerceIn(chartTopY, chartBottomY)
        if (redTop < redBottom) {
            drawRect(
                color = Color.Red.copy(alpha = 0.05f),
                topLeft = Offset(chartLeftX, redTop),
                size = Size(chartWidth, redBottom - redTop)
            )
        }

        // Green zone (4-10)
        val greenTop = yFor10.coerceIn(chartTopY, chartBottomY)
        val greenBottom = yFor4.coerceIn(chartTopY, chartBottomY)
        if (greenTop < greenBottom) {
            drawRect(
                color = Color(0xFF006400).copy(alpha = 0.05f),
                topLeft = Offset(chartLeftX, greenTop),
                size = Size(chartWidth, greenBottom - greenTop)
            )
        }

        // Blue zone (< 4)
        val blueTop = yFor4.coerceIn(chartTopY, chartBottomY)
        val blueBottom = chartBottomY
        if (blueTop < blueBottom) {
            drawRect(
                color = Color.Blue.copy(alpha = 0.05f),
                topLeft = Offset(chartLeftX, blueTop),
                size = Size(chartWidth, blueBottom - blueTop)
            )
        }

        // Function to transform data points to canvas coordinates
        fun toOffset(timestamp: Long, value: Float): Offset {
            val x = padding + ((timestamp - minTime) / timeRange) * chartWidth
            val y = padding + chartHeight - ((value - minValue) / valueRange) * chartHeight
            return Offset(x, y)
        }

        // Draw grid lines and labels
        val gridLineColor = Color.LightGray
        val labelCount = 5
        for (i in 0..labelCount) {
            // Horizontal lines and Y-axis labels
            val y = padding + chartHeight * i / labelCount
            drawLine(gridLineColor, start = Offset(padding, y), end = Offset(padding + chartWidth, y))
            val label = minValue + (valueRange * (labelCount - i) / labelCount)
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "%.1f".format(label),
                    padding / 2,
                    y + textPaint.textSize / 2,
                    textPaint
                )
            }
        }

        // Vertical lines and X-axis labels
        val displayTimeRange = maxTime - minTime
        if (displayTimeRange <= TimeUnit.HOURS.toMillis(26)) {
            // --- HOURLY LABELS ---
            val cal = Calendar.getInstance()
            cal.timeInMillis = minTime
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            val firstLabelTime = cal.timeInMillis

            val totalHours = TimeUnit.MILLISECONDS.toHours(maxTime - firstLabelTime).coerceAtLeast(1)
            val stepHours = when {
                totalHours <= 6 -> 1
                totalHours <= 12 -> 2
                else -> (totalHours / (labelCount -1)).toInt().coerceAtLeast(1)
            }

            var currentLabelTime = firstLabelTime
            while (currentLabelTime <= maxTime) {
                val x = toOffset(currentLabelTime, 0f).x
                if (x <= size.width - padding) { // Ensure line is within chart bounds
                    drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, padding + chartHeight))
                    val dateLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentLabelTime))
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(dateLabel, x, size.height - padding / 4, textPaint)
                    }
                }
                cal.timeInMillis = currentLabelTime
                cal.add(Calendar.HOUR_OF_DAY, stepHours)
                currentLabelTime = cal.timeInMillis
            }
        } else {
            // --- DAILY LABELS ---
            for (i in 0..labelCount) {
                val x = padding + chartWidth * i / labelCount
                drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, padding + chartHeight))
                val timestamp = (minTime + (timeRange * i / labelCount)).toLong()
                val dateLabel = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        dateLabel,
                        x,
                        size.height - padding / 4,
                        textPaint
                    )
                }
            }
        }

        // Draw threshold lines
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        if (4f >= minValue && 4f <= maxValue) {
            val y = toOffset(minTime, 4f).y
            drawLine(
                color = Color.Blue,
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 2f,
                pathEffect = pathEffect
            )
        }
        if (10f >= minValue && 10f <= maxValue) {
            val y = toOffset(minTime, 10f).y
            drawLine(
                color = Color.Red,
                start = Offset(padding, y),
                end = Offset(padding + chartWidth, y),
                strokeWidth = 2f,
                pathEffect = pathEffect
            )
        }

        // Draw data path
        if (records.size >= 2) {
            (0 until records.size - 1).forEach { i ->
                val p1 = toOffset(records[i].timestamp, records[i].value)
                val p2 = toOffset(records[i + 1].timestamp, records[i + 1].value)
                val value = records[i + 1].value
                val color = when {
                    value <= 4f -> Color.Blue
                    value <= 10f -> Color(0xFF006400) // Dark Green
                    else -> Color.Red
                }
                drawLine(color, p1, p2, strokeWidth = 3f)
            }
        }

        // Draw data points
        recordPoints.clear()
        records.forEach { record ->
            val point = toOffset(record.timestamp, record.value)
            recordPoints[record.id] = point
            val value = record.value
            val color = when {
                value <= 4f -> Color.Blue
                value <= 10f -> Color(0xFF006400) // Dark Green
                else -> Color.Red
            }
            drawCircle(color = color, radius = 8f, center = point)
            drawCircle(color = Color.White, radius = 5f, center = point)

            if (record.id == selectedRecord?.id) {
                drawCircle(color = color.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = point)
            }
        }

        // Draw event lines
        val eventTextPaint = android.graphics.Paint().apply {
            textSize = 10.sp.value * density.density
            textAlign = android.graphics.Paint.Align.LEFT
        }
        val eventBoxPaint = android.graphics.Paint()

        val groupedEvents = mutableListOf<MutableList<EventRecord>>()
        if (events.isNotEmpty()) {
            var currentGroup = mutableListOf(events.first())
            groupedEvents.add(currentGroup)
            for (i in 1 until events.size) {
                val event = events[i]
                val lastEventInGroup = currentGroup.last()
                val timeDiff = event.timestamp - lastEventInGroup.timestamp
                val xDiff = (timeDiff / timeRange) * chartWidth
                if (xDiff < 30.dp.toPx()) {
                    currentGroup.add(event)
                } else {
                    currentGroup = mutableListOf(event)
                    groupedEvents.add(currentGroup)
                }
            }
        }

        groupedEvents.forEach { group ->
            var yOffset = 0f
            val firstEventX = toOffset(group.first().timestamp, 0f).x

            drawLine(
                color = Color.DarkGray.copy(alpha = 0.5f),
                start = Offset(firstEventX, padding),
                end = Offset(firstEventX, padding + chartHeight),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )

            group.forEach { event ->
                val (color, label) = when (event.type) {
                    "INSULIN" -> Color.Blue to "I: ${event.value}u"
                    "CARBS" -> Color(0xFFFFA500) to "C: ${event.value}g"
                    else -> Color.Gray to ""
                }

                eventTextPaint.color = android.graphics.Color.WHITE
                eventBoxPaint.color = color.toArgb()

                val textWidth = eventTextPaint.measureText(label)
                val textHeight = eventTextPaint.descent() - eventTextPaint.ascent()
                val boxPadding = 4.dp.toPx()

                val boxLeft = firstEventX + 4.dp.toPx()
                val boxTop = padding + 4.dp.toPx() + yOffset
                val boxRect = android.graphics.RectF(boxLeft, boxTop, boxLeft + textWidth + 2 * boxPadding, boxTop + textHeight + boxPadding)

                drawIntoCanvas {
                    it.nativeCanvas.drawRoundRect(boxRect, 8.dp.toPx(), 8.dp.toPx(), eventBoxPaint)
                    it.nativeCanvas.drawText(
                        label,
                        boxRect.left + boxPadding,
                        boxRect.top + boxPadding + textHeight - eventTextPaint.descent(),
                        eventTextPaint
                    )
                }
                yOffset += boxRect.height() + 4.dp.toPx()
            }
        }
    }
}


@Composable
fun RecordItem(
    record: BloodSugarRecord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WaterDrop,
                contentDescription = "Blood Sugar Record",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Value: ${record.value}", style = MaterialTheme.typography.bodyLarge)
                Text(text = record.comment, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Record")
            }
        }
    }
}

@Composable
fun EventHistoryItem(
    event: EventRecord,
    onDelete: () -> Unit
) {
    val (color, label, icon) = when (event.type) {
        "INSULIN" -> Triple(Color.Blue, "Insulin: ${event.value}u", Icons.Default.MedicalServices)
        "CARBS" -> Triple(Color(0xFFFFA500), "Carbs: ${event.value}g", Icons.Default.Restaurant)
        else -> Triple(Color.Gray, "Unknown Event", Icons.Default.Info)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Event")
            }
        }
    }
}

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

@Composable
fun LogInputDialog(
    dialogType: DialogType,
    uiState: HomeUiState,
    onDismiss: () -> Unit,
    onSaveRecord: () -> Unit,
    onSaveEvent: () -> Unit,
    onSugarChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onInsulinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialogType == DialogType.SUGAR) "Log Blood Sugar" else "Log Event") },
        text = {
            Column {
                if (dialogType == DialogType.SUGAR) {
                    OutlinedTextField(
                        value = uiState.sugarValue,
                        onValueChange = onSugarChange,
                        label = { Text("Value (mmol/L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = onCommentChange,
                        label = { Text("Comment") }
                    )
                } else {
                    OutlinedTextField(
                        value = uiState.insulinValue,
                        onValueChange = onInsulinChange,
                        label = { Text("Insulin (units)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.carbsValue,
                        onValueChange = onCarbsChange,
                        label = { Text("Carbohydrates (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (dialogType == DialogType.SUGAR) onSaveRecord else onSaveEvent,
                enabled = (dialogType == DialogType.SUGAR && uiState.sugarValue.isNotBlank()) || (dialogType == DialogType.EVENT && (uiState.insulinValue.isNotBlank() || uiState.carbsValue.isNotBlank()))
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