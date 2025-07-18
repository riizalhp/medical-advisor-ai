package com.contsol.ayra.data.source.local.database.dao

import android.content.ContentValues
import android.content.Context
import com.contsol.ayra.data.source.local.database.AppSQLiteHelper
import com.contsol.ayra.data.source.local.database.model.User

class UserDao(context: Context) {

    private val dbHelper = AppSQLiteHelper(context)

    fun insert(user: User): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", user.name)
            put("gender", user.gender)
            put("created_at", user.createdAt)
        }
        return db.insert("User", null, values)
    }

    fun getUser(): User {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM User LIMIT 1", null)
        val users = mutableListOf<User>()

        cursor.use {
            while (it.moveToNext()) {
                val user = User(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    gender = it.getString(it.getColumnIndexOrThrow("gender")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                )
                users.add(user)
            }
        }

        return users[0]
    }

    fun deleteUser() {
        val db = dbHelper.writableDatabase
        db.delete("User", null, null)
    }
}
