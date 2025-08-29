package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class FilterType {
    TODAY,
    THREE_DAYS,
    SEVEN_DAYS,
    CUSTOM
}

enum class DialogType {
    SUGAR,
    EVENT,
    ACTIVITY
}

data class HomeUiState(
    val sugarValue: String = "",
    val comment: String = "",
    val insulinValue: String = "",
    val carbsValue: String = "",
    val records: List<BloodSugarRecord> = emptyList(),
    val events: List<EventRecord> = emptyList(),
    val activities: List<ActivityRecord> = emptyList(),
    val historyItems: List<Any> = emptyList(),
    val chartData: ChartData? = null,
    val selectedFilter: FilterType = FilterType.TODAY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val shownDialog: DialogType? = null,
    val selectedRecord: BloodSugarRecord? = null,
    val activityType: String = "Walking",
    val activityDuration: String = "",
    val activityIntensity: String = "Medium",
    val estimatedA1c: String = "N/A",
    val todaysCarbs: Float = 0f,
    val dailyCarbsGoal: Float = 200f,
    val foodItems: List<FoodItem> = emptyList(),
    val selectedFood: FoodItem? = null,
    val foodServingValue: String = "", // Will now hold the formatted details of a pending meal
    val useGramsInDialog: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val bloodSugarDao = db.bloodSugarDao()
    private val eventDao = db.eventDao()
    private val activityDao = db.activityDao()
    private val foodDao = db.foodDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.map { it.selectedFilter to (it.customStartDate to it.customEndDate) }.distinctUntilChanged().flatMapLatest { (filter, dates) ->
            val (start, end) = when (filter) {
                FilterType.TODAY -> getTodayRange()
                FilterType.THREE_DAYS -> getDaysRange(3)
                FilterType.SEVEN_DAYS -> getDaysRange(7)
                FilterType.CUSTOM -> dates.first to dates.second
            }
            if (start != null && end != null) {
                combine(bloodSugarDao.getRecordsInRange(start, end), eventDao.getEventsInRange(start, end), activityDao.getActivitiesInRange(start, end)) { records, events, activities ->
                    Triple(records, events, activities)
                }
            } else {
                combine(
                    bloodSugarDao.getAllRecords(),
                    eventDao.getEventsInRange(0, Long.MAX_VALUE),
                    activityDao.getActivitiesInRange(0, Long.MAX_VALUE)
                ) { records, events, activities ->
                    Triple(records, events, activities)
                }
            }
        }.onEach { (records, events, activities) ->
            processRecordsForUi(records, events, activities)
        }.launchIn(viewModelScope)

        bloodSugarDao.getAverageBloodSugar(since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90))
            .onEach { avgSugar ->
                if (avgSugar != null && avgSugar > 0) {
                    val eAgMgDl = avgSugar * 18.0182f
                    val a1c = (eAgMgDl + 46.7f) / 28.7f
                    _uiState.update { it.copy(estimatedA1c = "%.1f%%".format(a1c)) }
                } else {
                    _uiState.update { it.copy(estimatedA1c = "N/A") }
                }
            }.launchIn(viewModelScope)

        val (start, end) = getTodayRange()
        combine(settingsDataStore.dailyCarbsGoal, eventDao.getCarbsSumForDay(start, end)) { goal, carbs ->
            _uiState.update {
                it.copy(
                    dailyCarbsGoal = goal,
                    todaysCarbs = carbs ?: 0f
                )
            }
        }.launchIn(viewModelScope)

        foodDao.getAllFoodItems().onEach { foodList ->
            _uiState.update { it.copy(foodItems = foodList) }
        }.launchIn(viewModelScope)
    }

    private fun processRecordsForUi(records: List<BloodSugarRecord>, events: List<EventRecord>, activities: List<ActivityRecord>) {
        viewModelScope.launch(Dispatchers.Default) { // Calculations on background thread
            if (records.isEmpty() && events.isEmpty() && activities.isEmpty()) {
                _uiState.update { it.copy(records = emptyList(), events = emptyList(), activities = emptyList(), chartData = null, historyItems = emptyList()) }
                return@launch
            }

            val (filterStart, filterEnd) = when (_uiState.value.selectedFilter) {
                FilterType.TODAY -> getTodayRange()
                FilterType.THREE_DAYS -> getDaysRange(3)
                FilterType.SEVEN_DAYS -> getDaysRange(7)
                FilterType.CUSTOM -> _uiState.value.customStartDate to _uiState.value.customEndDate
            }

            if (filterStart == null) {
                _uiState.update { it.copy(records = records, events = events, activities = activities, chartData = null, historyItems = emptyList()) }
                return@launch
            }

            val lastRecordTime = records.maxOfOrNull { it.timestamp } ?: 0L
            val lastEventTime = events.maxOfOrNull { it.timestamp } ?: 0L
            val lastActivityTime = activities.maxOfOrNull { it.timestamp } ?: 0L
            val lastDataPointTime = max(lastRecordTime, max(lastEventTime, lastActivityTime))

            val rangeEnd = if (lastDataPointTime > 0) {
                lastDataPointTime + TimeUnit.HOURS.toMillis(1)
            } else {
                filterEnd ?: System.currentTimeMillis()
            }

            val avg = records.map { it.value }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
            val min = records.minOfOrNull { it.value } ?: 0f
            val max = records.maxOfOrNull { it.value } ?: 0f

            val chartData = ChartData(
                records = records.sortedBy { it.timestamp },
                events = events.sortedBy { it.timestamp },
                activities = activities.sortedBy { it.timestamp },
                avg = avg,
                min = min,
                max = max,
                rangeStart = filterStart,
                rangeEnd = rangeEnd
            )

            val combinedList = (records + events + activities).sortedByDescending {
                when (it) {
                    is BloodSugarRecord -> it.timestamp
                    is EventRecord -> it.timestamp
                    is ActivityRecord -> it.timestamp
                    else -> 0
                }
            }

            _uiState.update { it.copy(records = records, events = events, activities = activities, chartData = chartData, historyItems = combinedList) }
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _uiState.update { it.copy(customStartDate = start, customEndDate = end, selectedFilter = FilterType.CUSTOM) }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return start to end
    }

    private fun getDaysRange(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days + 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        val endCalendar = Calendar.getInstance()
        endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        endCalendar.set(Calendar.HOUR_OF_DAY, 0)
        endCalendar.set(Calendar.MINUTE, 0)
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)
        val end = endCalendar.timeInMillis

        return start to end
    }

    fun setSugarValue(value: String) {
        _uiState.update { it.copy(sugarValue = value) }
    }

    fun setComment(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    fun setInsulinValue(value: String) {
        _uiState.update { it.copy(insulinValue = value) }
    }

    fun setCarbsValue(value: String) {
        _uiState.update { it.copy(carbsValue = value, selectedFood = null, foodServingValue = "") }
    }

    fun onFoodSelected(foodItem: FoodItem?) {
        val servingValue = if (foodItem != null) "1" else ""
        _uiState.update { it.copy(
            selectedFood = foodItem,
            foodServingValue = servingValue,
            carbsValue = foodItem?.carbsPerServing?.toString() ?: "",
            useGramsInDialog = false
        ) }
    }

    fun calculateCarbsForLog(servingValueStr: String, useGrams: Boolean) {
        _uiState.update { it.copy(foodServingValue = servingValueStr, useGramsInDialog = useGrams) }
        val servingValue = servingValueStr.replace(',', '.').toFloatOrNull()
        val food = _uiState.value.selectedFood
        if (servingValue != null && food != null) {
            val calculatedCarbs = if (useGrams) {
                (servingValue / 100f) * food.carbsPer100g
            } else { // use number of servings
                servingValue * food.carbsPerServing
            }
            _uiState.update { it.copy(carbsValue = "%.1f".format(calculatedCarbs)) }
        } else {
            _uiState.update { it.copy(carbsValue = "") }
        }
    }

    fun saveInsulinCarbEvent() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val insulin = _uiState.value.insulinValue.replace(',', '.').toFloatOrNull()
            val carbs = _uiState.value.carbsValue.replace(',', '.').toFloatOrNull()

            if (insulin != null) {
                eventDao.insert(EventRecord(timestamp = timestamp, type = "INSULIN", value = insulin))
            }
            if (carbs != null) {
                val details = _uiState.value.foodServingValue // This now holds the formatted details of a pending meal
                val foodName = if (details.contains("\n")) "Multiple Items" else _uiState.value.selectedFood?.name

                eventDao.insert(EventRecord(
                    timestamp = timestamp,
                    type = "CARBS",
                    value = carbs,
                    foodName = foodName,
                    foodServing = details
                ))
            }
            _uiState.update { it.copy(insulinValue = "", carbsValue = "", selectedFood = null, foodServingValue = "") }
        }
    }
    
    fun logMealFromCalculator(carbs: Float, insulin: Float?, details: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            if (insulin != null && insulin > 0) {
                eventDao.insert(EventRecord(timestamp = timestamp, type = "INSULIN", value = insulin))
            }
            if (carbs > 0) {
                eventDao.insert(EventRecord(
                    timestamp = timestamp,
                    type = "CARBS",
                    value = carbs,
                    foodName = "Multiple Items",
                    foodServing = details
                ))
            }
        }
    }

    fun saveRecord() {
        viewModelScope.launch {
            val value = _uiState.value.sugarValue.replace(',', '.').toFloatOrNull() ?: return@launch
            val comment = _uiState.value.comment
            val record = BloodSugarRecord(
                timestamp = System.currentTimeMillis(),
                value = value,
                comment = comment
            )
            bloodSugarDao.insert(record)
            _uiState.update { it.copy(sugarValue = "", comment = "") }
        }
    }

    fun deleteRecord(record: BloodSugarRecord) {
        viewModelScope.launch {
            bloodSugarDao.delete(record)
        }
    }

    fun deleteEvent(event: EventRecord) {
        viewModelScope.launch {
            eventDao.delete(event)
        }
    }

    fun onLogSugarClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.SUGAR, selectedFood = null) }
    }

    fun onLogEventClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.EVENT, selectedFood = null) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(shownDialog = null, selectedFood = null) }
    }

    fun onChartRecordSelected(record: BloodSugarRecord) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun onChartSelectionDismissed() {
        _uiState.update { it.copy(selectedRecord = null) }
    }

    fun onLogActivityClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.ACTIVITY) }
    }

    fun setActivityType(type: String) {
        _uiState.update { it.copy(activityType = type) }
    }

    fun setActivityDuration(duration: String) {
        _uiState.update { it.copy(activityDuration = duration) }
    }

    fun setActivityIntensity(intensity: String) {
        _uiState.update { it.copy(activityIntensity = intensity) }
    }

    fun saveActivity() {
        viewModelScope.launch {
            val duration = _uiState.value.activityDuration.toIntOrNull() ?: return@launch
            val activity = ActivityRecord(
                timestamp = System.currentTimeMillis(),
                type = _uiState.value.activityType,
                durationMinutes = duration,
                intensity = _uiState.value.activityIntensity
            )
            activityDao.insert(activity)
            _uiState.update { it.copy(activityDuration = "") }
        }
    }

    fun deleteActivity(activity: ActivityRecord) {
        viewModelScope.launch {
            activityDao.delete(activity)
        }
    }
}
