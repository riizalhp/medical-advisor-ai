package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.AISuggestion

class AISuggestionDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(suggestion: AISuggestion): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", suggestion.date)
            put("category", suggestion.category)
            put("suggestion_text", suggestion.suggestionText)
            put("expired_time", suggestion.expiredTime)
        }
        return db.insert("AISuggestion", null, values)
    }

    fun getAll(): List<AISuggestion> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM AISuggestion ORDER BY timestamp ASC", null)
        val aISuggestions = mutableListOf<AISuggestion>()

        cursor.use {
            while (it.moveToNext()) {
                val suggestion = AISuggestion(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    date = it.getLong(it.getColumnIndexOrThrow("date")),
                    category = it.getString(it.getColumnIndexOrThrow("category")),
                    suggestionText = it.getString(it.getColumnIndexOrThrow("suggestion_text")),
                    expiredTime = it.getLong(it.getColumnIndexOrThrow("expired_time")),
                )
                aISuggestions.add(suggestion)
            }
        }

        return aISuggestions
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("AISuggestion", null, null)
    }
}
