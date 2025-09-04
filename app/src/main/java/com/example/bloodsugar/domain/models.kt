package com.example.bloodsugar.domain

import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import java.util.UUID

data class TirResult(
    val veryLow: Float = 0f,
    val low: Float = 0f,
    val timeInRange: Float = 0f,
    val high: Float = 0f,
    val veryHigh: Float = 0f
) {
    val totalBelowRange: Float get() = veryLow + low
    val totalAboveRange: Float get() = high + veryHigh
}

data class TirThresholds(
    val veryLow: Float = 3.0f,
    val low: Float = 4.0f,
    val high: Float = 10.0f,
    val veryHigh: Float = 13.9f
) {
    companion object {
        val Default = TirThresholds()
    }
}

data class Trend(
    val slope: Float, // units per hour
    val intercept: Float,
    val startTime: Long, // for prediction formula
    val rateOfChange: Float, // units per hour
    val prediction: Pair<Long, Float>? = null,
    val ema: List<Float> = emptyList()
)

data class ChartData(
    val records: List<BloodSugarRecord>,
    val events: List<EventRecord>,
    val activities: List<ActivityRecord>,
    val avg: Float,
    val min: Float,
    val max: Float,
    val rangeStart: Long,
    val rangeEnd: Long,
    val trend: Trend? = null
)

enum class MealType {
    BREAKFAST,
    DINNER,
    SUPPER
}

data class MealComponent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingValue: String,
    val useGrams: Boolean,
    val carbs: Float,
    val foodItemId: Long? = null
)