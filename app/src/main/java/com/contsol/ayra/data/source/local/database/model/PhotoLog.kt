package com.contsol.ayra.data.source.local.database.model

data class PhotoLog(
    val id: Long = 0L,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val symptomDetected: String? = null
)
