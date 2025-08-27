package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val breakfastCoefficient: String = "",
    val dinnerCoefficient: String = "",
    val supperCoefficient: String = "",
    val carbsPerBu: String = "",
    val hasUnsavedChanges: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = androidx.work.WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.breakfastCoefficient,
                settingsDataStore.dinnerCoefficient,
                settingsDataStore.supperCoefficient,
                settingsDataStore.carbsPerBu
            ) { breakfast, dinner, supper, carbsPerBu ->
                _uiState.update {
                    it.copy(
                        breakfastCoefficient = breakfast.toString(),
                        dinnerCoefficient = dinner.toString(),
                        supperCoefficient = supper.toString(),
                        carbsPerBu = carbsPerBu.toString(),
                        hasUnsavedChanges = false
                    )
                }
            }.collect{}
        }
    }

    fun setBreakfastCoefficient(coefficient: String) {
        _uiState.update { it.copy(breakfastCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setDinnerCoefficient(coefficient: String) {
        _uiState.update { it.copy(dinnerCoefficient = coefficient) }
    }

    fun setSupperCoefficient(coefficient: String) {
        _uiState.update { it.copy(supperCoefficient = coefficient) }
    }

    fun setCarbsPerBu(carbs: String) {
        _uiState.update { it.copy(carbsPerBu = carbs, hasUnsavedChanges = true) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsDataStore.saveCoefficients(
                breakfast = _uiState.value.breakfastCoefficient.replace(',', '.').toFloatOrNull() ?: 0f,
                dinner = _uiState.value.dinnerCoefficient.replace(',', '.').toFloatOrNull() ?: 0f,
                supper = _uiState.value.supperCoefficient.replace(',', '.').toFloatOrNull() ?: 0f
            )
            settingsDataStore.saveCarbsPerBu(
                carbs = _uiState.value.carbsPerBu.replace(',', '.').toFloatOrNull() ?: 10f
            )
            _uiState.update { it.copy(hasUnsavedChanges = false) }
        }
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }
}