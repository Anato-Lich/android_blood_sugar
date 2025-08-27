package com.example.bloodsugar.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_sugar_records")
data class BloodSugarRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val value: Float,
    val comment: String
)
