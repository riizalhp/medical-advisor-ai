package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contsol.ayra.data.source.local.database.entity.ChatLogEntity

@Dao
interface ChatLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatLogEntity): Long

    @Query("SELECT * FROM ChatLogEntity ORDER BY timestamp DESC")
    suspend fun getChatLogs(): List<ChatLogEntity>

    @Update
    suspend fun updateChatLog(chat: ChatLogEntity)

    @Query("DELETE FROM ChatLogEntity WHERE timestamp = :timestamp")
    suspend fun deleteChatLog(timestamp: Long)
}