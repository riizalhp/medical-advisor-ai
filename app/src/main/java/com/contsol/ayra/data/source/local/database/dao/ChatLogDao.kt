package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import androidx.core.database.getStringOrNull
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.ChatLog

class ChatLogDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(chat: ChatLog): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("message_content", chat.messageContent)
            put("is_user_message", if (chat.isUserMessage) 1 else 0)
            put("image_url", chat.imageUrl)
            put("timestamp", chat.timestamp)
        }
        return db.insert("ChatLog", null, values)
    }

    fun getAll(): List<ChatLog> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ChatLog ORDER BY timestamp ASC", null)
        val chatLogs = mutableListOf<ChatLog>()

        cursor.use {
            while (it.moveToNext()) {
                val chat = ChatLog(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    messageContent = it.getString(it.getColumnIndexOrThrow("message_content")),
                    isUserMessage = it.getInt(it.getColumnIndexOrThrow("is_user_message")) == 1,
                    imageUrl = it.getStringOrNull(it.getColumnIndexOrThrow("image_url")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                )
                chatLogs.add(chat)
            }
        }

        return chatLogs
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete("ChatLog", null, null)
    }
}
