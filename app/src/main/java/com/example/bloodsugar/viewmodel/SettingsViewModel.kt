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
    val dailyCarbsGoal: String = "",
    val insulinDoseAccuracy: String = "0.5",
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
                settingsDataStore.carbsPerBu,
                settingsDataStore.dailyCarbsGoal,
                settingsDataStore.insulinDoseAccuracy
            ) { values ->
                val breakfast = values[0]
                val dinner = values[1]
                val supper = values[2]
                val carbsPerBu = values[3]
                val dailyCarbsGoal = values[4]
                val insulinDoseAccuracy = values[5]

                _uiState.update {
                    it.copy(
                        breakfastCoefficient = breakfast.toString(),
                        dinnerCoefficient = dinner.toString(),
                        supperCoefficient = supper.toString(),
                        carbsPerBu = carbsPerBu.toString(),
                        dailyCarbsGoal = dailyCarbsGoal.toString(),
                        insulinDoseAccuracy = insulinDoseAccuracy.toString(),
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
        _uiState.update { it.copy(dinnerCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setSupperCoefficient(coefficient: String) {
        _uiState.update { it.copy(supperCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setCarbsPerBu(carbs: String) {
        _uiState.update { it.copy(carbsPerBu = carbs, hasUnsavedChanges = true) }
    }

    fun setDailyCarbsGoal(goal: String) {
        _uiState.update { it.copy(dailyCarbsGoal = goal, hasUnsavedChanges = true) }
    }

    fun setInsulinDoseAccuracy(accuracy: String) {
        _uiState.update { it.copy(insulinDoseAccuracy = accuracy, hasUnsavedChanges = true) }
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
            settingsDataStore.saveDailyCarbsGoal(
                goal = _uiState.value.dailyCarbsGoal.replace(',', '.').toFloatOrNull() ?: 200f
            )
            settingsDataStore.saveInsulinDoseAccuracy(
                accuracy = _uiState.value.insulinDoseAccuracy.replace(',', '.').toFloatOrNull() ?: 0.5f
            )
            _uiState.update { it.copy(hasUnsavedChanges = false) }
        }
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }
}