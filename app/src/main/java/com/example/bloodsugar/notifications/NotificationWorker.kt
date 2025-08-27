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
            when (type) {
                "daily" -> {
                    val time = inputData.getString("time")
                    Log.d(TAG, "Daily notification. Showing notification.")
                    showNotification(message, time)
                }
                "interval" -> {
                    val startTime = inputData.getString("startTime")
                    val endTime = inputData.getString("endTime")
                    if (startTime != null && endTime != null && isTimeInWindow(startTime, endTime)) {
                        Log.d(TAG, "Interval notification within window. Showing notification.")
                        showNotification(message, null)
                    } else {
                        Log.d(TAG, "Interval notification outside window. Not showing.")
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown notification type: $type")
                }
            }

            if (isStopped) {
                Log.d(TAG, "Worker was stopped. Not rescheduling.")
                return Result.success()
            }

            reschedule(type)

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
            val channel = android.app.NotificationChannel("blood_sugar_notifications", "Blood Sugar Reminders", android.app.NotificationManager.IMPORTANCE_HIGH)
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
            .setContentTitle("Blood Sugar Reminder")
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = inputData.getInt("notification_id", (System.currentTimeMillis() / 1000).toInt())
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notification shown with ID: $notificationId")
    }

    private fun calculateInitialDelayForDaily(hour: Long, minute: Long): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.toInt())
            set(java.util.Calendar.MINUTE, minute.toInt())
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun reschedule(type: String) {
        val originalTag = this.tags.firstOrNull()
        if (originalTag == null) {
            Log.w(TAG, "No tag found, cannot reschedule")
            return
        }

        val delay: Long = when (type) {
            "daily" -> {
                val time = inputData.getString("time")
                if (time.isNullOrEmpty()) {
                    Log.e(TAG, "Cannot reschedule daily work without time, using 24h fallback")
                    TimeUnit.HOURS.toMillis(24)
                } else {
                    Log.d(TAG, "Rescheduling daily notification for time: $time")
                    val timeParts = time.split(":")
                    val hour = timeParts[0].toLong()
                    val minute = timeParts[1].toLong()
                    calculateInitialDelayForDaily(hour, minute)
                }
            }
            "interval" -> {
                val intervalValue = inputData.getInt("intervalMinutes", 60)
                val startTime = inputData.getString("startTime")
                val endTime = inputData.getString("endTime")

                if (startTime == null || endTime == null) {
                    Log.e(TAG, "Cannot reschedule interval work without start/end time. Stopping reschedule.")
                    return
                }

                val nextExecutionTime = calculateNextIntervalTimestamp(intervalValue, startTime, endTime)
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

    private fun isTimeInWindow(startTime: String, endTime: String): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val result = isTimeInWindow(startTime, endTime, calendar)
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        Log.d(TAG, "Time check - current: $currentHour:$currentMinute, window: $startTime-$endTime, inWindow: $result")
        return result
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
        Log.d(TAG, "Calculating next interval: $intervalMinutes minutes, from $startTimeStr to $endTimeStr")
        val now = java.util.Calendar.getInstance()
        val (startHour, startMinute) = startTimeStr.split(":").map { it.toInt() }

        // 1. Find an "anchor" time. This is the most recent startTime.
        var anchor = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, startHour)
            set(java.util.Calendar.MINUTE, startMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (anchor.after(now)) {
            anchor.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        Log.d(TAG, "Anchor time: ${anchor.time}")

        // 2. Start generating ticks from the anchor and find the first one after `now`.
        var nextTrigger = anchor.clone() as java.util.Calendar
        while (nextTrigger.timeInMillis <= now.timeInMillis) {
            nextTrigger.add(java.util.Calendar.MINUTE, intervalMinutes)
        }
        Log.d(TAG, "First potential trigger: ${nextTrigger.time}")

        // 3. Now we have the first potential trigger time. Check if it's in a valid window.
        // Loop until we find a valid one.
        for (i in 0..(1440 / intervalMinutes.coerceAtLeast(1))) { // Limit loop to one day's worth of intervals
            Log.d(TAG, "Checking trigger: ${nextTrigger.time}")
            if (isTimeInWindow(startTimeStr, endTimeStr, nextTrigger)) {
                Log.d(TAG, "Found valid trigger time: ${nextTrigger.time}")
                return nextTrigger.timeInMillis // Found it.
            }
            // If not, advance to the next tick and check again.
            nextTrigger.add(java.util.Calendar.MINUTE, intervalMinutes)
        }

        Log.w(TAG, "Could not find a valid next trigger time.")
        return -1L // Fallback, should not be reached
    }
}