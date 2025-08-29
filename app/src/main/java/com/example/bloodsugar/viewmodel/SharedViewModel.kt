package com.example.bloodsugar.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bloodsugar.database.FoodItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MealToLog(
    val carbs: Float,
    val insulin: Float?,
    val details: List<String>
)

data class SharedUiState(
    val mealToLog: MealToLog? = null
)

class SharedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SharedUiState())
    val uiState: StateFlow<SharedUiState> = _uiState.asStateFlow()

    fun setMealToLog(meal: MealToLog) {
        _uiState.update { it.copy(mealToLog = meal) }
    }

    fun clearMealToLog() {
        _uiState.update { it.copy(mealToLog = null) }
    }
}