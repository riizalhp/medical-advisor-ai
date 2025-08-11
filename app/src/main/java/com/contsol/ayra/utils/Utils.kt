package com.contsol.ayra.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun convertTimestampToDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("HH.mm", Locale.getDefault())
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

fun getGreeting(): String {
    val calendar = Calendar.getInstance()
    val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

    return when (hourOfDay) {
        in 0..11 -> "Selamat Pagi"
        in 12..16 -> "Selamat Siang"
        in 17..20 -> "Selamat Sore"
        else -> "Selamat Malam"
    }
}