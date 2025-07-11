package com.contsol.ayra.data.source.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.contsol.ayra.data.source.local.database.dao.AISuggestionDao
import com.contsol.ayra.data.source.local.database.dao.ChatLogDao
import com.contsol.ayra.data.source.local.database.dao.HealthMetricDao
import com.contsol.ayra.data.source.local.database.dao.PhotoLogDao
import com.contsol.ayra.data.source.local.database.dao.UserDao
import com.contsol.ayra.data.source.local.database.entity.AISuggestionEntity
import com.contsol.ayra.data.source.local.database.entity.ChatLogEntity
import com.contsol.ayra.data.source.local.database.entity.HealthMetricEntity
import com.contsol.ayra.data.source.local.database.entity.PhotoLogEntity
import com.contsol.ayra.data.source.local.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ChatLogEntity::class,
        HealthMetricEntity::class,
        PhotoLogEntity::class,
        AISuggestionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatLogDao(): ChatLogDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun photoLogDao(): PhotoLogDao
    abstract fun aiSuggestionDao(): AISuggestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthai_app_db"
                ).build().also { INSTANCE = it }
            }
    }
}