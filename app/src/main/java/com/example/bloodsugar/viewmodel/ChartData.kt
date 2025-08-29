package com.example.bloodsugar.viewmodel

import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord

data class ChartData(
    val records: List<BloodSugarRecord>,
    val events: List<EventRecord>,
    val activities: List<ActivityRecord>,
    val avg: Float,
    val min: Float,
    val max: Float,
    val rangeStart: Long,
    val rangeEnd: Long
)