package com.example.bloodsugar.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String = "daily", // "daily" or "interval"
    val time: String = "08:00", // For daily notifications, e.g., "08:00"
    val intervalMinutes: Int = 60, // For interval notifications
    val message: String = "",
    val isEnabled: Boolean = true,
    val startTime: String? = null, // e.g., "08:00"
    val endTime: String? = null // e.g., "22:00"
)