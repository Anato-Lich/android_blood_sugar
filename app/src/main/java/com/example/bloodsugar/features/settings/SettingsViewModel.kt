package com.example.bloodsugar.features.settings

import android.app.Application
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.AppDatabase
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
    val hasUnsavedChanges: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = androidx.work.WorkManager.getInstance(application)
    private val repository: BloodSugarRepository

    private val _exportChannel = Channel<String>()
    val exportChannel = _exportChannel.receiveAsFlow()

    private val _pdfExportChannel = Channel<Uri>()
    val pdfExportChannel = _pdfExportChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BloodSugarRepository(db)

        viewModelScope.launch {
            combine(
                settingsDataStore.breakfastCoefficient,
                settingsDataStore.dinnerCoefficient,
                settingsDataStore.supperCoefficient,
                settingsDataStore.carbsPerBu,
                settingsDataStore.dailyCarbsGoal,
                settingsDataStore.insulinDoseAccuracy,
                settingsDataStore.postMealNotificationEnabled,
                settingsDataStore.postMealNotificationDelay
            ) { values ->
                val breakfast = values[0] as Float
                val dinner = values[1] as Float
                val supper = values[2] as Float
                val carbsPerBu = values[3] as Float
                val dailyCarbsGoal = values[4] as Float
                val insulinDoseAccuracy = values[5] as Float
                val postMealEnabled = values[6] as Boolean
                val postMealDelay = values[7] as Int

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
                postMealDelay = _uiState.value.postMealNotificationDelay.toIntOrNull() ?: 120
            )
            _uiState.update { it.copy(hasUnsavedChanges = false) }
        }
    }

    fun cancelAllWork() {
        workManager.cancelAllWork()
    }

    fun exportDataAsCsv() {
        viewModelScope.launch {
            val csvContent = generateCsvContent()
            _exportChannel.send(csvContent)
        }
    }

    fun exportDataAsPdf(context: Context) {
        viewModelScope.launch {
            val uri = generateAndSavePdf(context)
            if (uri != null) {
                _pdfExportChannel.send(uri)
            }
        }
    }

    private suspend fun generateAndSavePdf(context: Context): Uri? {
        val bloodSugarRecords = repository.getAllRecordsList()
        val eventRecords = repository.getAllEventsList()
        val activityRecords = repository.getAllActivitiesList()

        val allRecords = (bloodSugarRecords.map { it as Any } + eventRecords.map { it as Any } + activityRecords.map { it as Any }).sortedByDescending {
            when (it) {
                is com.example.bloodsugar.database.BloodSugarRecord -> it.timestamp
                is com.example.bloodsugar.database.EventRecord -> it.timestamp
                is com.example.bloodsugar.database.ActivityRecord -> it.timestamp
                else -> 0
            }
        }

        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = android.graphics.Paint()

        var yPosition = 40f
        paint.textSize = 16f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("Blood Sugar Report", 40f, yPosition, paint)
        yPosition += 40f

        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        allRecords.forEach {
            if (yPosition > 800) { // New page if content overflows
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 40f
            }

            val recordText = when (it) {
                is com.example.bloodsugar.database.BloodSugarRecord -> {
                    val timestamp = sdf.format(java.util.Date(it.timestamp))
                    "$timestamp - Blood Sugar: ${it.value} mmol/L - ${it.comment}"
                }
                is com.example.bloodsugar.database.EventRecord -> {
                    val timestamp = sdf.format(java.util.Date(it.timestamp))
                    val unit = if (it.type == "INSULIN") "units" else "grams"
                    "$timestamp - ${it.type}: ${it.value} $unit - ${it.foodName ?: ""}"
                }
                is com.example.bloodsugar.database.ActivityRecord -> {
                    val timestamp = sdf.format(java.util.Date(it.timestamp))
                    "$timestamp - Activity: ${it.type} for ${it.durationMinutes} min (${it.intensity})"
                }
                else -> ""
            }
            canvas.drawText(recordText, 40f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)

        try {
            val exportsDir = java.io.File(context.cacheDir, "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }
            val file = java.io.File(exportsDir, "bloodsugar_report_${System.currentTimeMillis()}.pdf")
            val fos = java.io.FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            return FileProvider.getUriForFile(context, "com.example.bloodsugar.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private suspend fun generateCsvContent(): String {
        val bloodSugarRecords = repository.getAllRecordsList()
        val eventRecords = repository.getAllEventsList()
        val activityRecords = repository.getAllActivitiesList()

        val allRecords = (bloodSugarRecords.map { it as Any } + eventRecords.map { it as Any } + activityRecords.map { it as Any }).sortedByDescending {
            when (it) {
                is com.example.bloodsugar.database.BloodSugarRecord -> it.timestamp
                is com.example.bloodsugar.database.EventRecord -> it.timestamp
                is com.example.bloodsugar.database.ActivityRecord -> it.timestamp
                else -> 0
            }
        }

        val header = "Timestamp,Type,Value,Unit,Comment/Details"
        val rows = allRecords.map { record ->
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            when (record) {
                is com.example.bloodsugar.database.BloodSugarRecord -> {
                    val timestamp = sdf.format(java.util.Date(record.timestamp))
                    "$timestamp,Blood Sugar,${record.value},mmol/L,${record.comment.replace(",", ";")}"
                }
                is com.example.bloodsugar.database.EventRecord -> {
                    val timestamp = sdf.format(java.util.Date(record.timestamp))
                    val unit = if (record.type == "INSULIN") "units" else "grams"
                    val details = record.foodName ?: ""
                    "$timestamp,${record.type},${record.value},$unit,${details.replace(",", ";")}"
                }
                is com.example.bloodsugar.database.ActivityRecord -> {
                    val timestamp = sdf.format(java.util.Date(record.timestamp))
                    val details = "${record.type} (${record.intensity})"
                    "$timestamp,Activity,${record.durationMinutes},minutes,$details"
                }
                else -> ""
            }
        }

        return header + "\n" + rows.joinToString("\n")
    }
}
