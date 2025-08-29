package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.FoodItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MealComponent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingValue: String,
    val useGrams: Boolean,
    val carbs: Float,
    val foodItemId: Long? = null
)

data class CalculatorUiState(
    // Insulin from Carbs Tab
    val components: List<MealComponent> = emptyList(),
    val insulinDose: Float? = null,
    val manualCarbsEntry: String = "",
    val foodCarbs: String = "",
    val showMealReadyMessage: Boolean = false,

    // Carbs from Insulin Tab
    val insulinDoseForCarbs: String = "",
    val calculatedCarbs: Float? = null,

    // Remaining Carbs Tab
    val insulinDoseForRemainingCarbs: String = "",
    val plannedCarbs: String = "",
    val remainingCarbs: Float? = null,
    val totalCarbsForDose: Float? = null,

    // Common state
    val foodItems: List<FoodItem> = emptyList(),
    val selectedFood: FoodItem? = null,
    val foodServingValue: String = "",
    val dailyCarbsGoal: Float = 200f
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val foodDao = AppDatabase.getDatabase(application).foodDao()

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            foodDao.getAllFoodItems().collect { items ->
                _uiState.update { it.copy(foodItems = items) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.dailyCarbsGoal.collect { goal ->
                _uiState.update { it.copy(dailyCarbsGoal = goal) }
            }
        }
    }

    // --- Insulin from Carbs Tab ---

    fun onFoodSelected(foodItem: FoodItem?) {
        _uiState.update { it.copy(
            selectedFood = foodItem,
            foodServingValue = if (foodItem != null) "1" else "",
            foodCarbs = if (foodItem != null) "%.1f".format(foodItem.carbsPerServing) else ""
        ) }
    }

    fun calculateCarbsFromFood(servingValueStr: String, useGrams: Boolean) {
        _uiState.update { it.copy(foodServingValue = servingValueStr) }
        val servingValue = servingValueStr.replace(',', '.').toFloatOrNull()
        val food = _uiState.value.selectedFood
        if (servingValue != null && food != null) {
            val calculatedCarbs = if (useGrams) {
                (servingValue / 100f) * food.carbsPer100g
            } else { // use number of servings
                servingValue * food.carbsPerServing
            }
            _uiState.update { it.copy(foodCarbs = "%.1f".format(calculatedCarbs)) }
        } else {
            _uiState.update { it.copy(foodCarbs = "") }
        }
    }

    fun addFoodComponent(useGrams: Boolean) {
        val food = _uiState.value.selectedFood ?: return
        val carbs = _uiState.value.foodCarbs.replace(',', '.').toFloatOrNull() ?: return
        val servingValue = _uiState.value.foodServingValue

        val component = MealComponent(
            name = food.name,
            carbs = carbs,
            servingValue = servingValue,
            useGrams = useGrams,
            foodItemId = food.id
        )

        _uiState.update { it.copy(
            components = it.components + component,
            selectedFood = null,
            foodServingValue = "",
            foodCarbs = ""
        ) }
    }

    fun setManualCarbsEntry(entry: String) {
        _uiState.update { it.copy(manualCarbsEntry = entry) }
    }

    fun addManualCarbsComponent() {
        val carbs = _uiState.value.manualCarbsEntry.replace(',', '.').toFloatOrNull() ?: return
        val component = MealComponent(
            name = "Manual Entry",
            carbs = carbs,
            servingValue = "%.1f".format(carbs),
            useGrams = true,
            foodItemId = null
        )
        _uiState.update { it.copy(
            components = it.components + component,
            manualCarbsEntry = ""
        ) }
    }

    fun removeComponent(id: String) {
        _uiState.update { it.copy(components = it.components.filterNot { c -> c.id == id }) }
    }

    fun updateComponent(id: String, newServingValue: String, useGrams: Boolean) {
        val componentToUpdate = _uiState.value.components.find { it.id == id } ?: return
        val foodItem = componentToUpdate.foodItemId?.let { foodId ->
            _uiState.value.foodItems.find { it.id == foodId }
        }

        val serving = newServingValue.replace(',', '.').toFloatOrNull()
        if (serving == null) return

        val newCarbs = if (foodItem != null) {
            if (useGrams) (serving / 100f) * foodItem.carbsPer100g else serving * foodItem.carbsPerServing
        } else {
            // It's a manual entry, the serving value is the carb value
            serving
        }

        val updatedComponents = _uiState.value.components.map {
            if (it.id == id) {
                it.copy(
                    carbs = newCarbs,
                    servingValue = newServingValue,
                    useGrams = useGrams
                )
            } else {
                it
            }
        }
        _uiState.update { it.copy(components = updatedComponents) }
    }

    fun calculateDose(mealType: MealType) {
        viewModelScope.launch {
            val totalCarbs = _uiState.value.components.sumOf { it.carbs.toDouble() }.toFloat()
            if (totalCarbs == 0f) {
                _uiState.update { it.copy(insulinDose = 0f) }
                return@launch
            }

            val coefficient = getCoefficientForMeal(mealType)
            val carbsPerBu = settingsDataStore.carbsPerBu.first()
            if (carbsPerBu == 0f) return@launch

            val breadUnits = totalCarbs / carbsPerBu
            val insulin = breadUnits * coefficient

            val accuracy = settingsDataStore.insulinDoseAccuracy.first()
            if (accuracy > 0f) {
                val roundedInsulin = (kotlin.math.ceil(insulin / accuracy) * accuracy).toFloat()
                _uiState.update { it.copy(insulinDose = roundedInsulin) }
            } else {
                _uiState.update { it.copy(insulinDose = insulin) }
            }
        }
    }

    fun clearCalculator() {
        _uiState.update { it.copy(
            components = emptyList(),
            insulinDose = null,
            manualCarbsEntry = "",
            foodCarbs = "",
            selectedFood = null,
            foodServingValue = ""
        ) }
    }

    fun resetFoodInputs() {
        _uiState.update { it.copy(foodServingValue = "", foodCarbs = "") }
    }


    // --- Other Tabs ---

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
            _uiState.update { it.copy(calculatedCarbs = breadUnits * carbsPerBu) }
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
                totalCarbsForDose = totalCarbs
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
