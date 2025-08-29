package com.example.bloodsugar.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // e.g., "INSULIN" or "CARBS"
    val value: Float,
    val foodName: String? = null,
    val foodServing: String? = null
)