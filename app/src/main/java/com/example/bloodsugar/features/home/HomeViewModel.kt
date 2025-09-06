package com.example.bloodsugar.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.ActivityIntensity
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.ActivityType
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.EventType
import com.example.bloodsugar.domain.ChartData
import com.example.bloodsugar.domain.TirCalculationUseCase
import com.example.bloodsugar.domain.TrendCalculationUseCase
import com.example.bloodsugar.domain.TirThresholds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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



@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BloodSugarRepository
    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = androidx.work.WorkManager.getInstance(application)
    private val tirCalculationUseCase = TirCalculationUseCase()
    private val trendCalculationUseCase = TrendCalculationUseCase()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)

        _uiState.map { it.selectedFilter to (it.customStartDate to it.customEndDate) }.distinctUntilChanged().flatMapLatest { (filter, dates) ->
            val (start, end) = when (filter) {
                FilterType.TODAY -> getTodayRange()
                FilterType.THREE_DAYS -> getDaysRange(3)
                FilterType.SEVEN_DAYS -> getDaysRange(7)
                FilterType.CUSTOM -> dates.first to dates.second
            }
            if (start != null && end != null) {
                repository.getCombinedDataInRange(start, end)
            } else {
                combine(
                    repository.getAllRecords(),
                    repository.getEventsInRange(0, Long.MAX_VALUE),
                    repository.getActivitiesInRange(0, Long.MAX_VALUE)
                ) { records, events, activities ->
                    Triple(records, events, activities)
                }
            }
        }.onEach { (records, events, activities) ->
            processRecordsForUi(records, events, activities)
        }.launchIn(viewModelScope)

        repository.getAverageBloodSugar(since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90))
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
        combine(settingsDataStore.dailyCarbsGoal, repository.getCarbsSumForDay(start, end)) { goal, carbs ->
            _uiState.update {
                it.copy(
                    dailyCarbsGoal = goal,
                    todaysCarbs = carbs ?: 0f
                )
            }
        }.launchIn(viewModelScope)

        repository.getAllFoodItems().onEach { foodList ->
            _uiState.update { it.copy(foodItems = foodList) }
        }.launchIn(viewModelScope)

        // Calculate average daily insulin for the last 7 days
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        repository.getDailyInsulinDoses(sevenDaysAgo, System.currentTimeMillis()).onEach { doses ->
            if (doses.isNotEmpty()) {
                _uiState.update { it.copy(avgDailyInsulin = doses.map { it.total }.average().toFloat()) }
            } else {
                _uiState.update { it.copy(avgDailyInsulin = 0f) }
            }
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

            val trend = trendCalculationUseCase.calculateTrend(records)

            val chartData = ChartData(
                records = records.sortedBy { it.timestamp },
                events = events.sortedBy { it.timestamp },
                activities = activities.sortedBy { it.timestamp },
                avg = avg,
                min = min,
                max = max,
                rangeStart = filterStart,
                rangeEnd = rangeEnd,
                trend = trend
            )

            val combinedList = (records + events + activities).sortedByDescending {
                when (it) {
                    is BloodSugarRecord -> it.timestamp
                    is EventRecord -> it.timestamp
                    is ActivityRecord -> it.timestamp
                    else -> 0
                }
            }

            val recentHistory = combinedList.take(5)
            val tirResult = tirCalculationUseCase(records, TirThresholds.Default)

            _uiState.update { it.copy(
                records = records, 
                events = events, 
                activities = activities, 
                chartData = chartData, 
                historyItems = combinedList, 
                recentHistoryItems = recentHistory,
                timeInRange = tirResult.timeInRange,
                timeAboveRange = tirResult.totalAboveRange,
                timeBelowRange = tirResult.totalBelowRange,
                veryLow = tirResult.veryLow,
                low = tirResult.low,
                high = tirResult.high,
                veryHigh = tirResult.veryHigh
            ) }
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _uiState.update { it.copy(customStartDate = start, customEndDate = end, selectedFilter = FilterType.CUSTOM) }
    }

    fun setCustomStartDate(start: Long) {
        val end = _uiState.value.customEndDate ?: System.currentTimeMillis()
        _uiState.update { it.copy(customStartDate = start, customEndDate = end, selectedFilter = FilterType.CUSTOM) }
    }

    fun setCustomEndDate(end: Long) {
        val start = _uiState.value.customStartDate ?: (end - TimeUnit.DAYS.toMillis(7))
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
        _uiState.update { it.copy(carbsValue = value) }
    }

    

    fun saveInsulinCarbEvent() {
        viewModelScope.launch {
            val timestamp = _uiState.value.newRecordTimestamp ?: System.currentTimeMillis()
            val insulin = _uiState.value.insulinValue.replace(',', '.').toFloatOrNull()
            val carbs = _uiState.value.carbsValue.replace(',', '.').toFloatOrNull()

            if (insulin != null) {
                repository.insertEvent(EventRecord(timestamp = timestamp, type = EventType.INSULIN, value = insulin))
            }
            if (carbs != null) {
                repository.insertEvent(EventRecord(timestamp = timestamp, type = EventType.CARBS, value = carbs))
                schedulePostMealNotification(carbs)
            }
            _uiState.update { it.copy(insulinValue = "", carbsValue = "", newRecordTimestamp = null) }
        }
    }
    
    fun logMealFromCalculator(carbs: Float, insulin: Float?, details: String) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            if (insulin != null && insulin > 0) {
                repository.insertEvent(EventRecord(timestamp = timestamp, type = EventType.INSULIN, value = insulin))
            }
            if (carbs > 0) {
                repository.insertEvent(EventRecord(
                    timestamp = timestamp,
                    type = EventType.CARBS,
                    value = carbs,
                    foodName = "Multiple Items",
                    foodServing = details
                ))
                schedulePostMealNotification(carbs)
            }
            _uiState.update { it.copy(scrollToHistory = true) }
        }
    }

    fun logInsulinAndCarbs(insulin: Float, carbs: Float) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            if (insulin > 0) {
                repository.insertEvent(EventRecord(timestamp = timestamp, type = EventType.INSULIN, value = insulin))
            }
            if (carbs > 0) {
                repository.insertEvent(EventRecord(
                    timestamp = timestamp,
                    type = EventType.CARBS,
                    value = carbs,
                    foodName = "From Calculator",
                    foodServing = "Calculated from %.1f u insulin".format(insulin)
                ))
                schedulePostMealNotification(carbs)
            }
            _uiState.update { it.copy(scrollToHistory = true) }
        }
    }

    fun saveRecord() {
        viewModelScope.launch {
            val value = _uiState.value.sugarValue.replace(',', '.').toFloatOrNull() ?: return@launch
            val comment = _uiState.value.comment
            val record = BloodSugarRecord(
                timestamp = _uiState.value.newRecordTimestamp ?: System.currentTimeMillis(),
                value = value,
                comment = comment
            )
            repository.insertRecord(record)
            _uiState.update { it.copy(sugarValue = "", comment = "", newRecordTimestamp = null) }
            checkTrendAndScheduleNotifications()
        }
    }

    fun deleteRecord(record: BloodSugarRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun deleteEvent(event: EventRecord) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun onLogSugarClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.SUGAR, newRecordTimestamp = System.currentTimeMillis()) }
    }

    fun onLogEventClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.EVENT, newRecordTimestamp = System.currentTimeMillis()) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(shownDialog = null, newRecordTimestamp = null) }
    }

    fun onChartRecordSelected(record: BloodSugarRecord) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun onChartSelectionDismissed() {
        _uiState.update { it.copy(selectedRecord = null) }
    }

    fun onScrollToHistoryHandled() {
        _uiState.update { it.copy(scrollToHistory = false) }
    }

    fun handleIntentAction(action: String?) {
        when (action) {
            "ACTION_OPEN_LOG_SUGAR_DIALOG" -> onLogSugarClicked()
        }
    }

    fun onLogActivityClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.ACTIVITY, newRecordTimestamp = System.currentTimeMillis()) }
    }

    fun setActivityType(type: String) {
        _uiState.update { it.copy(activityType = type) }
    }
    
    fun setNewRecordTimestamp(timestamp: Long) {
        _uiState.update { it.copy(newRecordTimestamp = timestamp) }
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
                timestamp = _uiState.value.newRecordTimestamp ?: System.currentTimeMillis(),
                type = ActivityType.valueOf(_uiState.value.activityType.uppercase()),
                durationMinutes = duration,
                intensity = ActivityIntensity.valueOf(_uiState.value.activityIntensity.uppercase())
            )
            repository.insertActivity(activity)
            _uiState.update { it.copy(activityDuration = "", newRecordTimestamp = null) }
        }
    }

    fun deleteActivity(activity: ActivityRecord) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
        }
    }

    private fun checkTrendAndScheduleNotifications() {
        viewModelScope.launch {
            val trendEnabled = settingsDataStore.trendNotificationEnabled.first()
            if (!trendEnabled) {
                workManager.cancelUniqueWork("trend-notification-low")
                workManager.cancelUniqueWork("trend-notification-high")
                return@launch
            }

            val lowThreshold = settingsDataStore.trendNotificationLowThreshold.first()
            val highThreshold = settingsDataStore.trendNotificationHighThreshold.first()

            val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
            val recentRecords = repository.getRecordsInRange(since, System.currentTimeMillis()).first()

            if (recentRecords.size < 2) { // Changed from 3 to 2 to match use case
                workManager.cancelUniqueWork("trend-notification-low")
                workManager.cancelUniqueWork("trend-notification-high")
                return@launch
            }

            val trend = trendCalculationUseCase.calculateTrend(recentRecords)

            if (trend == null) {
                workManager.cancelUniqueWork("trend-notification-low")
                workManager.cancelUniqueWork("trend-notification-high")
                return@launch
            }

            val slopePerHour = trend.slope
            val intercept = trend.intercept
            val startTime = trend.startTime
            val slopePerMs = slopePerHour / 3_600_000f

            // Check for low threshold
            if (slopePerMs < 0) {
                val crossingTimeMs = ((lowThreshold - intercept) / slopePerMs) + startTime
                if (crossingTimeMs > System.currentTimeMillis()) {
                    scheduleTrendNotifications("low", lowThreshold, crossingTimeMs.toLong())
                } else {
                    workManager.cancelUniqueWork("trend-notification-low")
                }
            } else {
                workManager.cancelUniqueWork("trend-notification-low")
            }

            // Check for high threshold
            if (slopePerMs > 0) {
                val crossingTimeMs = ((highThreshold - intercept) / slopePerMs) + startTime
                if (crossingTimeMs > System.currentTimeMillis()) {
                    scheduleTrendNotifications("high", highThreshold, crossingTimeMs.toLong())
                } else {
                    workManager.cancelUniqueWork("trend-notification-high")
                }
            } else {
                workManager.cancelUniqueWork("trend-notification-high")
            }
        }
    }

    private fun scheduleTrendNotifications(type: String, threshold: Float, crossingTime: Long) {
        val uniqueWorkName = "trend-notification-$type"
        val now = System.currentTimeMillis()
        val fifteenMinutesInMillis = TimeUnit.MINUTES.toMillis(15)

        // Immediate notification
        val message = "Blood sugar is trending $type. It may reach %.1f at %s.".format(threshold, SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(crossingTime)))
        val immediateData = workDataOf(
            "message" to message,
            "type" to "trend-immediate"
        )
        val immediateRequest = OneTimeWorkRequestBuilder<com.example.bloodsugar.notifications.NotificationWorker>()
            .setInputData(immediateData)
            .build()

        // Pre-emptive notification
        val preemptiveTime = crossingTime - fifteenMinutesInMillis
        if (preemptiveTime > now) {
            val delay = preemptiveTime - now
            val preemptiveMessage = "Blood sugar may reach %.1f in 15 minutes.".format(threshold)
            val preemptiveData = workDataOf(
                "message" to preemptiveMessage,
                "type" to "trend-preemptive"
            )
            val preemptiveRequest = OneTimeWorkRequestBuilder<com.example.bloodsugar.notifications.NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(preemptiveData)
                .build()

            workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, listOf(immediateRequest, preemptiveRequest))
        } else {
            workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, immediateRequest)
        }
    }

    private fun schedulePostMealNotification(carbs: Float) {
        viewModelScope.launch {
            if (carbs > 0) {
                val settingsFlow = combine(
                    settingsDataStore.postMealNotificationEnabled,
                    settingsDataStore.postMealNotificationDelay
                ) { enabled, delay ->
                    Pair(enabled, delay)
                }

                val (isEnabled, delay) = settingsFlow.first()

                if (isEnabled) {
                    val inputData = workDataOf(
                        "message" to "Time to check your blood sugar after your meal.",
                        "type" to "post-meal"
                    )

                    val postMealWorkRequest = OneTimeWorkRequestBuilder<com.example.bloodsugar.notifications.NotificationWorker>()
                        .setInitialDelay(delay.toLong(), TimeUnit.MINUTES)
                        .setInputData(inputData)
                        .addTag("post-meal-notification")
                        .build()

                    workManager.enqueueUniqueWork(
                        "post-meal-${System.currentTimeMillis()}",
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        postMealWorkRequest
                    )
                }
            }
        }
    }
}
