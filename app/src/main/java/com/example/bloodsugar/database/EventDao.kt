package com.example.bloodsugar.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventRecord)

    @Delete
    suspend fun delete(event: EventRecord)

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<EventRecord>>
}