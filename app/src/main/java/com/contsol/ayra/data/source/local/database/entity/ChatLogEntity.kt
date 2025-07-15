package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message_content: String,
    val is_user_message: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
