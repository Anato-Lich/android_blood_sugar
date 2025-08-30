package com.example.bloodsugar.features.notifications

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.NotificationSetting
import com.example.bloodsugar.notifications.NotificationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

data class NotificationsUiState(
    val notifications: List<NotificationSetting> = emptyList()
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationSettingDao = AppDatabase.getDatabase(application).notificationSettingDao()
    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationSettingDao.getAll().collect { notifications ->
                _uiState.update { it.copy(notifications = notifications) }
            }
        }
    }

    fun addNotification(setting: NotificationSetting) {
        viewModelScope.launch {
            val newId = notificationSettingDao.insert(setting)
            val settingToSchedule = setting.copy(id = newId.toInt())
            scheduleNotification(settingToSchedule)
        }
    }

    fun deleteNotification(setting: NotificationSetting) {
        viewModelScope.launch {
            notificationSettingDao.delete(setting)
            cancelNotification(setting)
        }
    }

    fun toggleNotification(setting: NotificationSetting, isEnabled: Boolean) {
        viewModelScope.launch {
            val updatedSetting = setting.copy(isEnabled = isEnabled)
            notificationSettingDao.update(updatedSetting)
            if (isEnabled) {
                scheduleNotification(updatedSetting)
            } else {
                cancelNotification(updatedSetting)
            }
        }
    }

    fun checkWorkStatus(setting: NotificationSetting) {
        val workInfosFuture = workManager.getWorkInfosByTag(setting.id.toString())
        workInfosFuture.addListener({
            try {
                val workInfos = workInfosFuture.get()
                if (workInfos.isEmpty()) {
                    Log.d("WorkStatus", "No WorkInfo found for tag: ${setting.id}")
                }
                for (workInfo in workInfos) {
                    var delayStr = "N/A"
                    if (workInfo.state == WorkInfo.State.ENQUEUED) {
                        val delayMillis = when (setting.type) {
                            "daily" -> {
                                val timeParts = setting.time.split(":")
                                val hour = timeParts[0].toLong()
                                val minute = timeParts[1].toLong()
                                calculateInitialDelayForDaily(hour, minute)
                            }
                            "interval" -> {
                                if (setting.startTime != null && setting.endTime != null) {
                                    val nextExecutionTime = calculateNextIntervalTimestamp(setting.intervalMinutes, setting.startTime, setting.endTime)
                                    if (nextExecutionTime != -1L) {
                                        (nextExecutionTime - System.currentTimeMillis()).coerceAtLeast(0)
                                    } else {
                                        -1L
                                    }
                                } else {
                                    -1L
                                }
                            }
                            else -> -1L
                        }

                        if (delayMillis != -1L) {
                            val hours = TimeUnit.MILLISECONDS.toHours(delayMillis)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis) % 60
                            val seconds = TimeUnit.MILLISECONDS.toSeconds(delayMillis) % 60
                            delayStr = "$hours h, $minutes m, $seconds s"
                        }
                    }

                    Log.d("WorkStatus", "WorkInfo for tag '${setting.id}': State=${workInfo.state}, ID=${workInfo.id}, Calculated Delay: $delayStr, Data=${workInfo.outputData}")
                }
            } catch (e: ExecutionException) {
                Log.e("WorkStatus", "Error getting work info", e)
            } catch (e: InterruptedException) {
                Log.e("WorkStatus", "Error getting work info", e)
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    private fun scheduleNotification(setting: NotificationSetting) {
        val inputData = workDataOf(
            "notification_id" to setting.id,
            "message" to setting.message,
            "time" to setting.time,
            "type" to setting.type,
            "intervalMinutes" to setting.intervalMinutes,
            "startTime" to setting.startTime,
            "endTime" to setting.endTime
        )

        val workRequest = when (setting.type) {
            "daily" -> {
                val timeParts = setting.time.split(":")
                val hour = timeParts[0].toLong()
                val minute = timeParts[1].toLong()
                val initialDelay = calculateInitialDelayForDaily(hour, minute)
                OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag(setting.id.toString())
                    .build()
            }
            "interval" -> {
                if (setting.startTime == null || setting.endTime == null) {
                    Log.e("NotificationsViewModel", "Cannot schedule interval notification ${setting.id} without start/end time.")
                    null
                } else {
                    val nextExecutionTime = calculateNextIntervalTimestamp(setting.intervalMinutes, setting.startTime!!, setting.endTime!!)
                    if (nextExecutionTime == -1L) {
                        Log.e("NotificationsViewModel", "Could not calculate next execution time for ${setting.id}. Not scheduling.")
                        null
                    } else {
                        val delay = (nextExecutionTime - System.currentTimeMillis()).coerceAtLeast(0)
                        OneTimeWorkRequestBuilder<NotificationWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .addTag(setting.id.toString())
                            .build()
                    }
                }
            }
            else -> null
        }
        workRequest?.let {
            workManager.enqueueUniqueWork(
                setting.id.toString(),
                ExistingWorkPolicy.REPLACE,
                it
            )
        }
    }

    private fun cancelNotification(setting: NotificationSetting) {
        workManager.cancelAllWorkByTag(setting.id.toString())
    }

    private fun calculateInitialDelayForDaily(hour: Long, minute: Long): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.toInt())
            set(java.util.Calendar.MINUTE, minute.toInt())
            set(java.util.Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun isTimeInWindow(startTime: String, endTime: String, calendar: java.util.Calendar): Boolean {
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        val (startHour, startMinute) = startTime.split(":").map { it.toInt() }
        val (endHour, endMinute) = endTime.split(":").map { it.toInt() }

        val startTimeInMinutes = startHour * 60 + startMinute
        val originalEndTimeInMinutes = endHour * 60 + endMinute
        val endTimeInMinutesWithBuffer = originalEndTimeInMinutes + 1
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        return if (startTimeInMinutes <= originalEndTimeInMinutes) {
            currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutesWithBuffer
        } else { // Overnight case
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutesWithBuffer
        }
    }

    private fun calculateNextIntervalTimestamp(intervalMinutes: Int, startTimeStr: String, endTimeStr: String): Long {
        val now = java.util.Calendar.getInstance()
        val (startHour, startMinute) = startTimeStr.split(":").map { it.toInt() }

        var anchor = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, startHour)
            set(java.util.Calendar.MINUTE, startMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (anchor.after(now)) {
            anchor.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        var nextTrigger = anchor.clone() as java.util.Calendar
        while (nextTrigger.timeInMillis <= now.timeInMillis) {
            nextTrigger.add(java.util.Calendar.MINUTE, intervalMinutes)
        }

        for (i in 0..(1440 / intervalMinutes.coerceAtLeast(1))) {
            if (isTimeInWindow(startTimeStr, endTimeStr, nextTrigger)) {
                return nextTrigger.timeInMillis
            }
            nextTrigger.add(java.util.Calendar.MINUTE, intervalMinutes)
        }

        return -1L
    }
}