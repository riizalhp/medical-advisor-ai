package com.contsol.ayra.data.source.local.database.model

data class HealthMetric(
    val id: Long = 0L,
    val metricType: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)
