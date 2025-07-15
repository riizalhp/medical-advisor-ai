package com.contsol.ayra.utils

import java.text.SimpleDateFormat
import java.util.Date

fun convertTimestampToDate(timestamp: Long): String {
    val date = Date(timestamp * 1000) // Convert seconds to milliseconds
    val format = SimpleDateFormat("HH.mm")
    return format.format(date)
}