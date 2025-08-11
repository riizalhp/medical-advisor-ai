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
            put("age", user.age)
            put("gender", user.gender)
            put("weight", user.weight)
            put("height", user.height)
            put("blood_type", user.bloodType)
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
                    age = it.getInt(it.getColumnIndexOrThrow("age")),
                    gender = it.getString(it.getColumnIndexOrThrow("gender")),
                    weight = it.getDouble(it.getColumnIndexOrThrow("weight")),
                    height = it.getDouble(it.getColumnIndexOrThrow("height")),
                    bloodType = it.getString(it.getColumnIndexOrThrow("blood_type")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                )
                users.add(user)
            }
        }

        return users[0]
    }

    fun getUserName(): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT name FROM User LIMIT 1", null)
        var userName = ""

        cursor.use {
            while (it.moveToNext()) {
                userName = it.getString(it.getColumnIndexOrThrow("name"))
            }
        }

        return userName
    }

    fun deleteUser() {
        val db = dbHelper.writableDatabase
        db.delete("User", null, null)
    }
}
