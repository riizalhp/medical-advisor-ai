package com.contsol.ayra.data.source.local.database.model

data class ChatLog(
    val id: Long = 0L,
    val messageContent: String,
    val isUserMessage: Boolean,
    val imageUrl: String? = null,
    val timestamp: Long =  System.currentTimeMillis(),
)
