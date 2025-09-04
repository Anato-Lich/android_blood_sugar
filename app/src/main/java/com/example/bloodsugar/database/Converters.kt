package com.example.bloodsugar.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String {
        return value.name
    }

    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return ActivityType.valueOf(value)
    }

    @TypeConverter
    fun fromActivityIntensity(value: ActivityIntensity): String {
        return value.name
    }

    @TypeConverter
    fun toActivityIntensity(value: String): ActivityIntensity {
        return ActivityIntensity.valueOf(value)
    }

    @TypeConverter
    fun fromEventType(value: EventType): String {
        return value.name
    }

    @TypeConverter
    fun toEventType(value: String): EventType {
        return EventType.valueOf(value)
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String {
        return value.name
    }

    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return NotificationType.valueOf(value.uppercase())
    }
}
