package com.example.bloodsugar.features.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.database.AppDatabase
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.domain.TirCalculationUseCase
import com.example.bloodsugar.domain.TirThresholds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BloodSugarRepository
    private val tirCalculationUseCase = TirCalculationUseCase()

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)
        updateAnalysisPeriod(7)
    }

    fun updateAnalysisPeriod(days: Int) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(days.toLong())
            val records = repository.getRecordsInRange(startTime, endTime).first()
            val dailyInsulinFromDb = repository.getDailyInsulinDoses(startTime, endTime).first()

            val tirResult = tirCalculationUseCase(records, TirThresholds.Default)

            // Create a complete list of daily insulin doses for the period
            val completeDailyInsulin = mutableListOf<com.example.bloodsugar.database.DailyInsulinDose>()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startTime

            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            while (calendar.timeInMillis <= endTime) {
                val dayString = formatter.format(calendar.time)
                val doseForDay = dailyInsulinFromDb.find { it.day == dayString }?.total ?: 0f
                completeDailyInsulin.add(com.example.bloodsugar.database.DailyInsulinDose(day = dayString, total = doseForDay))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            _uiState.value = AnalysisUiState(
                timeInRange = tirResult.timeInRange,
                timeAboveRange = tirResult.totalAboveRange,
                timeBelowRange = tirResult.totalBelowRange,
                veryLow = tirResult.veryLow,
                low = tirResult.low,
                high = tirResult.high,
                veryHigh = tirResult.veryHigh,
                selectedPeriod = days,
                dailyInsulin = completeDailyInsulin
            )
        }
    }
}
