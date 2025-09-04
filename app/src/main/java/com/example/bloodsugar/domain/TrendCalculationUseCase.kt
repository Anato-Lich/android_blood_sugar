package com.example.bloodsugar.domain

import com.example.bloodsugar.database.BloodSugarRecord
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class TrendCalculationUseCase {
    fun calculateTrend(records: List<BloodSugarRecord>): Trend? {
        if (records.size < 2) return null

        val sortedRecords = records.sortedBy { it.timestamp }

        // Use last 6 hours of data for more relevant trend, extend if needed
        val now = System.currentTimeMillis()
        var recentRecords = sortedRecords
            .filter { now - it.timestamp <= TimeUnit.HOURS.toMillis(6) }

        if (recentRecords.size < 2) {
            recentRecords = sortedRecords.filter { now - it.timestamp <= TimeUnit.HOURS.toMillis(12) }
        }

        if (recentRecords.size < 2) {
            recentRecords = sortedRecords.filter { now - it.timestamp <= TimeUnit.HOURS.toMillis(24) }
        }
        
        if (recentRecords.size < 2) {
            recentRecords = sortedRecords.takeLast(10)
        }

        if (recentRecords.size < 2) return null

        // Linear Regression
        val startTime = recentRecords.first().timestamp
        val n = recentRecords.size
        val sumX = recentRecords.sumOf { (it.timestamp - startTime).toDouble() }
        val sumY = recentRecords.sumOf { it.value.toDouble() }
        val sumXY = recentRecords.sumOf { (it.timestamp - startTime).toDouble() * it.value.toDouble() }
        val sumX2 = recentRecords.sumOf { (it.timestamp - startTime).toDouble() * (it.timestamp - startTime).toDouble() }

        val denominator = (n * sumX2 - sumX * sumX)
        if (denominator.absoluteValue < 1e-9) return null

        val slopeMs = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slopeMs * sumX) / n

        val slopeHour = slopeMs * 3_600_000.0

        // Rate of change from last two points
        val lastTwo = recentRecords.takeLast(2)
        val rateOfChange = if (lastTwo.size == 2) {
            val timeDiffHours = (lastTwo[1].timestamp - lastTwo[0].timestamp) / 3_600_000f
            if (timeDiffHours > 0) (lastTwo[1].value - lastTwo[0].value) / timeDiffHours else 0f
        } else 0f

        // Prediction
        val predictionMinutes = 60L
        val lastTimestamp = recentRecords.last().timestamp
        val predictionTimestamp = lastTimestamp + TimeUnit.MINUTES.toMillis(predictionMinutes)
        val predictionTimeSinceStart = predictionTimestamp - startTime

        val clampedSlopeHour = slopeHour.coerceIn(-15.0, 15.0)
        val clampedSlopeMs = clampedSlopeHour / 3_600_000.0

        val predictedValue = (clampedSlopeMs * predictionTimeSinceStart + intercept).toFloat()
        val clampedPredictedValue = predictedValue.coerceIn(1.0f, 25.0f)

        val prediction = predictionTimestamp to clampedPredictedValue

        // EMA
        val ema = computeEMA(sortedRecords.map { it.value })

        return Trend(
            slope = clampedSlopeHour.toFloat(),
            intercept = intercept.toFloat(),
            startTime = startTime,
            rateOfChange = rateOfChange,
            prediction = prediction,
            ema = ema
        )
    }

    private fun computeEMA(values: List<Float>, alpha: Double = 0.3): List<Float> {
        if (values.isEmpty()) return emptyList()
        val ema = mutableListOf(values.first())
        for (i in 1 until values.size) {
            val prev = ema[i - 1].toDouble()
            val current = values[i].toDouble()
            ema.add((alpha * current + (1 - alpha) * prev).toFloat())
        }
        return ema
    }
}
