package com.example.bloodsugar.features.analysis

import com.example.bloodsugar.database.DailyInsulinDose

data class AnalysisUiState(
    val timeInRange: Float = 0f,
    val timeAboveRange: Float = 0f,
    val timeBelowRange: Float = 0f,
    val veryLow: Float = 0f,
    val low: Float = 0f,
    val high: Float = 0f,
    val veryHigh: Float = 0f,
    val selectedPeriod: Int = 7, // Default to 7 days
    val dailyInsulin: List<DailyInsulinDose> = emptyList()
)
