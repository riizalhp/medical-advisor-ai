package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getStringOrNull
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.PhotoLog

class PhotoLogDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(photo: PhotoLog): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("file_path", photo.filePath)
            put("timestamp", photo.timestamp)
            put("symptom_detected", photo.symptomDetected)
        }
        return db.insert("PhotoLog", null, values)
    }

    fun getAll(): List<PhotoLog> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM PhotoLog ORDER BY timestamp ASC", null)
        val photoLogs = mutableListOf<PhotoLog>()

        cursor.use {
            while (it.moveToNext()) {
                val photo = PhotoLog(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                    symptomDetected = it.getStringOrNull(it.getColumnIndexOrThrow("symptom_detected")),
                )
                photoLogs.add(photo)
            }
        }

        return photoLogs
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("PhotoLog", null, null)
    }
}
