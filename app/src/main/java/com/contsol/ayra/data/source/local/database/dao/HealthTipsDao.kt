package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.Tips
import com.contsol.ayra.utils.getEndOfTheDayTimestamp
import androidx.core.database.sqlite.transaction

class HealthTipsDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(tips: Tips): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", tips.title)
            put("content", tips.content)
            put("expired_time", getEndOfTheDayTimestamp())
        }
        return db.insert("HealthTips", null, values)
    }

    fun insertAll(tips: List<Tips>): List<Long> {
        val db = dbHelper.writableDatabase
        val insertedIds = mutableListOf<Long>()
        db.transaction {
            try {
                tips.forEach { tip ->
                    val values = ContentValues().apply {
                        put("title", tip.title)
                        put("content", tip.content)
                        put("expired_time", getEndOfTheDayTimestamp())
                    }
                    val id = insert("HealthTips", null, values)
                    if (id != -1L) {
                        insertedIds.add(id)
                    }
                }
            } finally {
            }
        }
        return insertedIds
    }

    fun getAll(): List<Tips> {
        val db = dbHelper.readableDatabase
        val currentTime = System.currentTimeMillis()
        val cursor = db.rawQuery(
            "SELECT * FROM HealthTips WHERE expired_time > ?",
            arrayOf(currentTime.toString())
        )
        val healthTips = mutableListOf<Tips>()

        cursor.use {
            while (it.moveToNext()) {
                val tips = Tips(
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    content = it.getString(it.getColumnIndexOrThrow("content"))
                )
                healthTips.add(tips)
            }
        }

        return healthTips
    }

    fun deleteExpiredTips(): Int {
        val db = dbHelper.writableDatabase
        val currentTime = System.currentTimeMillis()
        // Delete tips where the expired_time is in the past
        return db.delete("HealthTips", "expired_time <= ?", arrayOf(currentTime.toString()))
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("HealthTips", null, null)
    }

    fun replaceAllTips(newTips: List<Tips>) {
        val db = dbHelper.writableDatabase
        db.transaction {
            try {
                // Delete all existing tips from the "HealthTips" table
                delete("HealthTips", null, null)

                // Insert new tips
                newTips.forEach { tip ->
                    val values = ContentValues().apply {
                        put("title", tip.title)
                        put("content", tip.content)
                        put("expired_time", getEndOfTheDayTimestamp()) // Sets expiry for today
                    }
                    insert("HealthTips", null, values)
                }
            } finally {
            }
        }
    }
}
