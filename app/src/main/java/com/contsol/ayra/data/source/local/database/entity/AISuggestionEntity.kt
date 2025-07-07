package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AISuggestionEntity(
    @PrimaryKey val id: Long,
    val date: Long,
    val category: String,
    val suggestion_text: String,
    val expired_time: Long
)
