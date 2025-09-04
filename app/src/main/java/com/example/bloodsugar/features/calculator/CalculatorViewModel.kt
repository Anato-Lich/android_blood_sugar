package com.example.bloodsugar.features.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.FoodItem
import com.example.bloodsugar.domain.InsulinCarbCalculatorUseCase
import com.example.bloodsugar.domain.MealComponent
import com.example.bloodsugar.domain.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val remainingCarbsComponents: List<MealComponent> = emptyList(),

    // Common state
    val foodItems: List<FoodItem> = emptyList(),
    val selectedFood: FoodItem? = null,
    val foodServingValue: String = "",
    val dailyCarbsGoal: Float = 200f
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val repository: BloodSugarRepository
    private val calculatorUseCase = InsulinCarbCalculatorUseCase(settingsDataStore)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)

        viewModelScope.launch {
            repository.getAllFoodItems().collect { items ->
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
            val dose = calculatorUseCase.calculateDose(totalCarbs, mealType)
            _uiState.update { it.copy(insulinDose = dose) }
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

    fun addRemainingCarbsComponent(useGrams: Boolean) {
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
            remainingCarbsComponents = it.remainingCarbsComponents + component,
            selectedFood = null,
            foodServingValue = "",
            foodCarbs = ""
        ) }
    }

    fun addManualRemainingCarbsComponent() {
        val carbs = _uiState.value.manualCarbsEntry.replace(',', '.').toFloatOrNull() ?: return
        val component = MealComponent(
            name = "Manual Entry",
            carbs = carbs,
            servingValue = "%.1f".format(carbs),
            useGrams = true,
            foodItemId = null
        )
        _uiState.update { it.copy(
            remainingCarbsComponents = it.remainingCarbsComponents + component,
            manualCarbsEntry = ""
        ) }
    }

    fun removeRemainingCarbsComponent(id: String) {
        _uiState.update { it.copy(remainingCarbsComponents = it.remainingCarbsComponents.filterNot { c -> c.id == id }) }
    }

    fun updateRemainingCarbsComponent(id: String, newServingValue: String, useGrams: Boolean) {
        val componentToUpdate = _uiState.value.remainingCarbsComponents.find { it.id == id } ?: return
        val foodItem = componentToUpdate.foodItemId?.let { foodId ->
            _uiState.value.foodItems.find { it.id == foodId }
        }

        val serving = newServingValue.replace(',', '.').toFloatOrNull()
        if (serving == null) return

        val newCarbs = if (foodItem != null) {
            if (useGrams) (serving / 100f) * foodItem.carbsPer100g else serving * foodItem.carbsPerServing
        } else {
            serving
        }

        val updatedComponents = _uiState.value.remainingCarbsComponents.map {
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
        _uiState.update { it.copy(remainingCarbsComponents = updatedComponents) }
    }

    fun setInsulinDoseForCarbs(dose: String) {
        _uiState.update { it.copy(insulinDoseForCarbs = dose) }
    }

    fun calculateCarbs(mealType: MealType) {
        viewModelScope.launch {
            val insulinDose = _uiState.value.insulinDoseForCarbs.replace(',', '.').toFloatOrNull() ?: 0f
            val carbs = calculatorUseCase.calculateCarbs(insulinDose, mealType)
            _uiState.update { it.copy(calculatedCarbs = carbs) }
        }
    }

    fun setInsulinDoseForRemainingCarbs(dose: String) {
        _uiState.update { it.copy(insulinDoseForRemainingCarbs = dose) }
    }

    fun calculateRemainingCarbs(mealType: MealType) {
        viewModelScope.launch {
            val insulinDose = _uiState.value.insulinDoseForRemainingCarbs.replace(',', '.').toFloatOrNull() ?: 0f
            val plannedCarbs = _uiState.value.remainingCarbsComponents.sumOf { it.carbs.toDouble() }.toFloat()
            val remaining = calculatorUseCase.calculateRemainingCarbs(insulinDose, plannedCarbs, mealType)
            val totalCarbs = plannedCarbs + remaining
            _uiState.update { it.copy(
                remainingCarbs = remaining,
                totalCarbsForDose = totalCarbs
            ) }
        }
    }
}