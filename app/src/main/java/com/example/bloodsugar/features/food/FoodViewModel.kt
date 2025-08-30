package com.example.bloodsugar.features.food

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.FoodItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BloodSugarRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)
    }

    val foodItems = repository.getAllFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addOrUpdateFoodItem(
        id: Long = 0,
        name: String,
        servingSizeGrams: String,
        carbsPerServing: String
    ) {
        val servingSize = servingSizeGrams.replace(',', '.').toFloatOrNull()
        val carbs = carbsPerServing.replace(',', '.').toFloatOrNull()

        if (name.isBlank() || servingSize == null || carbs == null || servingSize == 0f) {
            // TODO: Show error to the user
            return
        }

        val carbsPer100g = (carbs / servingSize) * 100

        val foodItem = FoodItem(
            id = id,
            name = name,
            servingSizeGrams = servingSize,
            carbsPerServing = carbs,
            carbsPer100g = carbsPer100g
        )

        viewModelScope.launch {
            if (id == 0L) {
                repository.insertFoodItem(foodItem)
            } else {
                repository.updateFoodItem(foodItem)
            }
        }
    }

    fun deleteFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            repository.deleteFoodItem(foodItem)
        }
    }
}