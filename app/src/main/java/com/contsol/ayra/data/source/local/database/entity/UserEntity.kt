package com.contsol.ayra.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val gender: String,
    val created_at: Long
)
