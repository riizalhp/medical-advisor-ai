package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contsol.ayra.data.source.local.database.entity.AISuggestionEntity

@Dao
interface AISuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aiSuggestion: AISuggestionEntity): Long

    @Query("SELECT * FROM AISuggestionEntity WHERE date = :date")
    suspend fun getTodaySuggestion(date: Long): AISuggestionEntity?

    @Update
    suspend fun updateSuggestion(aiSuggestion: AISuggestionEntity)

    @Query("DELETE FROM AISuggestionEntity WHERE expired_time < :date")
    suspend fun deleteExpiredSuggestion(date: Long)
}