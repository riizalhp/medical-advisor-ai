package com.contsol.ayra.data.datasource

import com.contsol.ayra.data.source.local.preference.NewUserPreferences

interface NewUserDataSource {
    fun isFirstLaunch(): Boolean
    fun setFirstLaunch(value: Boolean)
}

class NewUserDataSourceImpl(
    private val preference: NewUserPreferences,
) : NewUserDataSource {
    override fun isFirstLaunch(): Boolean = preference.isFirstLaunch()
    override fun setFirstLaunch(value: Boolean) = preference.setFirstLaunch(value)
}