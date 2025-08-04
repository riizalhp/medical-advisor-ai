package com.contsol.ayra.data.source.local.preference

import android.content.Context
import android.content.SharedPreferences
import com.contsol.ayra.utils.SharedPreferenceUtils.get
import com.contsol.ayra.utils.SharedPreferenceUtils.set


interface NewUserPreferences {
    fun isFirstLaunch(): Boolean
    fun setFirstLaunch(value: Boolean)
}

class NewUserPreferencesImpl(private val prefs: SharedPreferences) : NewUserPreferences {
    override fun isFirstLaunch(): Boolean = prefs[KEY_FIRST_LAUNCH, true]
    override fun setFirstLaunch(value: Boolean) {
        prefs[KEY_FIRST_LAUNCH] = value
    }

    private companion object {
        const val KEY_FIRST_LAUNCH = "firstLaunch"
    }
}