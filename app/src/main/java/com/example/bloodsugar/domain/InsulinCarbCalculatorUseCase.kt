package com.example.bloodsugar.domain

import com.example.bloodsugar.data.SettingsDataStore
import kotlinx.coroutines.flow.first



class InsulinCarbCalculatorUseCase(private val settingsDataStore: SettingsDataStore) {

    suspend fun calculateDose(totalCarbs: Float, mealType: MealType): Float {
        if (totalCarbs == 0f) return 0f

        val coefficient = getCoefficientForMeal(mealType)
        val carbsPerBu = settingsDataStore.carbsPerBu.first()
        if (carbsPerBu == 0f) return 0f

        val breadUnits = totalCarbs / carbsPerBu
        val insulin = breadUnits * coefficient

        val accuracy = settingsDataStore.insulinDoseAccuracy.first()
        return if (accuracy > 0f) {
            (kotlin.math.ceil(insulin / accuracy) * accuracy)
        } else {
            insulin
        }
    }

    suspend fun calculateCarbs(insulinDose: Float, mealType: MealType): Float {
        val coefficient = getCoefficientForMeal(mealType)
        if (coefficient == 0f) return 0f
        val carbsPerBu = settingsDataStore.carbsPerBu.first()
        val breadUnits = insulinDose / coefficient
        return breadUnits * carbsPerBu
    }

    suspend fun calculateRemainingCarbs(insulinDose: Float, plannedCarbs: Float, mealType: MealType): Float {
        val coefficient = getCoefficientForMeal(mealType)
        if (coefficient == 0f) return 0f
        val carbsPerBu = settingsDataStore.carbsPerBu.first()
        val totalBreadUnits = insulinDose / coefficient
        val totalCarbs = totalBreadUnits * carbsPerBu
        return totalCarbs - plannedCarbs
    }

    private suspend fun getCoefficientForMeal(mealType: MealType): Float {
        return when (mealType) {
            MealType.BREAKFAST -> settingsDataStore.breakfastCoefficient.first()
            MealType.DINNER -> settingsDataStore.dinnerCoefficient.first()
            MealType.SUPPER -> settingsDataStore.supperCoefficient.first()
        }
    }
}
