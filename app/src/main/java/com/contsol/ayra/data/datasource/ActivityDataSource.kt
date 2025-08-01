package com.contsol.ayra.data.datasource

import com.contsol.ayra.data.source.local.preference.ActivityPreference

interface ActivityDataSource {
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

class ActivityDataSourceImpl(
    private val preference: ActivityPreference,
) : ActivityDataSource {
    override fun getCountMinum(): Int = preference.getCountMinum()
    override fun setCountMinum(value: Int) = preference.setCountMinum(value)

    override fun getCountMakan(): Int = preference.getCountMakan()
    override fun setCountMakan(value: Int) = preference.setCountMakan(value)

    override fun getStepsStart(): Float = preference.getStepsStart()
    override fun setStepsStart(value: Float) = preference.setStepsStart(value)

    override fun getLastDate(): String = preference.getLastDate()
    override fun setLastDate(date: String) = preference.setLastDate(date)

    override fun isNewDay(): Boolean = preference.isNewDay()
    override fun resetAll() = preference.resetAll()

}