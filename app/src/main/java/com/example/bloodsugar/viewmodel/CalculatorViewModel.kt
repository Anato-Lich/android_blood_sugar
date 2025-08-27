package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val carbs: String = "",
    val insulinDose: Float? = null,
    val insulinDoseForCarbs: String = "",
    val calculatedCarbs: Float? = null,
    val insulinDoseForRemainingCarbs: String = "",
    val plannedCarbs: String = "",
    val remainingCarbs: Float? = null,
    val totalCarbsForDose: Float? = null,
    val calculationId: Int = 0,
    val lastCalculationSource: Int = 0 // 1, 2, or 3
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    fun setCarbs(carbs: String) {
        _uiState.update { it.copy(carbs = carbs) }
    }

    fun calculateDose(mealType: MealType) {
        viewModelScope.launch {
            val coefficient = getCoefficientForMeal(mealType)
            val carbsPerBu = settingsDataStore.carbsPerBu.first()
            if (carbsPerBu == 0f) return@launch
            val carbs = _uiState.value.carbs.replace(',', '.').toFloatOrNull() ?: 0f
            val breadUnits = carbs / carbsPerBu
            _uiState.update { it.copy(
                insulinDose = breadUnits * coefficient,
                calculationId = it.calculationId + 1,
                lastCalculationSource = 1
            ) }
        }
    }

    fun setInsulinDoseForCarbs(dose: String) {
        _uiState.update { it.copy(insulinDoseForCarbs = dose) }
    }

    fun calculateCarbs(mealType: MealType) {
        viewModelScope.launch {
            val coefficient = getCoefficientForMeal(mealType)
            if (coefficient == 0f) return@launch
            val carbsPerBu = settingsDataStore.carbsPerBu.first()
            val insulinDose = _uiState.value.insulinDoseForCarbs.replace(',', '.').toFloatOrNull() ?: 0f
            val breadUnits = insulinDose / coefficient
            _uiState.update { it.copy(
                calculatedCarbs = breadUnits * carbsPerBu,
                calculationId = it.calculationId + 1,
                lastCalculationSource = 2
            ) }
        }
    }

    fun setInsulinDoseForRemainingCarbs(dose: String) {
        _uiState.update { it.copy(insulinDoseForRemainingCarbs = dose) }
    }

    fun setPlannedCarbs(carbs: String) {
        _uiState.update { it.copy(plannedCarbs = carbs) }
    }

    fun calculateRemainingCarbs(mealType: MealType) {
        viewModelScope.launch {
            val coefficient = getCoefficientForMeal(mealType)
            if (coefficient == 0f) return@launch
            val carbsPerBu = settingsDataStore.carbsPerBu.first()
            val insulinDose = _uiState.value.insulinDoseForRemainingCarbs.replace(',', '.').toFloatOrNull() ?: 0f
            val plannedCarbs = _uiState.value.plannedCarbs.replace(',', '.').toFloatOrNull() ?: 0f
            val totalBreadUnits = insulinDose / coefficient
            val totalCarbs = totalBreadUnits * carbsPerBu
            _uiState.update { it.copy(
                remainingCarbs = totalCarbs - plannedCarbs,
                totalCarbsForDose = totalCarbs,
                calculationId = it.calculationId + 1,
                lastCalculationSource = 3
            ) }
        }
    }

    private suspend fun getCoefficientForMeal(mealType: MealType): Float {
        return when (mealType) {
            MealType.BREAKFAST -> settingsDataStore.breakfastCoefficient.first()
            MealType.DINNER -> settingsDataStore.dinnerCoefficient.first()
            MealType.SUPPER -> settingsDataStore.supperCoefficient.first()
        }
    }
}
