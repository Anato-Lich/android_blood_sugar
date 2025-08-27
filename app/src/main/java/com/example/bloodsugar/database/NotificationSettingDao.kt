package com.example.bloodsugar.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notificationSetting: NotificationSetting): Long

    @Update
    suspend fun update(notificationSetting: NotificationSetting)

    @Delete
    suspend fun delete(notificationSetting: NotificationSetting)

    @Query("SELECT * FROM notification_settings ORDER BY id DESC")
    fun getAll(): Flow<List<NotificationSetting>>

    @Query("SELECT * FROM notification_settings WHERE isEnabled = 1")
    fun getEnabledNotifications(): Flow<List<NotificationSetting>>
}
