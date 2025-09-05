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
import androidx.core.content.ContextCompat
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

    private fun tickerFlow(period: Long, initialDelay: Long = 0L) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification(emptyList(), null, null, null, null, null, null, 0f, 0f).build())

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
                settingsDataStore.postMealNotificationDelay,
                settingsDataStore.dailyCarbsGoal
            ) { notifications, combinedData, postMealEnabled, postMealDelay, dailyCarbsGoal ->

                val (dailyRecords, dailyEvents, _) = combinedData

                val upcomingNotifications = getClosestNotifications(notifications)
                val postMealNotification = getPostMealNotification(dailyEvents, postMealEnabled, postMealDelay)

                val dailyAverage = dailyRecords.map { it.value }.average().toFloat().takeIf { !it.isNaN() }
                val lastValue = dailyRecords.maxByOrNull { it.timestamp }?.value
                val minValue = dailyRecords.minOfOrNull { it.value }
                val maxValue = dailyRecords.maxOfOrNull { it.value }
                val chartBitmap = generateChartBitmap(dailyRecords)
                val todaysCarbs = dailyEvents.filter { it.type == EventType.CARBS }.sumOf { it.value.toDouble() }.toFloat()

                notificationManager.notify(1, createNotification(upcomingNotifications, postMealNotification, dailyAverage, lastValue, minValue, maxValue, chartBitmap, todaysCarbs, dailyCarbsGoal).build())
            }.debounce(500L).collect()
        }

        return START_STICKY
    }

    private fun getPostMealNotification(events: List<com.example.bloodsugar.database.EventRecord>, enabled: Boolean, delay: Int): String? {
        if (!enabled) return null
        val lastCarbEvent = events.filter { it.type == EventType.CARBS }.maxByOrNull { it.timestamp }
        if (lastCarbEvent != null) {
            val checkTime = lastCarbEvent.timestamp + TimeUnit.MINUTES.toMillis(delay.toLong())
            if (checkTime > System.currentTimeMillis()) {
                val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(checkTime - System.currentTimeMillis())
                return "Check in ${remainingMinutes + 1}m"
            }
        }
        return null
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
            return ContextCompat.getColor(this, R.color.text_color_default)
        }
        return when (getSugarLevelCategory(value)) {
            SugarLevelCategory.LOW -> ContextCompat.getColor(this, R.color.secondaryBlue)
            SugarLevelCategory.IN_RANGE -> ContextCompat.getColor(this, R.color.primaryGreen)
            SugarLevelCategory.HIGH -> ContextCompat.getColor(this, R.color.errorRed)
        }
    }

    private fun getBackgroundColorResource(value: Float?): Int {
        if (value == null) {
            return R.drawable.notification_metric_box_default
        }
        return when (getSugarLevelCategory(value)) {
            SugarLevelCategory.LOW -> R.drawable.notification_metric_box_blue
            SugarLevelCategory.IN_RANGE -> R.drawable.notification_metric_box_green
            SugarLevelCategory.HIGH -> R.drawable.notification_metric_box_red
        }
    }

    private fun createNotification(
        upcomingNotifications: List<NotificationSetting>,
        postMealNotification: String?,
        dailyAverage: Float?,
        lastValue: Float?,
        minValue: Float?,
        maxValue: Float?,
        chartBitmap: Bitmap?,
        todaysCarbs: Float,
        dailyCarbsGoal: Float
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

        // Expanded RemoteViews
        val expandedRemoteViews = RemoteViews(packageName, R.layout.notification_persistent)
        val logSugarIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_OPEN_LOG_SUGAR_DIALOG"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val logSugarPendingIntent = PendingIntent.getActivity(this, 1, logSugarIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        expandedRemoteViews.setOnClickPendingIntent(R.id.notification_log_sugar_button, logSugarPendingIntent)

        // Notification list for expanded view
        val notificationLines = mutableListOf<String>()
        postMealNotification?.let { notificationLines.add(it) }
        upcomingNotifications.forEach {
            val nextTime = getNextExecutionTime(it)
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTime))
            notificationLines.add("${it.message} at $formattedTime")
        }

        val lineIds = listOf(R.id.notification_line_1, R.id.notification_line_2, R.id.notification_line_3)
        if (notificationLines.isEmpty()) {
            expandedRemoteViews.setViewVisibility(R.id.notification_text_container, View.GONE)
        } else {
            expandedRemoteViews.setViewVisibility(R.id.notification_text_container, View.VISIBLE)
            notificationLines.forEachIndexed { index, text ->
                if (index < lineIds.size) {
                    expandedRemoteViews.setTextViewText(lineIds[index], "• $text")
                    expandedRemoteViews.setViewVisibility(lineIds[index], View.VISIBLE)
                }
            }
            for (i in notificationLines.size until lineIds.size) {
                expandedRemoteViews.setViewVisibility(lineIds[i], View.GONE)
            }
        }

        // Set values for expanded view
        if (dailyAverage != null) {
            expandedRemoteViews.setTextViewText(R.id.notification_average, "%.1f".format(dailyAverage))
            expandedRemoteViews.setTextColor(R.id.notification_average, getValueColor(dailyAverage))
            expandedRemoteViews.setInt(R.id.notification_avg_container, "setBackgroundResource", getBackgroundColorResource(dailyAverage))
        } else {
            expandedRemoteViews.setTextViewText(R.id.notification_average, "--")
            expandedRemoteViews.setTextColor(R.id.notification_average, ContextCompat.getColor(this, R.color.text_color_gray))
            expandedRemoteViews.setInt(R.id.notification_avg_container, "setBackgroundResource", R.drawable.notification_metric_box_default)
        }

        if (lastValue != null) {
            expandedRemoteViews.setTextViewText(R.id.notification_last, "%.1f".format(lastValue))
            expandedRemoteViews.setTextColor(R.id.notification_last, getValueColor(lastValue))
            expandedRemoteViews.setInt(R.id.notification_last_container, "setBackgroundResource", getBackgroundColorResource(lastValue))
        } else {
            expandedRemoteViews.setTextViewText(R.id.notification_last, "--")
            expandedRemoteViews.setTextColor(R.id.notification_last, ContextCompat.getColor(this, R.color.text_color_gray))
            expandedRemoteViews.setInt(R.id.notification_last_container, "setBackgroundResource", R.drawable.notification_metric_box_default)
        }

        if (minValue != null) {
            expandedRemoteViews.setTextViewText(R.id.notification_min, "%.1f".format(minValue))
            expandedRemoteViews.setTextColor(R.id.notification_min, getValueColor(minValue))
            expandedRemoteViews.setInt(R.id.notification_min_container, "setBackgroundResource", getBackgroundColorResource(minValue))
        } else {
            expandedRemoteViews.setTextViewText(R.id.notification_min, "--")
            expandedRemoteViews.setTextColor(R.id.notification_min, ContextCompat.getColor(this, R.color.text_color_gray))
            expandedRemoteViews.setInt(R.id.notification_min_container, "setBackgroundResource", R.drawable.notification_metric_box_default)
        }

        if (maxValue != null) {
            expandedRemoteViews.setTextViewText(R.id.notification_max, "%.1f".format(maxValue))
            expandedRemoteViews.setTextColor(R.id.notification_max, getValueColor(maxValue))
            expandedRemoteViews.setInt(R.id.notification_max_container, "setBackgroundResource", getBackgroundColorResource(maxValue))
        } else {
            expandedRemoteViews.setTextViewText(R.id.notification_max, "--")
            expandedRemoteViews.setTextColor(R.id.notification_max, ContextCompat.getColor(this, R.color.text_color_gray))
            expandedRemoteViews.setInt(R.id.notification_max_container, "setBackgroundResource", R.drawable.notification_metric_box_default)
        }

        if (chartBitmap != null) {
            expandedRemoteViews.setImageViewBitmap(R.id.notification_chart, chartBitmap)
            expandedRemoteViews.setViewVisibility(R.id.notification_chart, View.VISIBLE)
        } else {
            expandedRemoteViews.setImageViewBitmap(R.id.notification_chart, null)
            expandedRemoteViews.setViewVisibility(R.id.notification_chart, View.GONE)
        }

        val progress = if (dailyCarbsGoal > 0) (todaysCarbs / dailyCarbsGoal * 100).toInt() else 0
        expandedRemoteViews.setProgressBar(R.id.notification_carb_progressbar, 100, progress, false)
        expandedRemoteViews.setTextViewText(R.id.notification_carb_text, "${todaysCarbs.toInt()}g")
        expandedRemoteViews.setTextColor(R.id.notification_carb_text, ContextCompat.getColor(this, R.color.primaryGreen))


        // Collapsed RemoteViews
        val collapsedRemoteViews = RemoteViews(packageName, R.layout.notification_collapsed)
        if (lastValue != null) {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_last, "%.1f".format(lastValue))
            collapsedRemoteViews.setTextColor(R.id.collapsed_notification_last, getValueColor(lastValue))
        } else {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_last, "--")
        }

        if (dailyAverage != null) {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_average, "%.1f".format(dailyAverage))
            collapsedRemoteViews.setTextColor(R.id.collapsed_notification_average, getValueColor(dailyAverage))
        } else {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_average, "--")
            collapsedRemoteViews.setTextColor(R.id.collapsed_notification_average, ContextCompat.getColor(this, R.color.text_color_gray))
        }

        val nextNotificationString = postMealNotification ?: upcomingNotifications.firstOrNull()?.let {
            val nextTime = getNextExecutionTime(it)
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextTime))
            "${it.message} at $formattedTime"
        }

        if (nextNotificationString != null) {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_next, nextNotificationString)
        } else {
            collapsedRemoteViews.setTextViewText(R.id.collapsed_notification_next, "No reminders")
        }


        return NotificationCompat.Builder(this, "persistent_notification_channel")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Blood Sugar Tracker")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedRemoteViews)
            .setCustomBigContentView(expandedRemoteViews)
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

        val density = resources.displayMetrics.density
        val width = (120 * density).toInt()  // 120dp width to match your layout
        val height = (56 * density).toInt()  // 56dp height to match your chart box
        val padding = 8f

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

        // Line paint
        val linePaint = Paint().apply {
            color = 0xFF4CAF50.toInt() // PrimaryGreen
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Fill paint
        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        // Point paint
        val pointPaint = Paint().apply {
            color = 0xFF4CAF50.toInt() // PrimaryGreen
            strokeWidth = 2f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Last point paint (highlighted)
        val lastPointPaint = Paint().apply {
            color = 0xFFFF5722.toInt() // Orange/Red for last point
            strokeWidth = 2f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val gradient = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), 0x804CAF50.toInt(), 0x004CAF50.toInt(), android.graphics.Shader.TileMode.CLAMP)
        fillPaint.shader = gradient

        val points = mutableListOf<Pair<Float, Float>>()

        sortedRecords.forEachIndexed { i, record ->
            val x = padding + ((record.timestamp - minTime) / timeRange) * (width - 2 * padding)
            val y = (height - padding) - ((record.value - minValue) / valueRange) * (height - 2 * padding)

            points.add(x to y)

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

        // Draw fill area
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)

        // Draw points
        points.forEachIndexed { index, point ->
            val (x, y) = point
            val paint = if (index == points.size - 1) lastPointPaint else pointPaint

            // Draw point as small circle
            canvas.drawCircle(x, y, if (index == points.size - 1) 4f else 3f, paint)

            // Add small stroke around last point for better visibility
            if (index == points.size - 1) {
                val strokePaint = Paint().apply {
                    color = 0xFFFFFFFF.toInt() // White stroke
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(x, y, 4f, strokePaint)
            }
        }

        return bitmap
    }
}