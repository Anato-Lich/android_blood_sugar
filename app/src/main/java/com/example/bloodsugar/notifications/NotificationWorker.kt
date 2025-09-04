package com.example.bloodsugar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.bloodsugar.R
import com.example.bloodsugar.database.NotificationType
import com.example.bloodsugar.domain.NotificationTimeCalculator
import java.util.concurrent.TimeUnit

private const val TAG = "NotificationWorker"

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d(TAG, "Worker STARTED for tags: ${this.tags}")
        Log.d(TAG, "Worker input data: ${inputData.keyValueMap}")

        try {
            // Log all available data first
            inputData.keyValueMap.forEach { (key, value) ->
                Log.d(TAG, "Input data - $key: $value (type: ${value?.javaClass?.simpleName})")
            }

            val message = inputData.getString("message")
            val type = inputData.getString("type")

            Log.d(TAG, "Parsed - message: $message, type: $type")

            if (message.isNullOrEmpty() || type.isNullOrEmpty()) {
                Log.e(TAG, "Missing required input data - message: $message, type: $type")
                return Result.failure()
            }

            // Rest of your existing logic...
            when (type.lowercase()) {
                NotificationType.DAILY.name.lowercase() -> {
                    val time = inputData.getString("time")
                    Log.d(TAG, "Daily notification. Showing notification.")
                    showNotification(message, time)
                }
                NotificationType.INTERVAL.name.lowercase() -> {
                    val startTime = inputData.getString("startTime")
                    val endTime = inputData.getString("endTime")
                    if (startTime != null && endTime != null && NotificationTimeCalculator.isTimeInWindow(startTime, endTime)) {
                        Log.d(TAG, "Interval notification within window. Showing notification.")
                        showNotification(message, null)
                    } else {
                        Log.d(TAG, "Interval notification outside window. Not showing.")
                    }
                }
                "post-meal", "trend-immediate", "trend-preemptive" -> {
                    Log.d(TAG, "Post-meal or Trend notification. Showing notification.")
                    showNotification(message, null)
                }
                else -> {
                    Log.w(TAG, "Unknown notification type: $type")
                }
            }

            if (isStopped) {
                Log.d(TAG, "Worker was stopped. Not rescheduling.")
                return Result.success()
            }

            if (type.equals(NotificationType.DAILY.name, ignoreCase = true) || type.equals(NotificationType.INTERVAL.name, ignoreCase = true)) { // Only reschedule daily and interval notifications
                reschedule(type)
            }

            Log.d(TAG, "Worker COMPLETED successfully")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork()", e)
            return Result.failure()
        }
    }

    private fun showNotification(message: String, time: String?) {
        Log.d(TAG, "Showing notification: $message, time: $time")

        val notificationManager = applicationContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("blood_sugar_notifications", applicationContext.getString(R.string.notification_channel_name), android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(applicationContext, com.example.bloodsugar.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationText = if (time != null) "$message at $time" else message

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "blood_sugar_notifications")
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = inputData.getInt("notification_id", (System.currentTimeMillis() / 1000).toInt())
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }

    private fun reschedule(type: String) {
        val originalTag = this.tags.firstOrNull()
        if (originalTag == null) {
            Log.w(TAG, "No tag found, cannot reschedule")
            return
        }

        val delay: Long = when (type.lowercase()) {
            NotificationType.DAILY.name.lowercase() -> {
                val time = inputData.getString("time")
                if (time.isNullOrEmpty()) {
                    Log.e(TAG, "Cannot reschedule daily work without time, using 24h fallback")
                    TimeUnit.HOURS.toMillis(24)
                } else {
                    Log.d(TAG, "Rescheduling daily notification for time: $time")
                    val timeParts = time.split(":")
                    val hour = timeParts[0].toLong()
                    val minute = timeParts[1].toLong()
                    NotificationTimeCalculator.calculateInitialDelayForDaily(hour, minute)
                }
            }
            NotificationType.INTERVAL.name.lowercase() -> {
                val intervalValue = inputData.getInt("intervalMinutes", 60)
                val startTime = inputData.getString("startTime")
                val endTime = inputData.getString("endTime")

                if (startTime == null || endTime == null) {
                    Log.e(TAG, "Cannot reschedule interval work without start/end time. Stopping reschedule.")
                    return
                }

                val nextExecutionTime = NotificationTimeCalculator.calculateNextIntervalTimestamp(intervalValue, startTime, endTime)
                if (nextExecutionTime == -1L) {
                    Log.e(TAG, "Could not calculate next execution time. Stopping reschedule.")
                    return
                }

                val now = System.currentTimeMillis()
                (nextExecutionTime - now).coerceAtLeast(0)
            }
            else -> {
                Log.w(TAG, "Unknown type for rescheduling: $type")
                return
            }
        }

        Log.d(TAG, "Rescheduling work for tag '$originalTag'. Type: $type, Delay: ${delay}ms")

        val nextWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(originalTag)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            originalTag,
            ExistingWorkPolicy.REPLACE,
            nextWorkRequest
        )
        Log.d(TAG, "Work enqueued for tag: $originalTag")
    }
}
