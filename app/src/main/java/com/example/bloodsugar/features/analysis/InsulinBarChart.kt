package com.example.bloodsugar.features.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.times
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.bloodsugar.database.DailyInsulinDose

@Composable
fun InsulinBarChart(dailyDoses: List<DailyInsulinDose>) {
    if (dailyDoses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No insulin data available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val inputFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val outputFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

    val labels = remember(dailyDoses) {
        dailyDoses.map { dose ->
            runCatching { inputFormat.parse(dose.day) }
                .getOrNull()
                ?.let { outputFormat.format(it) }
                ?: "??"
        }
    }

    val maxInsulinDose = remember(dailyDoses) {
        (dailyDoses.maxOfOrNull { it.total }?.toFloat() ?: 0f).coerceAtLeast(1f)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val labelSizeSp = 10.sp

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Horizontal scroll container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Transparent)
                .horizontalScroll(rememberScrollState()) // ← Key: horizontal scroll
        ) {
            // Calculate total width needed
            val barCount = dailyDoses.size
            val barWidthDp = 40.dp
            val spacingDp = 12.dp
            val totalWidth = (barCount * barWidthDp + (barCount - 1) * spacingDp) + 32.dp // padding
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(200.dp)
            ) {
                val chartHeight = size.height
                val barWidth = barWidthDp.toPx()
                val spacing = spacingDp.toPx()

                val barYStart = chartHeight - 30.dp.toPx()
                val barYEnd = 20.dp.toPx()
                val scale = (barYStart - barYEnd) / maxInsulinDose

                // Draw bars and labels
                dailyDoses.forEachIndexed { index, dose ->
                    val barHeight = dose.total * scale
                    val xPos = 20.dp.toPx() + index * (barWidth + spacing)

                    val rect = RectF(
                        xPos,
                        barYStart - barHeight,
                        xPos + barWidth,
                        barYStart
                    )

                    val labelPaint = Paint().apply {
                        color = textColor.toArgb()
                        textSize = with(density) { labelSizeSp.toPx() }
                        textAlign = Paint.Align.CENTER
                    }
                    // Label on top of bar
                    if (dose.total > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            dose.total.toString(),
                            xPos + barWidth / 2,
                            (barYStart - barHeight) - 12.dp.toPx(),
                            labelPaint
                        )
                    }

                    // Gradient fill
                    drawIntoCanvas { canvas ->
                        val shader = android.graphics.LinearGradient(
                            rect.left, rect.top,
                            rect.left, rect.bottom,
                            intArrayOf(
                                primaryColor.toArgb(),
                                primaryColor.copy(alpha = 0.7f).toArgb()
                            ),
                            null,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        val paint = Paint().apply {
                            this.shader = shader
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            rect,
                            8.dp.toPx(), // corner radius
                            8.dp.toPx(),
                            paint
                        )
                    }

                    // Label below bar
                    drawContext.canvas.nativeCanvas.drawText(
                        labels[index],
                        xPos + barWidth / 2,
                        barYStart + 18.dp.toPx(),
                        labelPaint
                    )
                }
            }
        }
    }
}