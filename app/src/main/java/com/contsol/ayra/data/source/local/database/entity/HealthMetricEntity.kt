package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HealthMetricEntity(
    @PrimaryKey val id: Long,
    val metric_type: String,
    val value: String,
    val timestamp: Long,
    val notes: String
)
