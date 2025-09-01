package com.example.bloodsugar.data

import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.DailyInsulinDose
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BloodSugarRepository(db: AppDatabase) {

    private val bloodSugarDao = db.bloodSugarDao()
    private val eventDao = db.eventDao()
    private val activityDao = db.activityDao()
    private val foodDao = db.foodDao()

    fun getRecordsInRange(start: Long, end: Long): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecordsInRange(start, end)
    }

    fun getEventsInRange(start: Long, end: Long): Flow<List<EventRecord>> {
        return eventDao.getEventsInRange(start, end)
    }

    fun getActivitiesInRange(start: Long, end: Long): Flow<List<ActivityRecord>> {
        return activityDao.getActivitiesInRange(start, end)
    }

    fun getDailyInsulinDoses(start: Long, end: Long): Flow<List<DailyInsulinDose>> {
        return eventDao.getDailyInsulinDoses(start, end)
    }

    fun getCombinedDataInRange(start: Long, end: Long): Flow<Triple<List<BloodSugarRecord>, List<EventRecord>, List<ActivityRecord>>> {
        return combine(
            getRecordsInRange(start, end),
            getEventsInRange(start, end),
            getActivitiesInRange(start, end)
        ) { records, events, activities ->
            Triple(records, events, activities)
        }
    }

    fun getAllRecords(): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getAllRecords()
    }

    fun getAverageBloodSugar(since: Long): Flow<Float?> {
        return bloodSugarDao.getAverageBloodSugar(since)
    }

    fun getCarbsSumForDay(start: Long, end: Long): Flow<Float?> {
        return eventDao.getCarbsSumForDay(start, end)
    }

    fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodDao.getAllFoodItems()
    }

    suspend fun insertEvent(event: EventRecord) {
        eventDao.insert(event)
    }

    suspend fun insertRecord(record: BloodSugarRecord) {
        bloodSugarDao.insert(record)
    }

    suspend fun deleteRecord(record: BloodSugarRecord) {
        bloodSugarDao.delete(record)
    }

    suspend fun deleteEvent(event: EventRecord) {
        eventDao.delete(event)
    }

    suspend fun insertActivity(activity: ActivityRecord) {
        activityDao.insert(activity)
    }

    suspend fun deleteActivity(activity: ActivityRecord) {
        activityDao.delete(activity)
    }

    suspend fun getAllRecordsList(): List<BloodSugarRecord> {
        return bloodSugarDao.getAllRecordsList()
    }

    suspend fun getAllEventsList(): List<EventRecord> {
        return eventDao.getAllEventsList()
    }

    suspend fun getAllActivitiesList(): List<ActivityRecord> {
        return activityDao.getAllActivitiesList()
    }

    suspend fun insertFoodItem(foodItem: FoodItem) {
        foodDao.insert(foodItem)
    }

    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodDao.update(foodItem)
    }

    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodDao.delete(foodItem)
    }
}
