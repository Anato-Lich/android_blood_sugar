package com.example.bloodsugar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.bloodsugar.MainActivity
import com.example.bloodsugar.R
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.BloodSugarDao
import com.example.bloodsugar.database.NotificationSetting
import com.example.bloodsugar.database.NotificationSettingDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PersistentNotificationService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationSettingDao: NotificationSettingDao by lazy {
        AppDatabase.getDatabase(this).notificationSettingDao()
    }

    private val bloodSugarRecordDao: BloodSugarDao by lazy {
        AppDatabase.getDatabase(this).bloodSugarDao()
    }

    private fun tickerFlow(period: Long, initialDelay: Long = 0L) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification(null, null, null, null, null).build())

        scope.launch {
            val dailyRecordsFlow = tickerFlow(TimeUnit.MINUTES.toMillis(1)).flatMapLatest {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.HOURS.toMillis(24)
                bloodSugarRecordDao.getRecordsInRange(startTime, endTime)
            }

            combine(
                notificationSettingDao.getEnabledNotifications(),
                dailyRecordsFlow
            ) { notifications, dailyRecords ->
                val closestNotifications = getClosestNotifications(notifications)

                val dailyAverage = dailyRecords.map { it.value }.average().toFloat().takeIf { !it.isNaN() }
                val lastValue = dailyRecords.maxByOrNull { it.timestamp }?.value
                val minValue = dailyRecords.minOfOrNull { it.value }
                val maxValue = dailyRecords.maxOfOrNull { it.value }

                notificationManager.notify(1, createNotification(closestNotifications, dailyAverage, lastValue, minValue, maxValue).build())
            }.debounce(500L).collect()
        }

        return START_STICKY
    }

    private fun getClosestNotifications(notifications: List<NotificationSetting>): List<NotificationSetting> {
        if (notifications.isEmpty()) return emptyList()

        val notificationsWithNextTime = notifications.map { it to getNextExecutionTime(it) }
        val minTime = notificationsWithNextTime.minOfOrNull { it.second } ?: return emptyList()

        return notificationsWithNextTime.filter { it.second == minTime }.map { it.first }
    }

    private fun calculateNextIntervalTimestamp(intervalMinutes: Int, startTimeStr: String, endTimeStr: String): Long {
        val now = Calendar.getInstance()
        val (startHour, startMinute) = startTimeStr.split(":").map { it.toInt() }

        // 1. Find an "anchor" time. This is the most recent startTime.
        var anchor = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (anchor.after(now)) {
            anchor.add(Calendar.DAY_OF_YEAR, -1)
        }

        // 2. Start generating ticks from the anchor and find the first one after `now`.
        var nextTrigger = anchor.clone() as Calendar
        while (nextTrigger.timeInMillis <= now.timeInMillis) {
            nextTrigger.add(Calendar.MINUTE, intervalMinutes)
        }

        // 3. Now we have the first potential trigger time. Check if it's in a valid window.
        // Loop until we find a valid one.
        for (i in 0..(1440 / intervalMinutes.coerceAtLeast(1))) { // Limit loop to one day's worth of intervals
            if (isTimeInWindow(startTimeStr, endTimeStr, nextTrigger)) {
                return nextTrigger.timeInMillis // Found it.
            }
            // If not, advance to the next tick and check again.
            nextTrigger.add(Calendar.MINUTE, intervalMinutes)
        }

        return -1L // Fallback, should not be reached
    }

    private fun isTimeInWindow(startTime: String, endTime: String, calendar: Calendar = Calendar.getInstance()): Boolean {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

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

    private fun getNextExecutionTime(setting: NotificationSetting): Long {
        return when (setting.type) {
            "daily" -> {
                val now = Calendar.getInstance()
                val target = Calendar.getInstance()
                val timeParts = setting.time.split(":")
                target.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                target.set(Calendar.MINUTE, timeParts[1].toInt())
                target.set(Calendar.SECOND, 0)
                target.set(Calendar.MILLISECOND, 0)
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                target.timeInMillis
            }
            "interval" -> {
                if (setting.startTime == null || setting.endTime == null) return Long.MAX_VALUE
                calculateNextIntervalTimestamp(setting.intervalMinutes, setting.startTime!!, setting.endTime!!)
            }
            else -> Long.MAX_VALUE
        }
    }

    private fun getValueColor(value: Float?): Int {
        if (value == null) {
            return android.graphics.Color.BLACK
        }
        return when {
            value < 4f -> android.graphics.Color.BLUE
            value <= 10f -> android.graphics.Color.parseColor("#006400") // Dark Green
            else -> android.graphics.Color.RED
        }
    }

    private fun createNotification(
        closestNotifications: List<NotificationSetting>?,
        dailyAverage: Float?,
        lastValue: Float?,
        minValue: Float?,
        maxValue: Float?
    ): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("persistent_notification_channel", "Persistent Notification", NotificationManager.IMPORTANCE_MIN)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create RemoteViews
        val remoteViews = RemoteViews(packageName, R.layout.notification_persistent)

        val nextReminderText = if (!closestNotifications.isNullOrEmpty()) {
            val nextTime = getNextExecutionTime(closestNotifications.first())
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTime))
            val reminderMessages = closestNotifications.joinToString { it.message }
            "Next: $reminderMessages at $formattedTime"
        } else {
            "No reminders set."
        }
        remoteViews.setTextViewText(R.id.notification_text, nextReminderText)

        if (dailyAverage != null) {
            remoteViews.setTextViewText(R.id.notification_average, "%.1f".format(dailyAverage))
            remoteViews.setTextColor(R.id.notification_average, getValueColor(dailyAverage))
            remoteViews.setViewVisibility(R.id.notification_average_label, View.VISIBLE)
            remoteViews.setViewVisibility(R.id.notification_average, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.notification_average_label, View.GONE)
            remoteViews.setViewVisibility(R.id.notification_average, View.GONE)
        }

        if (lastValue != null) {
            remoteViews.setTextViewText(R.id.notification_last, "Last: ${"%.1f".format(lastValue)}")
            remoteViews.setTextColor(R.id.notification_last, getValueColor(lastValue))
            remoteViews.setViewVisibility(R.id.notification_last, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.notification_last, View.GONE)
        }

        if (minValue != null) {
            remoteViews.setTextViewText(R.id.notification_min, "Min: ${"%.1f".format(minValue)}")
            remoteViews.setTextColor(R.id.notification_min, getValueColor(minValue))
            remoteViews.setViewVisibility(R.id.notification_min, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.notification_min, View.GONE)
        }

        if (maxValue != null) {
            remoteViews.setTextViewText(R.id.notification_max, "Max: ${"%.1f".format(maxValue)}")
            remoteViews.setTextColor(R.id.notification_max, getValueColor(maxValue))
            remoteViews.setViewVisibility(R.id.notification_max, View.VISIBLE)
        } else {
            remoteViews.setViewVisibility(R.id.notification_max, View.GONE)
        }

        return NotificationCompat.Builder(this, "persistent_notification_channel")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Blood Sugar Tracker")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}