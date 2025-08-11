package com.contsol.ayra.data.source.local.database.model

data class User(
    val id: Long = 0L,
    val name: String,
    val age: Int,
    val gender: String,
    val weight: Double,
    val height: Double,
    val bloodType: String,
    val createdAt: Long =  System.currentTimeMillis(),
)
