package com.contsol.ayra.data.source.local.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromFloatArray(array: FloatArray): String = array.joinToString(",")

    @TypeConverter
    fun toFloatArray(data: String): FloatArray =
        if (data.isEmpty()) FloatArray(0)
        else data.split(",").map { it.toFloat() }.toFloatArray()
}