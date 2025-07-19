package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = KnowledgeBaseEntity::class,
        parentColumns = ["id"],
        childColumns = ["knowledgeBaseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["knowledgeBaseId"])]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0,
    val knowledgeBaseId: String,
    val chunkIndex: Int,
    val content: String,
    val embedding: FloatArray,
    val metadata: String? = null
)
