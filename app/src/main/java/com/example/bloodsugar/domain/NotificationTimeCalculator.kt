package com.example.bloodsugar.domain

import java.util.Calendar

object NotificationTimeCalculator {

    fun calculateInitialDelayForDaily(hour: Long, minute: Long): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.toInt())
            set(Calendar.MINUTE, minute.toInt())
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    fun calculateNextIntervalTimestamp(intervalMinutes: Int, startTimeStr: String, endTimeStr: String): Long {
        val now = Calendar.getInstance()
        val (startHour, startMinute) = startTimeStr.split(":").map { it.toInt() }

        val anchor = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (anchor.after(now)) {
            anchor.add(Calendar.DAY_OF_YEAR, -1)
        }

        val nextTrigger = anchor.clone() as Calendar
        while (nextTrigger.timeInMillis <= now.timeInMillis) {
            nextTrigger.add(Calendar.MINUTE, intervalMinutes)
        }

        for (i in 0..(1440 / intervalMinutes.coerceAtLeast(1))) {
            if (isTimeInWindow(startTimeStr, endTimeStr, nextTrigger)) {
                return nextTrigger.timeInMillis
            }
            nextTrigger.add(Calendar.MINUTE, intervalMinutes)
        }

        return -1L
    }

    fun isTimeInWindow(startTime: String, endTime: String, calendar: Calendar = Calendar.getInstance()): Boolean {
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
}
