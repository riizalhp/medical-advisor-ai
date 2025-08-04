package com.contsol.ayra.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

fun convertTimestampToDate(timestamp: Long): String {
    val date = Date(timestamp * 1000) // Convert seconds to milliseconds
    val format = SimpleDateFormat("HH.mm")
    return format.format(date)
}

fun getEndOfTheDayTimestamp(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}