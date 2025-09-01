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

    @Query("SELECT SUM(value) as total, strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') as day FROM events WHERE type = 'INSULIN' AND timestamp BETWEEN :startTime AND :endTime GROUP BY day ORDER BY day ASC")
    fun getDailyInsulinDoses(startTime: Long, endTime: Long): Flow<List<DailyInsulinDose>>

    @Query("SELECT SUM(value) FROM events WHERE type = 'CARBS' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getCarbsSumForDay(startOfDay: Long, endOfDay: Long): Flow<Float?>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<EventRecord>
}