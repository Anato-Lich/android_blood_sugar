package com.example.bloodsugar.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    var chartZoomLevel by remember { mutableStateOf(1f) }
    var chartPanOffset by remember { mutableStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubberPosition by remember { mutableStateOf(0f) }

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
            onActivityIntensityChange = { homeViewModel.setActivityIntensity(it) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key Metrics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (uiState.chartData != null) {
                    Metric("Min", uiState.chartData!!.min)
                    Metric("Avg", uiState.chartData!!.avg)
                    Metric("Max", uiState.chartData!!.max)
                } else {
                    Text(
                        "Log a blood sugar reading to see your metrics.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
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
                    val pagerState = rememberPagerState(pageCount = { 3 })
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
                            2 -> TirBar(uiState = uiState, modifier = cardModifier)
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

        // Chart Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                BloodSugarChart(
                    chartData = uiState.chartData,
                    selectedRecord = uiState.selectedRecord,
                    onRecordClick = homeViewModel::onChartRecordSelected,
                    onDismissTooltip = homeViewModel::onChartSelectionDismissed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    zoomLevel = chartZoomLevel,
                    panOffset = chartPanOffset,
                    onZoom = { newZoom -> chartZoomLevel = newZoom },
                    onPan = { newPan -> chartPanOffset = newPan },
                    isScrubbing = isScrubbing,
                    scrubberPosition = scrubberPosition,
                    onScrub = { position ->
                        isScrubbing = true
                        scrubberPosition = position
                    },
                    onScrubEnd = { isScrubbing = false }
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

        // Recent History Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.recentHistoryItems.isEmpty()) {
                    Text("Your recent logs will appear here.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                } else {
                    uiState.recentHistoryItems.forEach { item ->
                        when (item) {
                            is BloodSugarRecord -> RecordItem(record = item, onDelete = { homeViewModel.deleteRecord(item) })
                            is EventRecord -> EventHistoryItem(event = item, onDelete = { homeViewModel.deleteEvent(item) })
                            is ActivityRecord -> ActivityHistoryItem(activity = item, onDelete = { homeViewModel.deleteActivity(item) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("View All History")
                    }
                }
            }
        }
    }
}

@Composable
fun TirBar(uiState: HomeUiState, modifier: Modifier = Modifier) {
    val total = uiState.timeBelowRange + uiState.timeInRange + uiState.timeAboveRange
    if (total == 0f) {
        Row(
            modifier = Modifier
                .padding(vertical = 2.dp, horizontal = 8.dp)
                .height(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                "No data in selected period",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }

        return
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Text(
                "Time In Range",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 8.dp)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.medium)) {
                if (uiState.timeBelowRange > 0) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(uiState.timeBelowRange)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    )
                }
                if (uiState.timeInRange > 0) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(uiState.timeInRange)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                if (uiState.timeAboveRange > 0) {
                    Box(
                        modifier = Modifier.fillMaxHeight().weight(uiState.timeAboveRange)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)){
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Low",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "%.1f%%".format(uiState.timeBelowRange),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Normal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "%.1f%%".format(uiState.timeInRange),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "High",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "%.1f%%".format(uiState.timeAboveRange),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun Metric(label: String, value: Float, modifier: Modifier = Modifier) {
    val valueColor = getValueColor(
        value = value,
        lowColor = MaterialTheme.colorScheme.secondary,
        inRangeColor = MaterialTheme.colorScheme.primary,
        highColor = MaterialTheme.colorScheme.error
    )
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = valueColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 20.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    modifier: Modifier = Modifier,
    zoomLevel: Float = 1f,
    panOffset: Float = 0f,
    onZoom: (Float) -> Unit,
    onPan: (Float) -> Unit,
    isScrubbing: Boolean,
    scrubberPosition: Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit
) {
    val records = chartData?.records ?: emptyList()
    val events = chartData?.events?.sortedBy { it.timestamp } ?: emptyList()
    val activities = chartData?.activities?.sortedBy { it.timestamp } ?: emptyList()
    val density = LocalDensity.current

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            color = onSurfaceColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val eventTextPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.LEFT
        }
    }
    val eventBoxPaint = remember { android.graphics.Paint() }

    val activityIndicatorTextPaint = remember(density) {
        android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.LEFT
        }
    }

    val recordPoints = remember { mutableStateMapOf<Long, Offset>() }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val tooltipTextPaint = remember(density) {
        android.graphics.Paint().apply {
            color = Color.White.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val tooltipBgPaint = remember {
        android.graphics.Paint().apply {
            color = Color.DarkGray.copy(alpha = 0.8f).toArgb()
        }
    }

    val padding = with(LocalDensity.current) { 16.dp.toPx() }

    Canvas(modifier = modifier
        .pointerInput(records, selectedRecord) {
            detectTapGestures(
                onTap = { tapOffset ->
                    var tappedRecordId: Long? = null
                    val tapRadius = with(density) { 16.dp.toPx() }

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
        }
        .pointerInput(chartData, zoomLevel, panOffset) {
            detectTransformGestures { _, pan, zoom, _ ->
                onZoom(zoomLevel * zoom)
                if (chartData != null) {
                    val chartWidthPx = size.width - (2f * padding)
                    if (chartWidthPx > 0) {
                        val timePerPixel =
                            (chartData.rangeEnd - chartData.rangeStart).toFloat() / chartWidthPx
                        val timeShift = pan.x * timePerPixel
                        onPan(panOffset - timeShift)
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> onScrub(offset.x) },
                onDragEnd = { onScrubEnd() },
                onDragCancel = { onScrubEnd() },
                onDrag = { change, _ ->
                    onScrub(change.position.x)
                    change.consume()
                }
            )
        }
    ) {
        if (chartData == null || (records.isEmpty() && events.isEmpty() && activities.isEmpty())) {
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "Log a blood sugar reading to see your chart.",
                    center.x,
                    center.y,
                    textPaint
                )
            }
            return@Canvas
        }

        val canvasWidth = size.width
        val canvasHeight = size.height

        val initialMinTime = chartData.rangeStart
        val initialMaxTime = chartData.rangeEnd
        val initialTimeRange = (initialMaxTime - initialMinTime).toFloat()

        val visibleTimeRange = (initialTimeRange / zoomLevel).coerceIn(0f, initialTimeRange)
        val currentCenterTime = (initialMinTime + initialMaxTime) / 2f + panOffset

        var calculatedMinTime = (currentCenterTime - visibleTimeRange / 2f).toLong()
        var calculatedMaxTime = (currentCenterTime + visibleTimeRange / 2f).toLong()

        if (calculatedMinTime < initialMinTime) {
            calculatedMinTime = initialMinTime
            calculatedMaxTime = (initialMinTime + visibleTimeRange).toLong()
        }
        if (calculatedMaxTime > initialMaxTime) {
            calculatedMaxTime = initialMaxTime
            calculatedMinTime = (initialMaxTime - visibleTimeRange).toLong()
        }

        val minTime = calculatedMinTime
        val maxTime = calculatedMaxTime
        val timeRange = (maxTime - minTime).toFloat()

        val minValue = 0f
        val maxValue = 15f
        val valueRange = max(maxValue - minValue, 1f)

        fun toOffset(timestamp: Long, value: Float): Offset {
            val x = padding + ((timestamp - minTime).toFloat() / timeRange) * (canvasWidth - 2 * padding)
            val y = padding + (canvasHeight - 2 * padding) - ((value.coerceIn(minValue, maxValue) - minValue) / valueRange) * (canvasHeight - 2 * padding)
            return Offset(x, y)
        }

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
                val xDiff = (timeDiff.toFloat() / timeRange) * (canvasWidth - 2 * padding)
                if (xDiff < 30.dp.toPx()) {
                    currentGroup.add(item)
                } else {
                    currentGroup = mutableListOf(item)
                    groupedItems.add(currentGroup)
                }
            }
            return groupedItems
        }

        val yFor10 = toOffset(minTime, 10f).y
        val yFor4 = toOffset(minTime, 4f).y
        val chartTopY = padding
        val chartBottomY = canvasHeight - padding
        val chartLeftX = padding

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(errorColor.copy(alpha = 0.05f), errorColor.copy(alpha = 0.01f)),
                startY = chartTopY,
                endY = yFor10
            ),
            topLeft = Offset(chartLeftX, chartTopY),
            size = Size(canvasWidth - 2 * padding, (yFor10 - chartTopY).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.05f), primaryColor.copy(alpha = 0.01f)),
                startY = yFor10,
                endY = yFor4
            ),
            topLeft = Offset(chartLeftX, yFor10),
            size = Size(canvasWidth - 2 * padding, (yFor4 - yFor10).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.05f), secondaryColor.copy(alpha = 0.01f)),
                startY = yFor4,
                endY = chartBottomY
            ),
            topLeft = Offset(chartLeftX, yFor4),
            size = Size(canvasWidth - 2 * padding, (chartBottomY - yFor4).coerceAtLeast(0f))
        )

        val gridLineColor = Color.LightGray
        val labelCount = 5
        for (i in 0..labelCount) {
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
            val cal = Calendar.getInstance().apply { timeInMillis = minTime }
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
                if (x <= canvasWidth - padding) {
                    drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, canvasHeight - padding))
                    val dateLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentLabelTime))
                    drawIntoCanvas { it.nativeCanvas.drawText(dateLabel, x, canvasHeight - padding / 4, textPaint) }
                }
                cal.timeInMillis = currentLabelTime
                cal.add(Calendar.HOUR_OF_DAY, stepHours)
                currentLabelTime = cal.timeInMillis
            }
        } else {
            for (i in 0..labelCount) {
                val x = padding + (canvasWidth - 2 * padding) * i / labelCount
                drawLine(gridLineColor, start = Offset(x, padding), end = Offset(x, canvasHeight - padding))
                val timestamp = (minTime + (timeRange * i / labelCount)).toLong()
                val dateLabel = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                drawIntoCanvas { it.nativeCanvas.drawText(dateLabel, x, canvasHeight - padding / 4, textPaint) }
            }
        }

        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        if (4f in minValue..maxValue) {
            val y = toOffset(minTime, 4f).y
            drawLine(secondaryColor, start = Offset(padding, y), end = Offset(canvasWidth - padding, y), strokeWidth = 2f, pathEffect = pathEffect)
        }
        if (10f in minValue..maxValue) {
            val y = toOffset(minTime, 10f).y
            drawLine(errorColor, start = Offset(padding, y), end = Offset(canvasWidth - padding, y), strokeWidth = 2f, pathEffect = pathEffect)
        }

        if (records.size >= 2) {
            val fillPath = Path()
            val linePath = Path()

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
            }

            for (i in 0 until records.size - 1) {
                val p0 = toOffset(records[i].timestamp, records[i].value)
                val p1 = toOffset(records[i + 1].timestamp, records[i + 1].value)
                val segmentPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    cubicTo(p0.x + (p1.x - p0.x) / 2f, p0.y, p0.x + (p1.x - p0.x) / 2f, p1.y, p1.x, p1.y)
                }
                val color0 = getValueColor(records[i].value, secondaryColor, primaryColor, errorColor)
                val color1 = getValueColor(records[i + 1].value, secondaryColor, primaryColor, errorColor)
                drawPath(
                    path = segmentPath,
                    brush = Brush.linearGradient(colors = listOf(color0, color1), start = p0, end = p1),
                    style = Stroke(width = 5f)
                )
            }

            val lastPoint = toOffset(records.last().timestamp, records.last().value)
            fillPath.lineTo(lastPoint.x, chartBottomY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                    startY = 0f,
                    endY = chartBottomY
                )
            )
        }

        recordPoints.clear()
        records.forEach { record ->
            val point = toOffset(record.timestamp, record.value)
            recordPoints[record.id] = point
            val color = getValueColor(record.value, secondaryColor, primaryColor, errorColor)
            drawCircle(color = color, radius = 8f, center = point)
            drawCircle(color = Color.White, radius = 5f, center = point)

            if (record.id == selectedRecord?.id) {
                drawCircle(color = color.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = point)
            }
        }

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
                it.nativeCanvas.drawText(valueText, point.x, boxTop + boxPadding + textHeight - tooltipTextPaint.descent(), tooltipTextPaint)
                it.nativeCanvas.drawText(timeText, point.x, boxTop + boxPadding + (textHeight * 2) - tooltipTextPaint.descent(), tooltipTextPaint)
            }
        }

        if (isScrubbing) {
            val scrubberX = scrubberPosition.coerceIn(padding, canvasWidth - padding)
            val scrubberTime = minTime + ((scrubberX - padding) / (canvasWidth - 2 * padding)) * timeRange

            val nextRecordIndex = records.indexOfFirst { it.timestamp >= scrubberTime }
            if (nextRecordIndex > 0) {
                val prevRecord = records[nextRecordIndex - 1]
                val nextRecord = records[nextRecordIndex]

                val t = (scrubberTime - prevRecord.timestamp) / (nextRecord.timestamp - prevRecord.timestamp).toFloat()
                val interpolatedValue = prevRecord.value + t * (nextRecord.value - prevRecord.value)

                val scrubberY = toOffset(scrubberTime.toLong(), interpolatedValue).y

                drawLine(color = onSurfaceColor, start = Offset(scrubberX, padding), end = Offset(scrubberX, canvasHeight - padding), strokeWidth = 2f)
                drawCircle(color = primaryColor, radius = 10f, center = Offset(scrubberX, scrubberY))

                val valueText = "%.1f".format(interpolatedValue)
                val timeText = timeFormat.format(Date(scrubberTime.toLong()))

                val textWidth = max(tooltipTextPaint.measureText(valueText), tooltipTextPaint.measureText(timeText))
                val textHeight = tooltipTextPaint.descent() - tooltipTextPaint.ascent()
                val boxPadding = 8.dp.toPx()
                val boxHeight = (textHeight * 2) + (boxPadding * 2.5f)
                val boxWidth = textWidth + (boxPadding * 2)

                var boxLeft = scrubberX - (boxWidth / 2)
                if (boxLeft < padding) boxLeft = padding
                if (boxLeft + boxWidth > canvasWidth - padding) boxLeft = canvasWidth - padding - boxWidth

                val boxTop = padding
                val boxRect = android.graphics.RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

                drawIntoCanvas {
                    it.nativeCanvas.drawRoundRect(boxRect, 10.dp.toPx(), 10.dp.toPx(), tooltipBgPaint)
                    it.nativeCanvas.drawText(valueText, boxLeft + boxWidth / 2, boxTop + boxPadding + textHeight - tooltipTextPaint.descent(), tooltipTextPaint)
                    it.nativeCanvas.drawText(timeText, boxLeft + boxWidth / 2, boxTop + boxPadding + (textHeight * 2) - tooltipTextPaint.descent(), tooltipTextPaint)
                }
            }
        }

        val groupedEvents = groupItemsByTime(events) { it.timestamp }
        groupedEvents.forEach { group ->
            var yOffset = 0f
            val firstEventX = toOffset(group.first().timestamp, 0f).x

            drawLine(color = Color.DarkGray.copy(alpha = 0.5f), start = Offset(firstEventX, padding), end = Offset(firstEventX, canvasHeight - padding), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))

            group.forEach { event ->
                val (color, label) = when (event.type) {
                    "INSULIN" -> secondaryColor to "I: ${event.value}u"
                    "CARBS" -> tertiaryColor to "C: ${event.value}g"
                    else -> Color.Gray to ""
                }

                eventTextPaint.color = Color.White.toArgb()
                eventBoxPaint.color = color.toArgb()

                val textWidth = eventTextPaint.measureText(label)
                val textHeight = eventTextPaint.descent() - eventTextPaint.ascent()
                val boxPadding = 4.dp.toPx()

                val boxLeft = firstEventX + 4.dp.toPx()
                val boxTop = padding + 4.dp.toPx() + yOffset
                val boxRect = android.graphics.RectF(boxLeft, boxTop, boxLeft + textWidth + 2 * boxPadding, boxTop + textHeight + boxPadding)

                drawIntoCanvas {
                    it.nativeCanvas.drawRoundRect(boxRect, 8.dp.toPx(), 8.dp.toPx(), eventBoxPaint)
                    it.nativeCanvas.drawText(label, boxRect.left + boxPadding, boxRect.top + boxPadding + textHeight - eventTextPaint.descent(), eventTextPaint)
                }
                yOffset += boxRect.height() + 4.dp.toPx()
            }
        }

        val groupedActivities = groupItemsByTime(activities) { it.timestamp }
        groupedActivities.forEach { group ->
            var yOffset = 0f
            val firstActivityX = toOffset(group.first().timestamp, 0f).x

            drawLine(color = tertiaryColor.copy(alpha = 0.5f), start = Offset(firstActivityX, padding), end = Offset(firstActivityX, canvasHeight - padding), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))

            group.reversed().forEach { activity ->
                val label1 = activity.type
                val label2 = "${activity.durationMinutes} min"

                val boxPaint = android.graphics.Paint().apply { color = tertiaryColor.toArgb() }

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
                    it.nativeCanvas.drawText(label1, boxRect.left + boxPadding, boxRect.top + boxPadding + textHeight - activityIndicatorTextPaint.descent(), activityIndicatorTextPaint)
                    it.nativeCanvas.drawText(label2, boxRect.left + boxPadding, boxRect.top + (2 * boxPadding) + (2 * textHeight) - activityIndicatorTextPaint.descent(), activityIndicatorTextPaint)
                }
                yOffset += boxHeight + 4.dp.toPx()
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
