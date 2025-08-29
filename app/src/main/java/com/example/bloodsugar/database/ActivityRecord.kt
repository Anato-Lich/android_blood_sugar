package com.example.bloodsugar.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // e.g., "Walking", "Running", "Cycling"
    val durationMinutes: Int,
    val intensity: String // e.g., "Low", "Medium", "High"
)