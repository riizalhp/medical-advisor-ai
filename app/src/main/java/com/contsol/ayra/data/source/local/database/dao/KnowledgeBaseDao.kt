package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contsol.ayra.data.source.local.database.entity.KnowledgeBaseEntity

interface KnowledgeBaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(knowledgeBase: KnowledgeBaseEntity): Long

    @Query("SELECT * FROM KnowledgeBaseEntity")
    suspend fun getAllKnowledgeBases(): List<KnowledgeBaseEntity>

    @Query("SELECT * FROM KnowledgeBaseEntity WHERE id = :id")
    suspend fun getKnowledgeBaseById(id: Long): KnowledgeBaseEntity?

    @Update
    suspend fun updateKnowledgeBase(knowledgeBase: KnowledgeBaseEntity)

    @Query("DELETE FROM KnowledgeBaseEntity WHERE id = :id")
    suspend fun deleteKnowledgeBase(id: Long)
}