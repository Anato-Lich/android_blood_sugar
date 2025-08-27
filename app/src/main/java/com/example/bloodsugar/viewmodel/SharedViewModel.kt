package com.example.bloodsugar.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SharedUiState(
    val latestInsulinDose: String = "",
    val latestCarbs: String = ""
)

class SharedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SharedUiState())
    val uiState: StateFlow<SharedUiState> = _uiState.asStateFlow()

    fun setLatestInsulinDose(dose: Float?) {
        _uiState.update { it.copy(latestInsulinDose = dose?.let { "%.2f".format(it) } ?: "") }
    }

    fun setLatestCarbs(carbs: Float?) {
        _uiState.update { it.copy(latestCarbs = carbs?.let { "%.2f".format(it) } ?: "") }
    }
}