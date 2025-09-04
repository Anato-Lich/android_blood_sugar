package com.example.bloodsugar.features.home

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.features.history.getValueColor
import com.example.bloodsugar.domain.ChartData
import com.example.bloodsugar.domain.TirThresholds
import com.example.bloodsugar.database.EventType
import com.example.bloodsugar.domain.Trend
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max

@Composable
fun BloodSugarChart(
    chartData: ChartData?,
    selectedRecord: BloodSugarRecord?,
    onRecordClick: (BloodSugarRecord) -> Unit,
    onDismissTooltip: () -> Unit,
    modifier: Modifier = Modifier,
    isScrubbing: Boolean,
    scrubberPosition: Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit
) {
    val records = chartData?.records ?: emptyList()
    val events = chartData?.events?.sortedBy { it.timestamp } ?: emptyList()
    val activities = chartData?.activities?.sortedBy { it.timestamp } ?: emptyList()
    val density = LocalDensity.current

    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableFloatStateOf(0f) }

    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val textPaint = remember(density) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }

    val eventTextPaint = remember(density) {
        Paint().apply {
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.LEFT
        }
    }
    val eventBoxPaint = remember { Paint() }

    val activityIndicatorTextPaint = remember(density) {
        Paint().apply {
            color = Color.White.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.LEFT
        }
    }

    val recordPoints = remember { mutableStateMapOf<Long, Offset>() }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val tooltipTextPaint = remember(density) {
        Paint().apply {
            color = Color.White.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val tooltipBgPaint = remember {
        Paint().apply {
            color = Color.DarkGray.copy(alpha = 0.8f).toArgb()
        }
    }

    val padding = with(LocalDensity.current) { 16.dp.toPx() }
    val thresholds = TirThresholds.Default

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
                zoomLevel *= zoom
                if (chartData != null) {
                    val chartWidthPx = size.width - (2f * padding)
                    if (chartWidthPx > 0) {
                        val timePerPixel =
                            (chartData.rangeEnd - chartData.rangeStart).toFloat() / chartWidthPx
                        val timeShift = pan.x * timePerPixel
                        panOffset -= timeShift
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

        val yForHigh = toOffset(minTime, thresholds.high).y
        val yForLow = toOffset(minTime, thresholds.low).y
        val chartTopY = padding
        val chartBottomY = canvasHeight - padding
        val chartLeftX = padding

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(errorColor.copy(alpha = 0.05f), errorColor.copy(alpha = 0.01f)),
                startY = chartTopY,
                endY = yForHigh
            ),
            topLeft = Offset(chartLeftX, chartTopY),
            size = Size(canvasWidth - 2 * padding, (yForHigh - chartTopY).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.05f), primaryColor.copy(alpha = 0.01f)),
                startY = yForHigh,
                endY = yForLow
            ),
            topLeft = Offset(chartLeftX, yForHigh),
            size = Size(canvasWidth - 2 * padding, (yForLow - yForHigh).coerceAtLeast(0f))
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.05f), secondaryColor.copy(alpha = 0.01f)),
                startY = yForLow,
                endY = chartBottomY
            ),
            topLeft = Offset(chartLeftX, yForLow),
            size = Size(canvasWidth - 2 * padding, (chartBottomY - yForLow).coerceAtLeast(0f))
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
        if (thresholds.low in minValue..maxValue) {
            val y = toOffset(minTime, thresholds.low).y
            drawLine(secondaryColor, start = Offset(padding, y), end = Offset(canvasWidth - padding, y), strokeWidth = 2f, pathEffect = pathEffect)
        }
        if (thresholds.high in minValue..maxValue) {
            val y = toOffset(minTime, thresholds.high).y
            drawLine(errorColor, start = Offset(padding, y), end = Offset(canvasWidth - padding, y), strokeWidth = 2f, pathEffect = pathEffect)
        }

        chartData.trend?.let { trend ->
            if (records.size < 2) return@let

            val emaValues = trend.ema
            val emaPath = Path()

            records.forEachIndexed { index, record ->
                if (index < emaValues.size) {
                    val point = toOffset(record.timestamp, emaValues[index])
                    if (index == 0) {
                        emaPath.moveTo(point.x, point.y)
                    } else {
                        val prev = records[index - 1]
                        if (index -1 < emaValues.size) {
                            val prevEma = emaValues[index - 1]
                            val prevPoint = toOffset(prev.timestamp, prevEma)
                            val cp1x = prevPoint.x + (point.x - prevPoint.x) / 2f
                            emaPath.cubicTo(cp1x, prevPoint.y, cp1x, point.y, point.x, point.y)
                        }
                    }
                }
            }

            // Draw smooth EMA trend line
            drawPath(
                path = emaPath,
                color = onSurfaceColor.copy(alpha = 0.7f),
                style = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            )

            // Draw Rate of Change Label
            val rate = trend.rateOfChange
            val (arrowText, textColorLocal) = when {
                rate > 3.0f -> Pair("↑%.1f".format(rate), errorColor)
                rate > 1.0f -> Pair("↗%.1f".format(rate), Color(0xFFFF9800)) // Orange
                rate < -3.0f -> Pair("↓%.1f".format(rate.absoluteValue), secondaryColor)
                rate < -1.0f -> Pair("↘%.1f".format(rate.absoluteValue), secondaryColor)
                else -> Pair("→%.1f".format(rate), primaryColor.copy(alpha = 0.7f))
            }

            val lastRecord = records.last()
            val lastPoint = toOffset(lastRecord.timestamp, lastRecord.value)
            val textX = lastPoint.x + 12.dp.toPx()
            val textY = lastPoint.y - 12.dp.toPx()

            val rocPaint = Paint().apply {
                color = textColorLocal.toArgb()
                textSize = with(density) { 12.sp.toPx() }
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }
            val rocBgPaint = Paint().apply {
                color = textColorLocal.copy(alpha=0.3f).toArgb()
                isAntiAlias = true
            }

            // Background bubble
            val textBounds = Rect()
            rocPaint.getTextBounds(arrowText, 0, arrowText.length, textBounds)
            val bgPadding = 4.dp.toPx()
            val bgRect = RectF(
                textX - bgPadding,
                textY + textBounds.top - bgPadding,
                textX + textBounds.width() + bgPadding,
                textY + textBounds.bottom + bgPadding
            )

            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(bgRect, 6.dp.toPx(), 6.dp.toPx(), rocBgPaint)
                it.nativeCanvas.drawText(arrowText, textX, textY, rocPaint)
            }

            // Draw prediction line
            trend.prediction?.let { (predictedTimestamp, predictedValue) ->
                val predictedPoint = toOffset(predictedTimestamp, predictedValue)

                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    start = lastPoint,
                    end = predictedPoint,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 8f,
                    center = predictedPoint
                )
            }
        }

        if (records.size >= 2) {
            val fillPath = Path()

            records.forEachIndexed { i, record ->
                val p1 = toOffset(record.timestamp, record.value)
                if (i == 0) {
                    fillPath.moveTo(p1.x, chartBottomY)
                    fillPath.lineTo(p1.x, p1.y)
                } else {
                    val p0 = toOffset(records[i - 1].timestamp, records[i - 1].value)
                    val cx1 = p0.x + (p1.x - p0.x) / 2f
                    val cy1 = p0.y
                    val cx2 = p0.x + (p1.x - p0.x) / 2f
                    val cy2 = p1.y
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

            val boxRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

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
                val boxRect = RectF(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight)

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
                    EventType.INSULIN -> secondaryColor to "I: ${event.value}u"
                    EventType.CARBS -> tertiaryColor to "C: ${event.value}g"
                }

                eventTextPaint.color = Color.White.toArgb()
                eventBoxPaint.color = color.toArgb()

                val textWidth = eventTextPaint.measureText(label)
                val textHeight = eventTextPaint.descent() - eventTextPaint.ascent()
                val boxPadding = 4.dp.toPx()

                val boxLeft = firstEventX + 4.dp.toPx()
                val boxTop = padding + 4.dp.toPx() + yOffset
                val boxRect = RectF(boxLeft, boxTop, boxLeft + textWidth + 2 * boxPadding, boxTop + textHeight + boxPadding)

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
                val label1 = activity.type.name
                val label2 = "${activity.durationMinutes} min"

                val boxPaint = Paint().apply { color = tertiaryColor.toArgb() }

                val textWidth = max(activityIndicatorTextPaint.measureText(label1), activityIndicatorTextPaint.measureText(label2))
                val textHeight = activityIndicatorTextPaint.descent() - activityIndicatorTextPaint.ascent()
                val boxPadding = 4.dp.toPx()

                val boxHeight = (2 * textHeight) + (2.5f * boxPadding)
                val boxLeft = firstActivityX + 4.dp.toPx()
                val boxBottom = (canvasHeight - padding) - yOffset
                val boxTop = boxBottom - boxHeight

                val boxRect = RectF(boxLeft, boxTop, boxLeft + textWidth + 2 * boxPadding, boxBottom)

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