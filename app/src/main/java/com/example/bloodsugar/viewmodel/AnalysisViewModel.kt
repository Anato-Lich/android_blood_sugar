package com.example.bloodsugar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.bloodsugar.database.AppDatabase
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AnalysisUiState(
    val timeInRange: Float = 0f,
    val timeAboveRange: Float = 0f,
    val timeBelowRange: Float = 0f,
    val veryLow: Float = 0f,
    val low: Float = 0f,
    val high: Float = 0f,
    val veryHigh: Float = 0f,
    val selectedPeriod: Int = 7 // Default to 7 days
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val bloodSugarDao = AppDatabase.getDatabase(application).bloodSugarDao()

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        calculateTir(7)
    }

    fun calculateTir(days: Int) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(days.toLong())
            val records = bloodSugarDao.getRecordsInRange(startTime, endTime).first()

            if (records.size < 2) {
                _uiState.value = AnalysisUiState(selectedPeriod = days)
                return@launch
            }

            var durationVeryLow = 0L
            var durationLow = 0L
            var durationInRange = 0L
            var durationHigh = 0L
            var durationVeryHigh = 0L

            for (i in 0 until records.size - 1) {
                val currentRecord = records[i]
                val nextRecord = records[i+1]
                val duration = currentRecord.timestamp - nextRecord.timestamp

                // Average value for the segment
                val avgValue = (currentRecord.value + nextRecord.value) / 2f

                when {
                    avgValue < 3.0f -> durationVeryLow += duration
                    avgValue < 4.0f -> durationLow += duration
                    avgValue <= 10.0f -> durationInRange += duration
                    avgValue <= 13.9f -> durationHigh += duration
                    else -> durationVeryHigh += duration
                }
            }

            val totalDuration = records.first().timestamp - records.last().timestamp
            if (totalDuration > 0) {
                val totalDurationFloat = totalDuration.toFloat()
                _uiState.value = AnalysisUiState(
                    timeInRange = (durationInRange / totalDurationFloat) * 100,
                    timeAboveRange = ((durationHigh + durationVeryHigh) / totalDurationFloat) * 100,
                    timeBelowRange = ((durationLow + durationVeryLow) / totalDurationFloat) * 100,
                    veryLow = (durationVeryLow / totalDurationFloat) * 100,
                    low = (durationLow / totalDurationFloat) * 100,
                    high = (durationHigh / totalDurationFloat) * 100,
                    veryHigh = (durationVeryHigh / totalDurationFloat) * 100,
                    selectedPeriod = days
                )
            }
        }
    }
}
