package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
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
    EVENT
}

data class HomeUiState(
    val sugarValue: String = "",
    val comment: String = "",
    val insulinValue: String = "",
    val carbsValue: String = "",
    val records: List<BloodSugarRecord> = emptyList(),
    val events: List<EventRecord> = emptyList(),
    val historyItems: List<Any> = emptyList(),
    val chartData: ChartData? = null,
    val selectedFilter: FilterType = FilterType.TODAY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val shownDialog: DialogType? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bloodSugarDao = AppDatabase.getDatabase(application).bloodSugarDao()
    private val eventDao = AppDatabase.getDatabase(application).eventDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun setInsulinValue(value: String) {
        _uiState.update { it.copy(insulinValue = value) }
    }

    fun setCarbsValue(value: String) {
        _uiState.update { it.copy(carbsValue = value) }
    }

    init {
        viewModelScope.launch {
            _uiState.map { it.selectedFilter to (it.customStartDate to it.customEndDate) }.distinctUntilChanged().flatMapLatest { (filter, dates) ->
                val (start, end) = when (filter) {
                    FilterType.TODAY -> getTodayRange()
                    FilterType.THREE_DAYS -> getDaysRange(3)
                    FilterType.SEVEN_DAYS -> getDaysRange(7)
                    FilterType.CUSTOM -> dates.first to dates.second
                }
                if (start != null && end != null) {
                    combine(bloodSugarDao.getRecordsInRange(start, end), eventDao.getEventsInRange(start, end)) { records, events ->
                        records to events
                    }
                } else {
                    combine(bloodSugarDao.getAllRecords(), flowOf(emptyList<EventRecord>())) { records, events ->
                        records to events
                    }
                }
            }.onEach { (records, events) ->
                processRecordsForUi(records, events)
            }.launchIn(viewModelScope)
        }
    }

    private fun processRecordsForUi(records: List<BloodSugarRecord>, events: List<EventRecord>) {
        viewModelScope.launch(Dispatchers.Default) { // Calculations on background thread
            if (records.isEmpty() && events.isEmpty()) {
                _uiState.update { it.copy(records = records, events = events, chartData = null, historyItems = emptyList()) }
                return@launch
            }

            val (filterStart, filterEnd) = when (_uiState.value.selectedFilter) {
                FilterType.TODAY -> getTodayRange()
                FilterType.THREE_DAYS -> getDaysRange(3)
                FilterType.SEVEN_DAYS -> getDaysRange(7)
                FilterType.CUSTOM -> _uiState.value.customStartDate to _uiState.value.customEndDate
            }

            if (filterStart == null) {
                _uiState.update { it.copy(records = records, events = events, chartData = null, historyItems = emptyList()) }
                return@launch
            }

            val lastRecordTime = records.maxOfOrNull { it.timestamp } ?: 0L
            val lastEventTime = events.maxOfOrNull { it.timestamp } ?: 0L
            val lastDataPointTime = max(lastRecordTime, lastEventTime)

            val rangeEnd = if (lastDataPointTime > 0) {
                lastDataPointTime + TimeUnit.HOURS.toMillis(1)
            } else {
                filterEnd ?: System.currentTimeMillis()
            }

            val avg = records.map { it.value }.average().toFloat()
            val min = records.minOfOrNull { it.value } ?: 0f
            val max = records.maxOfOrNull { it.value } ?: 0f

            val chartData = ChartData(
                records = records.sortedBy { it.timestamp },
                events = events.sortedBy { it.timestamp },
                avg = avg,
                min = min,
                max = max,
                rangeStart = filterStart,
                rangeEnd = rangeEnd
            )

            val combinedList = (records + events).sortedByDescending {
                when (it) {
                    is BloodSugarRecord -> it.timestamp
                    is EventRecord -> it.timestamp
                    else -> 0
                }
            }

            _uiState.update { it.copy(records = records, events = events, chartData = chartData, historyItems = combinedList) }
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
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return start to end
    }

    private fun getDaysRange(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val start = calendar.timeInMillis
        return start to System.currentTimeMillis()
    }

    fun setSugarValue(value: String) {
        _uiState.update { it.copy(sugarValue = value) }
    }

    fun setComment(comment: String) {
        _uiState.update { it.copy(comment = comment) }
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
                eventDao.insert(EventRecord(timestamp = timestamp, type = "CARBS", value = carbs))
            }

            _uiState.update { it.copy(insulinValue = "", carbsValue = "") }
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
        _uiState.update { it.copy(shownDialog = DialogType.SUGAR) }
    }

    fun onLogEventClicked() {
        _uiState.update { it.copy(shownDialog = DialogType.EVENT) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(shownDialog = null) }
    }

    fun onChartRecordSelected(record: BloodSugarRecord) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun onChartSelectionDismissed() {
        _uiState.update { it.copy(selectedRecord = null) }
    }
}