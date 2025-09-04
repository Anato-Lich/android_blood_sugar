package com.example.bloodsugar.domain

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.bloodsugar.R
import com.example.bloodsugar.data.BloodSugarRepository
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.database.EventType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportDataUseCase(private val repository: BloodSugarRepository, private val context: Context) {

    suspend fun generateCsvContent(): String {
        val bloodSugarRecords = repository.getAllRecordsList()
        val eventRecords = repository.getAllEventsList()
        val activityRecords = repository.getAllActivitiesList()

        val allRecords = (bloodSugarRecords.map { it } + eventRecords.map { it } + activityRecords.map { it }).sortedByDescending {
            when (it) {
                is BloodSugarRecord -> it.timestamp
                is EventRecord -> it.timestamp
                is ActivityRecord -> it.timestamp
                else -> 0
            }
        }

        val header = context.getString(R.string.export_csv_header)
        val rows = allRecords.map { record ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            when (record) {
                is BloodSugarRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    "$timestamp,${context.getString(R.string.export_type_blood_sugar)},${record.value},${context.getString(R.string.export_unit_mmol_l)},${record.comment.replace(",", ";")}"
                }
                is EventRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    val unit = if (record.type == EventType.INSULIN) context.getString(R.string.export_unit_units) else context.getString(R.string.export_unit_grams)
                    val details = record.foodName ?: ""
                    "$timestamp,${record.type.name},${record.value},$unit,${details.replace(",", ";")}"
                }
                is ActivityRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    val details = "${record.type.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }} (${record.intensity.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault())}})"
                    "$timestamp,${context.getString(R.string.export_type_activity)},${record.durationMinutes},${context.getString(R.string.export_unit_minutes)},$details"
                }
                else -> ""
            }
        }

        return header + "\n" + rows.joinToString("\n")
    }

    suspend fun generateAndSavePdf(context: Context): Uri? {
        val bloodSugarRecords = repository.getAllRecordsList()
        val eventRecords = repository.getAllEventsList()
        val activityRecords = repository.getAllActivitiesList()

        val allRecords = (bloodSugarRecords.map { it } + eventRecords.map { it } + activityRecords.map { it }).sortedByDescending {
            when (it) {
                is BloodSugarRecord -> it.timestamp
                is EventRecord -> it.timestamp
                is ActivityRecord -> it.timestamp
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
        canvas.drawText(context.getString(R.string.export_pdf_title), 40f, yPosition, paint)
        yPosition += 40f

        paint.textSize = 10f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        allRecords.forEach { record ->
            if (yPosition > 800) { // New page if content overflows
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 40f
            }

            val recordText = when (record) {
                is BloodSugarRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    "$timestamp - ${context.getString(R.string.export_type_blood_sugar)}: ${record.value} ${context.getString(R.string.export_unit_mmol_l)} - ${record.comment}"
                }
                is EventRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    val unit = if (record.type == EventType.INSULIN) context.getString(R.string.export_unit_units) else context.getString(R.string.export_unit_grams)
                    "$timestamp - ${record.type.name}: ${record.value} $unit - ${record.foodName ?: ""}"
                }
                is ActivityRecord -> {
                    val timestamp = sdf.format(Date(record.timestamp))
                    "$timestamp - ${context.getString(R.string.export_type_activity)}: ${record.type.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }} for ${record.durationMinutes} min (${record.intensity.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault())}})"
                }
                else -> ""
            }
            canvas.drawText(recordText, 40f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)

        try {
            val exportsDir = File(context.cacheDir, "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }
            val file = File(exportsDir, "bloodsugar_report_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            return FileProvider.getUriForFile(context, "com.example.bloodsugar.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
