package com.example.bloodsugar.domain

import com.example.bloodsugar.database.BloodSugarRecord



class TirCalculationUseCase {
    operator fun invoke(
        records: List<BloodSugarRecord>,
        thresholds: TirThresholds = TirThresholds()
    ): TirResult {
        val sortedRecords = records.sortedByDescending { it.timestamp }
        if (sortedRecords.size < 2) {
            return TirResult()
        }

        var durationVeryLow = 0L
        var durationLow = 0L
        var durationInRange = 0L
        var durationHigh = 0L
        var durationVeryHigh = 0L

        for (i in 0 until sortedRecords.size - 1) {
            val currentRecord = sortedRecords[i]
            val nextRecord = sortedRecords[i+1]
            val duration = currentRecord.timestamp - nextRecord.timestamp

            val avgValue = (currentRecord.value + nextRecord.value) / 2f

            when {
                avgValue < thresholds.veryLow -> durationVeryLow += duration
                avgValue < thresholds.low -> durationLow += duration
                avgValue <= thresholds.high -> durationInRange += duration
                avgValue <= thresholds.veryHigh -> durationHigh += duration
                else -> durationVeryHigh += duration
            }
        }

        val totalDuration = sortedRecords.first().timestamp - sortedRecords.last().timestamp
        return if (totalDuration > 0) {
            val totalDurationFloat = totalDuration.toFloat()
            TirResult(
                veryLow = (durationVeryLow / totalDurationFloat) * 100,
                low = (durationLow / totalDurationFloat) * 100,
                timeInRange = (durationInRange / totalDurationFloat) * 100,
                high = (durationHigh / totalDurationFloat) * 100,
                veryHigh = (durationVeryHigh / totalDurationFloat) * 100
            )
        } else {
            TirResult()
        }
    }
}