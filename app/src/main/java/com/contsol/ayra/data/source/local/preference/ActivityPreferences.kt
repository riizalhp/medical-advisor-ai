package com.contsol.ayra.data.source.local.preference

import android.content.Context
import android.content.SharedPreferences
import com.contsol.ayra.utils.SharedPreferenceUtils.set
import com.contsol.ayra.utils.SharedPreferenceUtils.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ActivityPreference {
    fun getCountMinum(): Int
    fun setCountMinum(value: Int)

    fun getCountMakan(): Int
    fun setCountMakan(value: Int)

    fun getStepsStart(): Float
    fun setStepsStart(value: Float)

    fun getLastDate(): String
    fun setLastDate(date: String)

    fun isNewDay(): Boolean
    fun resetAll()
}

class ActivityPreferenceImpl(private val prefs: SharedPreferences) : ActivityPreference {

    override fun getCountMinum(): Int = prefs[KEY_COUNT_MINUM, 0]
    override fun setCountMinum(value: Int) {
        prefs[KEY_COUNT_MINUM] = value
    }

    override fun getCountMakan(): Int = prefs[KEY_COUNT_MAKAN, 0]
    override fun setCountMakan(value: Int) {
        prefs[KEY_COUNT_MAKAN] = value
    }

    override fun getStepsStart(): Float = prefs[KEY_STEPS_START, 0f]
    override fun setStepsStart(value: Float) {
        prefs[KEY_STEPS_START] = value
    }

    override fun getLastDate(): String = prefs[KEY_LAST_DATE, ""]
    override fun setLastDate(date: String) {
        prefs[KEY_LAST_DATE] = date
    }

    override fun isNewDay(): Boolean {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return getLastDate() != today
    }

    override fun resetAll() {
        prefs[KEY_COUNT_MINUM] = 0
        prefs[KEY_COUNT_MAKAN] = 0
        prefs[KEY_STEPS_START] = 0f
        prefs[KEY_LAST_DATE] = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    companion object {
        const val PREF_NAME = "activity_tracker"
        private const val KEY_COUNT_MINUM = "countMinum"
        private const val KEY_COUNT_MAKAN = "countMakan"
        private const val KEY_STEPS_START = "stepsStart"
        private const val KEY_LAST_DATE = "lastDate"
    }
}