package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.KnowledgeBase

class KnowledgeBaseDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(knowledge: KnowledgeBase): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("category", knowledge.category)
            put("title", knowledge.title)
            put("content", knowledge.content)
            put("last_updated", knowledge.lastUpdated)
        }
        return db.insert("KnowledgeBase", null, values)
    }

    fun getAll(): List<KnowledgeBase> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM KnowledgeBase", null)
        val knowledgeBases = mutableListOf<KnowledgeBase>()

        cursor.use {
            while (it.moveToNext()) {
                val knowledge = KnowledgeBase(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    category = it.getString(it.getColumnIndexOrThrow("category")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    content = it.getString(it.getColumnIndexOrThrow("content")),
                    lastUpdated = it.getLong(it.getColumnIndexOrThrow("last_updated")),
                )
                knowledgeBases.add(knowledge)
            }
        }

        return knowledgeBases
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("KnowledgeBase", null, null)
    }
}
