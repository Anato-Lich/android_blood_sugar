package com.example.bloodsugar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.FoodItem
import com.example.bloodsugar.viewmodel.ChartData
import com.example.bloodsugar.viewmodel.DialogType
import com.example.bloodsugar.viewmodel.FilterType
import com.example.bloodsugar.viewmodel.HomeUiState
import com.example.bloodsugar.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

private fun getValueColor(value: Float): Color {
    return when {
        value < 4f -> Color.Blue
        value <= 10f -> Color(0xFF006400) // Dark Green
        else -> Color.Red
    }
}

@Composable
private fun getActivityIcon(activityType: String): ImageVector {
    return when (activityType) {
        "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
        "Gym" -> Icons.Default.FitnessCenter
        else -> Icons.Default.FitnessCenter
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

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
            onSaveActivity = {
                homeViewModel.saveActivity()
                homeViewModel.onDialogDismiss()
            },
            onSugarChange = { homeViewModel.setSugarValue(it) },
            onCommentChange = { homeViewModel.setComment(it) },
            onInsulinChange = { homeViewModel.setInsulinValue(it) },
            onCarbsChange = { homeViewModel.setCarbsValue(it) },
            onActivityTypeChange = { homeViewModel.setActivityType(it) },
            onActivityDurationChange = { homeViewModel.setActivityDuration(it) },
            onActivityIntensityChange = { homeViewModel.setActivityIntensity(it) },
            onFoodSelected = { homeViewModel.onFoodSelected(it) },
            onFoodServingChange = { useGrams, value -> homeViewModel.calculateCarbsForLog(value, useGrams) }
        )
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            uiState.chartData?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Metric("Avg", it.avg, modifier = Modifier.weight(1f))
                    Metric("Min", it.min, modifier = Modifier.weight(1f))
                    Metric("Max", it.max, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                    BloodSugarChart(
                        chartData = uiState.chartData,
                        selectedRecord = uiState.selectedRecord,
                        onRecordClick = homeViewModel::onChartRecordSelected,
                        onDismissTooltip = homeViewModel::onChartSelectionDismissed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )

                    var showFilterMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)) {
                        Button(onClick = { showFilterMenu = true }) {
                            val filterText = when (uiState.selectedFilter) {
                                FilterType.TODAY -> "Today"
                                FilterType.THREE_DAYS -> "3 Days"
                                FilterType.SEVEN_DAYS -> "7 Days"
                                FilterType.CUSTOM -> "Custom"
                            }
                            Text(text = "Filter: $filterText")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            val filterTypes = listOf(FilterType.TODAY, FilterType.THREE_DAYS, FilterType.SEVEN_DAYS, FilterType.CUSTOM)
                            filterTypes.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (filter) {
                                                FilterType.TODAY -> "Today"
                                                FilterType.THREE_DAYS -> "3 Days"
                                                FilterType.SEVEN_DAYS -> "7 Days"
                                                FilterType.CUSTOM -> "Custom"
                                            }
                                        )
                                    },
                                    onClick = {
                                        showFilterMenu = false
                                        if (filter == FilterType.CUSTOM) {
                                            showCustomDateRangePicker = true
                                        } else {
                                            homeViewModel.setFilter(filter)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { page ->
                        val cardModifier = Modifier.fillMaxWidth()
                        when (page) {
                            0 -> Hba1cMetric(uiState.estimatedA1c, modifier = cardModifier)
                            1 -> CarbsProgressMetric(
                                consumed = uiState.todaysCarbs,
                                goal = uiState.dailyCarbsGoal,
                                modifier = cardModifier
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        Modifier.height(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("History", style = MaterialTheme.typography.headlineSmall)
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
                is ActivityRecord -> {
                    ActivityHistoryItem(
                        activity = item,
                        onDelete = { homeViewModel.deleteActivity(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun Metric(label: String, value: Float, modifier: Modifier = Modifier) {
    val valueColor = getValueColor(value)
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = valueColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "%.1f".format(value), style = MaterialTheme.typography.titleLarge, color = valueColor)
        }
    }
}

@Composable
fun Hba1cMetric(value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Est. HbA1c", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "An estimate based on your 90-day average. Consult a doctor for official results.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

@Composable
fun CarbsProgressMetric(consumed: Float, goal: Float, modifier: Modifier = Modifier) {
    val progress = if (goal > 0) (consumed / goal) else 0f
    val color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 6.dp,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Text(
                    text = "${consumed.toInt()}g",
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Daily Carb Goal", style = MaterialTheme.typography.titleMedium)
                Text("Goal: ${goal.toInt()}g", style = MaterialTheme.typography.bodyMedium)
            }
        }
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
    val activities = chartData?.activities?.sortedBy { it.timestamp } ?: emptyList()
    val density = LocalDensity.current

    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12.sp.value * density.density
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val eventTextPaint = remember {
        android.graphics.Paint().apply {
            textSize = 10.sp.value * density.density
            textAlign = android.graphics.Paint.Align.LEFT
        }
    }
    val eventBoxPaint = remember { android.graphics.Paint() }

    val recordPoints = remember { mutableStateMapOf<Long, Offset>() }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12.sp.value * density.density
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val tooltipBgPaint = remember {
        android.graphics.Paint().apply {
            color = Color.DarkGray.copy(alpha = 0.8f).toArgb()
        }
    }

    Canvas(modifier = modifier.pointerInput(records) {
        detectTapGestures(
            onTap = { tapOffset ->
                var tappedRecordId: Long? = null
                val tapRadius = 16.dp.toPx()

                recordPoints.forEach { (id, pointOffset) ->
                    if ((tapOffset - pointOffset).getDistance() < tapRadius) {
                        tappedRecordId = id
                    }
                }

                val tappedRecord = tappedRecordId?.let { id ->
                    records.find { it.id == id }
                }

                if (tappedRecord != null) {
                    onRecordClick(tappedRecord)
                } else {
                    if (selectedRecord != null) {
                        onDismissTooltip()
                    }
                }
            }
        )
    }) {
        if (chartData == null || (records.isEmpty() && events.isEmpty() && activities.isEmpty())) {
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
        val canvasWidth = size.width
        val canvasHeight = size.height

        val minTime = chartData.rangeStart
        val maxTime = chartData.rangeEnd
        val timeRange = (maxTime - minTime).toFloat()

        val minValue = 0f
        val maxValue = 15f
        val valueRange = max(maxValue - minValue, 1f)

        // Draw zone backgrounds
        val yFor10 = padding + (canvasHeight - 2 * padding) - ((10f - minValue) / valueRange) * (canvasHeight - 2 * padding)
        val yFor4 = padding + (canvasHeight - 2 * padding) - ((4f - minValue) / valueRange) * (canvasHeight - 2 * padding)
        val chartTopY = padding
        val chartBottomY = canvasHeight - padding
        val chartLeftX = padding

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Red.copy(alpha = 0.05f), Color.Red.copy(alpha = 0.01f)),
                startY = chartTopY,
                endY = yFor10
            ),
            topLeft = Offset(chartLeftX, chartTopY),
            size = Size(canvasWidth - 2 * padding, (yFor10 - chartTopY).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF006400).copy(alpha = 0.05f), Color(0xFF006400).copy(alpha = 0.01f)),
                startY = yFor10,
                endY = yFor4
            ),
            topLeft = Offset(chartLeftX, yFor10),
            size = Size(canvasWidth - 2 * padding, (yFor4 - yFor10).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Blue.copy(alpha = 0.05f), Color.Blue.copy(alpha = 0.01f)),
                startY = yFor4,
                endY = chartBottomY
            ),
            topLeft = Offset(chartLeftX, yFor4),
            size = Size(canvasWidth - 2 * padding, (chartBottomY - yFor4).coerceAtLeast(0f))
        )

        // Function to transform data points to canvas coordinates
        fun toOffset(timestamp: Long, value: Float): Offset {
            val x = padding + ((timestamp - minTime) / timeRange) * (canvasWidth - 2 * padding)
            val y = padding + (canvasHeight - 2 * padding) - ((value.coerceIn(minValue, maxValue) - minValue) / valueRange) * (canvasHeight - 2 * padding)
            return Offset(x, y)
        }

        // Generic function to group items by time proximity to avoid clutter
        fun <T> groupItemsByTime(items: List<T>, getTimestamp: (T) -> Long): List<List<T>> {
            if (items.isEmpty()) return emptyList()
            val sortedItems = items.sortedBy { getTimestamp(it) }
            val groupedItems = mutableListOf<MutableList<T>>()
            var currentGroup = mutableListOf(sortedItems.first())
            groupedItems.add(currentGroup)
            for (i in 1 until sortedItems.size) {
                val item = sortedItems[i]
                val lastItemInGroup = currentGroup.last()
                val timeDiff = getTimestamp(item) - getTimestamp(lastItemInGroup)
                val xDiff = (timeDiff / timeRange) * (canvasWidth - 2 * padding)
                if (xDiff < 30.dp.toPx()) {
                    currentGroup.add(item)
                } else {
                    currentGroup = mutableListOf(item)
                    groupedItems.add(currentGroup)
                }
            }
            return groupedItems
        }

        // Draw grid lines and labels
        val gridLineColor = Color.LightGray
        val labelCount = 5
        for (i in 0..labelCount) {
            // Horizontal lines and Y-axis labels
            val y = padding + (canvasHeight - 2 * padding) * i / labelCount
            drawLine(gridLineColor, start = Offset(padding, y), end = Offset(canvasWidth - padding, y))
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

        val displayTimeRange = maxTime - minTime
        if (displayTimeRange <= TimeUnit.HOURS.toMillis(26)) {
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
                if (x <= canvasWidth - padding) { // Ensure line is within chart bounds
                    drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, canvasHeight - padding))
                    val dateLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentLabelTime))
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(dateLabel, x, canvasHeight - padding / 4, textPaint)
                    }
                }
                cal.timeInMillis = currentLabelTime
                cal.add(Calendar.HOUR_OF_DAY, stepHours)
                currentLabelTime = cal.timeInMillis
            }
        } else {
            for (i in 0..labelCount) {
                val x = padding + (canvasWidth - 2 * padding) * i / labelCount
                drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, canvasHeight - padding))
                val timestamp = (minTime + ((maxTime - minTime) * i / labelCount)).toLong()
                val dateLabel = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        dateLabel,
                        x,
                        canvasHeight - padding / 4,
                        textPaint
                    )
                }
            }
        }

        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        if (4f >= minValue && 4f <= maxValue) {
            val y = toOffset(minTime, 4f).y
            drawLine(
                color = Color.Blue,
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding, y),
                strokeWidth = 2f,
                pathEffect = pathEffect
            )
        }
        if (10f >= minValue && 10f <= maxValue) {
            val y = toOffset(minTime, 10f).y
            drawLine(
                color = Color.Red,
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding, y),
                strokeWidth = 2f,
                pathEffect = pathEffect
            )
        }

        // Draw data path
        if (records.size >= 2) {
            val linePath = Path()
            val fillPath = Path()

            records.forEachIndexed { i, record ->
                val p1 = toOffset(record.timestamp, record.value)
                if (i == 0) {
                    linePath.moveTo(p1.x, p1.y)
                    fillPath.moveTo(p1.x, chartBottomY)
                    fillPath.lineTo(p1.x, p1.y)
                } else {
                    val p0 = toOffset(records[i - 1].timestamp, records[i - 1].value)
                    val cx1 = p0.x + (p1.x - p0.x) / 2f
                    val cy1 = p0.y
                    val cx2 = p0.x + (p1.x - p0.x) / 2f
                    val cy2 = p1.y
                    linePath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    fillPath.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }

                val value = record.value
                val color = when {
                    value <= 4f -> Color.Blue
                    value <= 10f -> Color(0xFF006400) // Dark Green
                    else -> Color.Red
                }

                if (i > 0) {
                    val pPrev = toOffset(records[i-1].timestamp, records[i-1].value)
                    val prevColor = when {
                        records[i-1].value <= 4f -> Color.Blue
                        records[i-1].value <= 10f -> Color(0xFF006400)
                        else -> Color.Red
                    }
                    drawPath(
                        path = linePath,
                        brush = Brush.linearGradient(colors = listOf(prevColor, color), start = pPrev, end = p1),
                        style = Stroke(width = 5f)
                    )
                }
            }

            val lastPoint = toOffset(records.last().timestamp, records.last().value)
            fillPath.lineTo(lastPoint.x, chartBottomY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.LightGray.copy(alpha = 0.4f), Color.Transparent),
                    startY = 0f,
                    endY = chartBottomY
                )
            )
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

        // Draw tooltip for selected record
        selectedRecord?.let { record ->
            val point = toOffset(record.timestamp, record.value)
            val valueText = "%.1f".format(record.value)
            val timeText = timeFormat.format(Date(record.timestamp))

            val textWidth = max(tooltipTextPaint.measureText(valueText), tooltipTextPaint.measureText(timeText))
            val textHeight = tooltipTextPaint.descent() - tooltipTextPaint.ascent()
            val boxPadding = 8.dp.toPx()
            val boxHeight = (textHeight * 2) + (boxPadding * 2.5f)
            val boxWidth = textWidth + (boxPadding * 2)

            val boxTop = point.y - boxHeight - 18.dp.toPx()
            val boxLeft = point.x - (boxWidth / 2)

            val boxRect = android.graphics.RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(boxRect, 10.dp.toPx(), 10.dp.toPx(), tooltipBgPaint)
                it.nativeCanvas.drawText(
                    valueText,
                    point.x,
                    boxTop + boxPadding + textHeight - tooltipTextPaint.descent(),
                    tooltipTextPaint
                )
                it.nativeCanvas.drawText(
                    timeText,
                    point.x,
                    boxTop + boxPadding + (textHeight * 2) - tooltipTextPaint.descent(),
                    tooltipTextPaint
                )
            }
        }

        // Draw event lines
        val groupedEvents = groupItemsByTime(events) { it.timestamp }
        groupedEvents.forEach { group ->
            var yOffset = 0f
            val firstEventX = toOffset(group.first().timestamp, 0f).x

            drawLine(
                color = Color.DarkGray.copy(alpha = 0.5f),
                start = Offset(firstEventX, padding),
                end = Offset(firstEventX, canvasHeight - padding),
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

        // Draw activity indicators
        val groupedActivities = groupItemsByTime(activities) { it.timestamp }
        groupedActivities.forEach { group ->
            var yOffset = 0f
            val firstActivityX = toOffset(group.first().timestamp, 0f).x

            drawLine(
                color = tertiaryColor.copy(alpha = 0.5f),
                start = Offset(firstActivityX, padding),
                end = Offset(firstActivityX, canvasHeight - padding),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )

            group.reversed().forEach { activity ->
                val label1 = activity.type
                val label2 = "${activity.durationMinutes} min"

                val activityIndicatorTextPaint = android.graphics.Paint().apply {
                    color = Color.White.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                val boxPaint = android.graphics.Paint().apply {
                    color = tertiaryColor.toArgb()
                }

                val textWidth = max(activityIndicatorTextPaint.measureText(label1), activityIndicatorTextPaint.measureText(label2))
                val textHeight = activityIndicatorTextPaint.descent() - activityIndicatorTextPaint.ascent()
                val boxPadding = 4.dp.toPx()

                val boxHeight = (2 * textHeight) + (2.5f * boxPadding)
                val boxLeft = firstActivityX + 4.dp.toPx()
                val boxBottom = (canvasHeight - padding) - yOffset
                val boxTop = boxBottom - boxHeight

                val boxRect = android.graphics.RectF(boxLeft, boxTop, boxLeft + textWidth + 2 * boxPadding, boxBottom)

                drawIntoCanvas {
                    it.nativeCanvas.drawRoundRect(boxRect, 8.dp.toPx(), 8.dp.toPx(), boxPaint)
                    it.nativeCanvas.drawText(
                        label1,
                        boxRect.left + boxPadding,
                        boxRect.top + boxPadding + textHeight - activityIndicatorTextPaint.descent(),
                        activityIndicatorTextPaint
                    )
                    it.nativeCanvas.drawText(
                        label2,
                        boxRect.left + boxPadding,
                        boxRect.top + (2 * boxPadding) + (2 * textHeight) - activityIndicatorTextPaint.descent(),
                        activityIndicatorTextPaint
                    )
                }
                yOffset += boxHeight + 4.dp.toPx()
            }
        }
    }
}


@Composable
fun RecordItem(
    record: BloodSugarRecord,
    onDelete: () -> Unit
) {
    val valueColor = getValueColor(record.value)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp) // Adjust height to be dynamic if possible
                    .background(valueColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = "Blood Sugar Record",
                        tint = valueColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "%.1f mmol/L".format(record.value),
                            style = MaterialTheme.typography.titleLarge,
                            color = valueColor
                        )
                        if (record.comment.isNotBlank()) {
                            Text(
                                text = record.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Record")
                }
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
        "INSULIN" -> Triple(Color(0xFF5C6BC0), "Insulin", Icons.Default.MedicalServices) // Indigo
        "CARBS" -> Triple(Color(0xFFFFA726), "Carbs", Icons.Default.Restaurant) // Orange
        else -> Triple(Color.Gray, "Unknown Event", Icons.Default.Info)
    }
    val valueText = when(event.type) {
        "INSULIN" -> "%.1fu".format(event.value)
        "CARBS" -> "%.1fg".format(event.value)
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge,
                            color = color
                        )
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (event.foodServing != null) {
                            Text(
                                text = event.foodServing,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (event.foodName != null) {
                            Text(
                                text = "(${event.foodName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryItem(activity: ActivityRecord, onDelete: () -> Unit) {
    val color = MaterialTheme.colorScheme.tertiary
    val icon = getActivityIcon(activity.type)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp)
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = "Activity",
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = activity.type,
                            style = MaterialTheme.typography.titleLarge,
                            color = color
                        )
                        Text(
                            text = "${activity.durationMinutes} min (${activity.intensity})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(activity.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Activity")
                }
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
    onFoodSelected: (FoodItem?) -> Unit,
    onFoodServingChange: (Boolean, String) -> Unit
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
                        val isMultiItemMeal = uiState.foodServingValue.contains("\n")

                        if (isMultiItemMeal) {
                            Text("Meal Details", style = MaterialTheme.typography.titleMedium)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) { 
                                Text(uiState.foodServingValue, style = MaterialTheme.typography.bodySmall)
                            }

                        } else {
                            var foodExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = foodExpanded, onExpandedChange = {foodExpanded = !foodExpanded}) {
                                OutlinedTextField(
                                    value = uiState.selectedFood?.name ?: "",
                                    onValueChange = {},
                                    label = { Text("Select Food (Optional)") },
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = foodExpanded, onDismissRequest = { foodExpanded = false }) {
                                    DropdownMenuItem(text = { Text("None (Manual Entry)") }, onClick = {
                                        onFoodSelected(null)
                                        foodExpanded = false
                                    })
                                    uiState.foodItems.forEach { food ->
                                        DropdownMenuItem(text = { Text(food.name) }, onClick = {
                                            onFoodSelected(food)
                                            foodExpanded = false
                                        })
                                    }
                                }
                            }
                            if (uiState.selectedFood != null) {
                                var useGrams by remember { mutableStateOf(uiState.useGramsInDialog) }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (useGrams) {
                                        Button(onClick = { /* Selected */ }) { Text("Grams") }
                                        OutlinedButton(onClick = { useGrams = false }) { Text("Servings") }
                                    } else {
                                        OutlinedButton(onClick = { useGrams = true }) { Text("Grams") }
                                        Button(onClick = { /* Selected */ }) { Text("Servings") }
                                    }
                                }
                                OutlinedTextField(
                                    value = uiState.foodServingValue,
                                    onValueChange = { onFoodServingChange(useGrams, it) },
                                    label = { Text(if (useGrams) "Weight (g)" else "Number of Servings") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = uiState.carbsValue,
                            onValueChange = onCarbsChange,
                            label = { Text("Carbohydrates (grams)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            readOnly = uiState.selectedFood != null || isMultiItemMeal
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
