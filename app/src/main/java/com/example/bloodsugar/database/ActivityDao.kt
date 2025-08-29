package com.example.bloodsugar.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityRecord)

    @Delete
    suspend fun delete(activity: ActivityRecord)

    @Query("SELECT * FROM activities WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getActivitiesInRange(startTime: Long, endTime: Long): Flow<List<ActivityRecord>>
}