package com.example.bloodsugar.domain

enum class SugarLevelCategory {
    LOW, IN_RANGE, HIGH
}

fun getSugarLevelCategory(value: Float, thresholds: TirThresholds = TirThresholds.Default): SugarLevelCategory {
    return when {
        value <= thresholds.low -> SugarLevelCategory.LOW
        value < thresholds.high -> SugarLevelCategory.IN_RANGE
        else -> SugarLevelCategory.HIGH
    }
}
