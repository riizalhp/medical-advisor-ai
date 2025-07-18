package com.contsol.ayra.data.source.local.database.model

data class KnowledgeBase(
    val id: Long = 0L,
    val category: String,
    val title: String,
    val content: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
