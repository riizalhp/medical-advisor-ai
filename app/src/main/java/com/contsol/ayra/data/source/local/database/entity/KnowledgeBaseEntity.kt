package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class KnowledgeBaseEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val content: String,
    val last_updated: Long
)
