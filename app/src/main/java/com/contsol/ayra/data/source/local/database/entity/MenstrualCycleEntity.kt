package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MenstrualCycleEntity(
    @PrimaryKey val id: Long,
    val start_date: Long,
    val end_date: Long,
    val cycle_length: Int,
    val period_length: Int
)
