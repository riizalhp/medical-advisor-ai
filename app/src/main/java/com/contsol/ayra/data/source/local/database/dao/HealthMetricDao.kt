package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getStringOrNull
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.HealthMetric

class HealthMetricDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(healthMetric: HealthMetric): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("metric_type", healthMetric.metricType)
            put("value", healthMetric.value)
            put("timestamp", healthMetric.timestamp)
            put("notes", healthMetric.notes)
        }
        return db.insert("HealthMetric", null, values)
    }

    fun getAll(): List<HealthMetric> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM HealthMetric ORDER BY timestamp ASC", null)
        val healthMetrics = mutableListOf<HealthMetric>()

        cursor.use {
            while (it.moveToNext()) {
                val healthMetric = HealthMetric(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    metricType = it.getString(it.getColumnIndexOrThrow("metric_type")),
                    value = it.getString(it.getColumnIndexOrThrow("value")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                    notes = it.getStringOrNull(it.getColumnIndexOrThrow("notes")),
                )
                healthMetrics.add(healthMetric)
            }
        }

        return healthMetrics
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("HealthMetric", null, null)
    }
}
