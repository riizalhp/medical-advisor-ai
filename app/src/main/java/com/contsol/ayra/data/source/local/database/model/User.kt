package com.contsol.ayra.data.source.local.database.model

data class User(
    val id: Long = 0L,
    val name: String,
    val gender: String,
    val createdAt: Long =  System.currentTimeMillis(),
)
