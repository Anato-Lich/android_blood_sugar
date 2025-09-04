package com.example.bloodsugar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.bloodsugar.MainActivity
import com.example.bloodsugar.R
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.data.SettingsDataStore
import com.example.bloodsugar.database.AppDatabase
import com.example.bloodsugar.database.EventType
import com.example.bloodsugar.database.NotificationSetting
import com.example.bloodsugar.database.NotificationSettingDao
import com.example.bloodsugar.database.NotificationType
import com.example.bloodsugar.domain.NotificationTimeCalculator
import com.example.bloodsugar.domain.SugarLevelCategory
import com.example.bloodsugar.domain.TirThresholds
import com.example.bloodsugar.domain.Trend
import com.example.bloodsugar.domain.TrendCalculationUseCase
import com.example.bloodsugar.domain.getSugarLevelCategory
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
import androidx.core.graphics.createBitmap

class PersistentNotificationService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val repository: BloodSugarRepository by lazy {
        BloodSugarRepository(AppDatabase.getDatabase(this))
    }

    private val notificationSettingDao: NotificationSettingDao by lazy {
        AppDatabase.getDatabase(this).notificationSettingDao()
    }

    private val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(this)
    }

    private val trendCalculationUseCase: TrendCalculationUseCase by lazy {
        TrendCalculationUseCase()
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
        startForeground(1, createNotification(null, null, null, null, null, null, null).build())

        scope.launch {
            val dataFlow = tickerFlow(TimeUnit.MINUTES.toMillis(1)).flatMapLatest {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - TimeUnit.HOURS.toMillis(24)
                repository.getCombinedDataInRange(startTime, endTime)
            }

            combine(
                notificationSettingDao.getEnabledNotifications(),
                dataFlow,
                settingsDataStore.postMealNotificationEnabled,
                settingsDataStore.postMealNotificationDelay
            ) { notifications, combinedData, postMealEnabled, postMealDelay ->

                val (dailyRecords, dailyEvents, _) = combinedData

                val nextScheduledReminder = getClosestNotifications(notifications).firstOrNull()
                val nextScheduledReminderTime = nextScheduledReminder?.let { getNextExecutionTime(it) }

                val lastCarbEvent = dailyEvents.filter { it.type == EventType.CARBS }.maxByOrNull { it.timestamp }
                var postMealCheckTime: Long? = null
                if (lastCarbEvent != null && postMealEnabled) {
                    val checkTime = lastCarbEvent.timestamp + TimeUnit.MINUTES.toMillis(postMealDelay.toLong())
                    if (checkTime > System.currentTimeMillis()) {
                        postMealCheckTime = checkTime
                    }
                }

                var nextNotificationText: String? = null
                if (postMealCheckTime != null && (nextScheduledReminderTime == null || postMealCheckTime < nextScheduledReminderTime)) {
                    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(postMealCheckTime - System.currentTimeMillis())
                    nextNotificationText = "Check sugar in ${remainingMinutes + 1} min"
                } else if (nextScheduledReminderTime != null) {
                    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextScheduledReminderTime))
                    nextNotificationText = "Next: ${nextScheduledReminder.message} at $formattedTime"
                }

                val dailyAverage = dailyRecords.map { it.value }.average().toFloat().takeIf { !it.isNaN() }
                val lastValue = dailyRecords.maxByOrNull { it.timestamp }?.value
                val minValue = dailyRecords.minOfOrNull { it.value }
                val maxValue = dailyRecords.maxOfOrNull { it.value }
                val trend = trendCalculationUseCase.calculateTrend(dailyRecords)
                val chartBitmap = generateChartBitmap(dailyRecords)

                notificationManager.notify(1, createNotification(nextNotificationText, dailyAverage, lastValue, minValue, maxValue, trend, chartBitmap).build())
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

    private fun getNextExecutionTime(setting: NotificationSetting): Long {
        return when (setting.type) {
            NotificationType.DAILY -> {
                val timeParts = setting.time.split(":")
                val now = Calendar.getInstance()
                val target = Calendar.getInstance()
                target.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                target.set(Calendar.MINUTE, timeParts[1].toInt())
                target.set(Calendar.SECOND, 0)
                target.set(Calendar.MILLISECOND, 0)
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                target.timeInMillis
            }
            NotificationType.INTERVAL -> {
                if (setting.startTime == null || setting.endTime == null) return Long.MAX_VALUE
                NotificationTimeCalculator.calculateNextIntervalTimestamp(setting.intervalMinutes, setting.startTime, setting.endTime)
            }
        }
    }

    private fun getValueColor(value: Float?): Int {
        if (value == null) {
            return android.graphics.Color.BLACK
        }
        return when (getSugarLevelCategory(value)) {
            SugarLevelCategory.LOW -> 0xFF2196F3.toInt() // SecondaryBlue
            SugarLevelCategory.IN_RANGE -> 0xFF4CAF50.toInt() // PrimaryGreen
            SugarLevelCategory.HIGH -> 0xFFD32F2F.toInt() // ErrorRed
        }
    }

    private fun createNotification(
        nextNotificationText: String?,
        dailyAverage: Float?,
        lastValue: Float?,
        minValue: Float?,
        maxValue: Float?,
        trend: Trend?,
        chartBitmap: Bitmap?
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

        val logSugarIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_OPEN_LOG_SUGAR_DIALOG"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val logSugarPendingIntent = PendingIntent.getActivity(this, 1, logSugarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.notification_log_sugar_button, logSugarPendingIntent)

        // Set main notification text
        remoteViews.setTextViewText(R.id.notification_text, nextNotificationText ?: "No reminders set.")
        remoteViews.setTextColor(R.id.notification_text, android.graphics.Color.BLACK)

        // Set average value
        if (dailyAverage != null) {
            remoteViews.setTextViewText(R.id.notification_average, "%.1f".format(dailyAverage))
            remoteViews.setTextColor(R.id.notification_average, getValueColor(dailyAverage))
            remoteViews.setViewVisibility(R.id.notification_average, View.VISIBLE)
        } else {
            remoteViews.setTextViewText(R.id.notification_average, "--")
            remoteViews.setTextColor(R.id.notification_average, android.graphics.Color.GRAY)
        }

        // Set last value and trend
        if (lastValue != null) {
            remoteViews.setTextViewText(R.id.notification_last, "%.1f".format(lastValue))
            remoteViews.setTextColor(R.id.notification_last, getValueColor(lastValue))

            if (trend != null) {
                val (arrowText, textColor) = when {
                    trend.rateOfChange > 2.5f -> "↑" to 0xFFD32F2F.toInt() // ErrorRed - Rapid increase
                    trend.rateOfChange > 0.8f -> "↗" to 0xFFFFA726.toInt() // TertiaryOrange - Moderate increase
                    trend.rateOfChange < -2.5f -> "↓" to 0xFF2196F3.toInt() // SecondaryBlue - Rapid decrease
                    trend.rateOfChange < -0.8f -> "↘" to 0xFF2196F3.toInt() // SecondaryBlue - Moderate decrease
                    else -> "→" to getValueColor(lastValue) // Stable
                }
                remoteViews.setTextViewText(R.id.notification_trend, arrowText)
                remoteViews.setTextColor(R.id.notification_trend, textColor)
                remoteViews.setViewVisibility(R.id.notification_trend, View.VISIBLE)
            } else {
                remoteViews.setViewVisibility(R.id.notification_trend, View.GONE)
            }
        } else {
            remoteViews.setTextViewText(R.id.notification_last, "--")
            remoteViews.setTextColor(R.id.notification_last, android.graphics.Color.GRAY)
            remoteViews.setViewVisibility(R.id.notification_trend, View.GONE)
        }

        // Set min/max range
        if (minValue != null && maxValue != null) {
            remoteViews.setTextViewText(R.id.notification_min_max, "%.1f-%.1f".format(minValue, maxValue))
            remoteViews.setTextColor(R.id.notification_min_max, android.graphics.Color.BLACK)
            remoteViews.setViewVisibility(R.id.notification_min_max, View.VISIBLE)
        } else {
            remoteViews.setTextViewText(R.id.notification_min_max, "--")
            remoteViews.setTextColor(R.id.notification_min_max, android.graphics.Color.GRAY)
        }

        // Set chart
        if (chartBitmap != null) {
            remoteViews.setImageViewBitmap(R.id.notification_chart, chartBitmap)
            remoteViews.setViewVisibility(R.id.notification_chart, View.VISIBLE)
        } else {
            remoteViews.setImageViewResource(R.id.notification_chart, android.R.color.transparent)
        }

        return NotificationCompat.Builder(this, "persistent_notification_channel")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Blood Sugar Tracker")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun generateChartBitmap(records: List<com.example.bloodsugar.database.BloodSugarRecord>): Bitmap? {
        if (records.size < 2) return null

        val width = 300 // width in pixels
        val height = 100 // height in pixels
        val padding = 10f

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val sortedRecords = records.sortedBy { it.timestamp }
        val minTime = sortedRecords.first().timestamp
        val maxTime = sortedRecords.last().timestamp
        val timeRange = (maxTime - minTime).toFloat().coerceAtLeast(1f)

        val minValue = sortedRecords.minOf { it.value }.coerceAtMost(TirThresholds.Default.low - 1)
        val maxValue = sortedRecords.maxOf { it.value }.coerceAtLeast(TirThresholds.Default.high + 1)
        val valueRange = (maxValue - minValue).coerceAtLeast(1f)

        val path = Path()
        val fillPath = Path()

        val paint = Paint().apply {
            color = 0xFF4CAF50.toInt() // PrimaryGreen
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        sortedRecords.forEachIndexed { i, record ->
            val x = padding + ((record.timestamp - minTime) / timeRange) * (width - 2 * padding)
            val y = (height - padding) - ((record.value - minValue) / valueRange) * (height - 2 * padding)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height.toFloat())
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == sortedRecords.size - 1) {
                fillPath.lineTo(x, height.toFloat())
                fillPath.close()
            }
        }

        val gradient = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), 0x804CAF50.toInt(), 0x004CAF50.toInt(), android.graphics.Shader.TileMode.CLAMP)
        fillPaint.shader = gradient
        canvas.drawPath(fillPath, fillPaint)

        canvas.drawPath(path, paint)

        return bitmap
    }
}