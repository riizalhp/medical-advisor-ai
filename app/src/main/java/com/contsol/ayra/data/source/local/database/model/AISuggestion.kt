package com.contsol.ayra.data.source.local.database.model

data class AISuggestion(
    val id: Long = 0L,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val suggestionText: String,
    val expiredTime: Long = System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000) // a day in milliseconds
)
