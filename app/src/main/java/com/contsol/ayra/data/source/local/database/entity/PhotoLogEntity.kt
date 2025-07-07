package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PhotoLog(
    @PrimaryKey val id: Long,
    val file_path: String,
    val timestamp: Long,
    val symptom_detected: String
)

