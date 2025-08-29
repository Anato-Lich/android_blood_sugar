package com.example.bloodsugar.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val servingSizeGrams: Float,
    val carbsPerServing: Float,
    val carbsPer100g: Float
)