package com.contsol.ayra.data.source.local.preference

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

private const val PREFS_NAME = "HealthTipsPrefs"
private const val KEY_LAST_REFRESH_DATE = "last_refresh_date"

object TipsRefreshPreferences {

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Checks if tips should be refreshed today.
     * This is true if they haven't been refreshed yet today.
     */
    fun shouldRefreshTips(context: Context): Boolean {
        val prefs = getPreferences(context)
        val lastRefreshDate = prefs.getString(KEY_LAST_REFRESH_DATE, null)
        val todayDate = getTodayDateString()
        return lastRefreshDate != todayDate
    }

    /**
     * Marks tips as refreshed for today.
     */
    fun markTipsRefreshedToday(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit { putString(KEY_LAST_REFRESH_DATE, getTodayDateString()) }
    }
}