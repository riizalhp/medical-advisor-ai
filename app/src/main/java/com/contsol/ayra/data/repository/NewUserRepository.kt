package com.contsol.ayra.data.repository

import com.contsol.ayra.data.datasource.NewUserDataSource

interface NewUserRepository {
    fun isFirstLaunch(): Boolean
    fun setFirstLaunch(value: Boolean)
}

class NewUserRepositoryImpl(
    private val dataSource: NewUserDataSource,
) : NewUserRepository {
    override fun isFirstLaunch(): Boolean = dataSource.isFirstLaunch()
    override fun setFirstLaunch(value: Boolean) = dataSource.setFirstLaunch(value)
}