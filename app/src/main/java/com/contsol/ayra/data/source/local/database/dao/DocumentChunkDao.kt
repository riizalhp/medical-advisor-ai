package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.contsol.ayra.data.source.local.database.entity.DocumentChunkEntity

@Dao
interface DocumentChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("SELECT * FROM DocumentChunkEntity WHERE knowledgeBaseId = :knowledgeBaseId")
    suspend fun getChunksByKnowledgeBaseId(knowledgeBaseId: String): List<DocumentChunkEntity>

    @Query("SELECT * FROM DocumentChunkEntity")
    suspend fun getAllChunks(): List<DocumentChunkEntity>

    @Query("DELETE FROM DocumentChunkEntity WHERE knowledgeBaseId = :knowledgeBaseId")
    suspend fun deleteChunksByKnowledgeBaseId(knowledgeBaseId: String)

    @Query("DELETE FROM DocumentChunkEntity")
    suspend fun deleteAllChunks()
}
