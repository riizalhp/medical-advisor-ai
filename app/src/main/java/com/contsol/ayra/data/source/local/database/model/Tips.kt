package com.contsol.ayra.data.source.local.database.model

import kotlinx.serialization.Serializable

@Serializable
data class Tips(
    val title: String,
    val content: String
)