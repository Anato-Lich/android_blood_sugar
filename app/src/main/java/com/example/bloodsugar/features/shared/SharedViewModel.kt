package com.example.bloodsugar.features.shared

import androidx.lifecycle.ViewModel
import com.example.bloodsugar.domain.MealToLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SharedUiState(
    val mealToLog: MealToLog? = null
)
