package com.contsol.ayra.data.source.local.database.model

data class User(
    val id: Long = 0L,
    val name: String? = "User",
    val age: Int? = null,
    val gender: String? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val bloodType: String? = null,
    val createdAt: Long =  System.currentTimeMillis(),
)
