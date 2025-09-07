package com.example.bloodsugar.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.domain.ExportDataUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val breakfastCoefficient: String = "",
    val dinnerCoefficient: String = "",
    val supperCoefficient: String = "",
    val carbsPerBu: String = "",
    val dailyCarbsGoal: String = "",
    val insulinDoseAccuracy: String = "0.5",
    val postMealNotificationEnabled: Boolean = false,
    val postMealNotificationDelay: String = "120",
    val trendNotificationEnabled: Boolean = false,
    val trendNotificationLowThreshold: String = "4.0",
    val trendNotificationHighThreshold: String = "10.0",
    val trendNotificationTimeWindow: String = "60",
    val hasUnsavedChanges: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = androidx.work.WorkManager.getInstance(application)
    private val repository: BloodSugarRepository
    private val exportDataUseCase: ExportDataUseCase

    private val _exportChannel = Channel<String>()
    val exportChannel = _exportChannel.receiveAsFlow()

    private val _pdfExportChannel = Channel<Uri>()
    val pdfExportChannel = _pdfExportChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)
        exportDataUseCase = ExportDataUseCase(repository, application.applicationContext)

        viewModelScope.launch {
            combine(
                settingsDataStore.breakfastCoefficient,
                settingsDataStore.dinnerCoefficient,
                settingsDataStore.supperCoefficient,
                settingsDataStore.carbsPerBu,
                settingsDataStore.dailyCarbsGoal,
                settingsDataStore.insulinDoseAccuracy,
                settingsDataStore.postMealNotificationEnabled,
                settingsDataStore.postMealNotificationDelay,
                settingsDataStore.trendNotificationEnabled,
                settingsDataStore.trendNotificationLowThreshold,
                settingsDataStore.trendNotificationHighThreshold,
                settingsDataStore.trendNotificationTimeWindow
            ) { values ->
                val breakfast = values[0] as Float
                val dinner = values[1] as Float
                val supper = values[2] as Float
                val carbsPerBu = values[3] as Float
                val dailyCarbsGoal = values[4] as Float
                val insulinDoseAccuracy = values[5] as Float
                val postMealEnabled = values[6] as Boolean
                val postMealDelay = values[7] as Int
                val trendEnabled = values[8] as Boolean
                val trendLow = values[9] as Float
                val trendHigh = values[10] as Float
                val trendTimeWindow = values[11] as Int

                _uiState.update {
                    it.copy(
                        breakfastCoefficient = breakfast.toString(),
                        dinnerCoefficient = dinner.toString(),
                        supperCoefficient = supper.toString(),
                        carbsPerBu = carbsPerBu.toString(),
                        dailyCarbsGoal = dailyCarbsGoal.toString(),
                        insulinDoseAccuracy = insulinDoseAccuracy.toString(),
                        postMealNotificationEnabled = postMealEnabled,
                        postMealNotificationDelay = postMealDelay.toString(),
                        trendNotificationEnabled = trendEnabled,
                        trendNotificationLowThreshold = trendLow.toString(),
                        trendNotificationHighThreshold = trendHigh.toString(),
                        trendNotificationTimeWindow = trendTimeWindow.toString(),
                        hasUnsavedChanges = false
                    )
                }
            }.collect{}
        }
    }

    fun setBreakfastCoefficient(coefficient: String) {
        _uiState.update { it.copy(breakfastCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setDinnerCoefficient(coefficient: String) {
        _uiState.update { it.copy(dinnerCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setSupperCoefficient(coefficient: String) {
        _uiState.update { it.copy(supperCoefficient = coefficient, hasUnsavedChanges = true) }
    }

    fun setCarbsPerBu(carbs: String) {
        _uiState.update { it.copy(carbsPerBu = carbs, hasUnsavedChanges = true) }
    }

    fun setDailyCarbsGoal(goal: String) {
        _uiState.update { it.copy(dailyCarbsGoal = goal, hasUnsavedChanges = true) }
    }

    fun setInsulinDoseAccuracy(accuracy: String) {
        _uiState.update { it.copy(insulinDoseAccuracy = accuracy, hasUnsavedChanges = true) }
    }

    fun setPostMealNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(postMealNotificationEnabled = enabled, hasUnsavedChanges = true) }
    }

    fun setPostMealNotificationDelay(delay: String) {
        _uiState.update { it.copy(postMealNotificationDelay = delay, hasUnsavedChanges = true) }
    }

    fun setTrendNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(trendNotificationEnabled = enabled, hasUnsavedChanges = true) }
    }

    fun setTrendNotificationLowThreshold(threshold: String) {
        _uiState.update { it.copy(trendNotificationLowThreshold = threshold, hasUnsavedChanges = true) }
    }

    fun setTrendNotificationHighThreshold(threshold: String) {
        _uiState.update { it.copy(trendNotificationHighThreshold = threshold, hasUnsavedChanges = true) }
    }

    fun setTrendNotificationTimeWindow(window: String) {
        _uiState.update { it.copy(trendNotificationTimeWindow = window, hasUnsavedChanges = true) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsDataStore.saveSettings(
                breakfast = _uiState.value.breakfastCoefficient.replace(',', '.').toFloatOrNull() ?: 0f,
                dinner = _uiState.value.dinnerCoefficient.replace(',', '.').toFloatOrNull() ?: 0f,
                supper = _uiState.value.supperCoefficient.replace(',', '.').toFloatOrNull() ?: 0f,
                carbsPerBu = _uiState.value.carbsPerBu.replace(',', '.').toFloatOrNull() ?: 10f,
                dailyCarbsGoal = _uiState.value.dailyCarbsGoal.replace(',', '.').toFloatOrNull() ?: 200f,
                insulinDoseAccuracy = _uiState.value.insulinDoseAccuracy.replace(',', '.').toFloatOrNull() ?: 0.5f,
                postMealEnabled = _uiState.value.postMealNotificationEnabled,
                postMealDelay = _uiState.value.postMealNotificationDelay.toIntOrNull() ?: 120,
                trendNotificationsEnabled = _uiState.value.trendNotificationEnabled,
                trendLowThreshold = _uiState.value.trendNotificationLowThreshold.replace(',', '.').toFloatOrNull() ?: 4.0f,
                trendHighThreshold = _uiState.value.trendNotificationHighThreshold.replace(',', '.').toFloatOrNull() ?: 10.0f,
                trendTimeWindow = _uiState.value.trendNotificationTimeWindow.toIntOrNull() ?: 60
            )
            _uiState.update { it.copy(hasUnsavedChanges = false) }
        }
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    fun exportDataAsCsv() {
        viewModelScope.launch {
            val csvContent = exportDataUseCase.generateCsvContent()
            _exportChannel.send(csvContent)
        }
    }

    fun exportDataAsPdf(context: Context) {
        viewModelScope.launch {
            val uri = exportDataUseCase.generateAndSavePdf(context)
            if (uri != null) {
                _pdfExportChannel.send(uri)
            }
        }
    }
}