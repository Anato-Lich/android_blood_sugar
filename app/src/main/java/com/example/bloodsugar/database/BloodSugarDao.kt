package com.example.bloodsugar.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodSugarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BloodSugarRecord)

    @Update
    suspend fun update(record: BloodSugarRecord)

    @Delete
    suspend fun delete(record: BloodSugarRecord)

    @Query("SELECT * FROM blood_sugar_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BloodSugarRecord>>

    @Query("SELECT * FROM blood_sugar_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getRecordsInRange(startTime: Long, endTime: Long): Flow<List<BloodSugarRecord>>

    @Query("SELECT AVG(value) FROM blood_sugar_records WHERE timestamp >= :since")
    fun getAverageBloodSugar(since: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000): Flow<Float?>

    @Query("SELECT * FROM blood_sugar_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsList(): List<BloodSugarRecord>
}
