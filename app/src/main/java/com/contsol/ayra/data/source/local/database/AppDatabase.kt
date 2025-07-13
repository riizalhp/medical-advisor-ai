package com.contsol.ayra.data.source.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.contsol.ayra.data.source.local.database.dao.AISuggestionDao
import com.contsol.ayra.data.source.local.database.dao.ChatLogDao
import com.contsol.ayra.data.source.local.database.dao.DocumentChunkDao
import com.contsol.ayra.data.source.local.database.dao.HealthMetricDao
import com.contsol.ayra.data.source.local.database.dao.KnowledgeBaseDao
import com.contsol.ayra.data.source.local.database.dao.PhotoLogDao
import com.contsol.ayra.data.source.local.database.dao.UserDao
import com.contsol.ayra.data.source.local.database.entity.AISuggestionEntity
import com.contsol.ayra.data.source.local.database.entity.ChatLogEntity
import com.contsol.ayra.data.source.local.database.entity.DocumentChunkEntity
import com.contsol.ayra.data.source.local.database.entity.HealthMetricEntity
import com.contsol.ayra.data.source.local.database.entity.KnowledgeBaseEntity
import com.contsol.ayra.data.source.local.database.entity.PhotoLogEntity
import com.contsol.ayra.data.source.local.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ChatLogEntity::class,
        HealthMetricEntity::class,
        PhotoLogEntity::class,
        AISuggestionEntity::class,
        DocumentChunkEntity::class,
        KnowledgeBaseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatLogDao(): ChatLogDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun photoLogDao(): PhotoLogDao
    abstract fun aiSuggestionDao(): AISuggestionDao
    abstract fun documentChunkDao(): DocumentChunkDao
    abstract fun knowledgeBaseDao(): KnowledgeBaseDao

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