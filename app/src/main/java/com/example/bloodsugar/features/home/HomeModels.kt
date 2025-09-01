package com.example.bloodsugar.features.home

import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.FoodItem
import com.example.bloodsugar.domain.ChartData

enum class FilterType {
    TODAY,
    THREE_DAYS,
    SEVEN_DAYS,
    CUSTOM
}

enum class DialogType {
    SUGAR,
    EVENT,
    ACTIVITY
}

data class HomeUiState(
    val sugarValue: String = "",
    val comment: String = "",
    val insulinValue: String = "",
    val carbsValue: String = "",
    val records: List<BloodSugarRecord> = emptyList(),
    val events: List<EventRecord> = emptyList(),
    val activities: List<ActivityRecord> = emptyList(),
    val historyItems: List<Any> = emptyList(),
    val recentHistoryItems: List<Any> = emptyList(),
    val chartData: ChartData? = null,
    val selectedFilter: FilterType = FilterType.TODAY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val shownDialog: DialogType? = null,
    val selectedRecord: BloodSugarRecord? = null,
    val activityType: String = "Walking",
    val activityDuration: String = "",
    val activityIntensity: String = "Medium",
    val estimatedA1c: String = "N/A",
    val todaysCarbs: Float = 0f,
    val dailyCarbsGoal: Float = 200f,
    val foodItems: List<FoodItem> = emptyList(),
    val timeInRange: Float = 0f,
    val timeAboveRange: Float = 0f,
    val timeBelowRange: Float = 0f,
    val veryLow: Float = 0f,
    val low: Float = 0f,
    val high: Float = 0f,
    val veryHigh: Float = 0f,
    val scrollToHistory: Boolean = false,
    val avgDailyInsulin: Float? = null
)
