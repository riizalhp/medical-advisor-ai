package com.contsol.ayra.data.repository

import com.contsol.ayra.data.datasource.ActivityDataSource

interface ActivityRepository {
    fun getCountMinum(): Int
    fun addCountMinum(value: Int)

    fun getCountMakan(): Int
    fun addCountMakan(value: Int)

    fun getStepsStart(): Float
    fun setStepsStart(value: Float)

    fun isNewDay(): Boolean
    fun resetDailyActivity()
}

class ActivityRepositoryImpl(
    private val dataSource: ActivityDataSource,
) : ActivityRepository {

    override fun getCountMinum(): Int = dataSource.getCountMinum()

    override fun addCountMinum(value: Int) {
        val current = dataSource.getCountMinum()
        dataSource.setCountMinum(current + value)
    }

    override fun getCountMakan(): Int = dataSource.getCountMakan()

    override fun addCountMakan(value: Int) {
        val current = dataSource.getCountMakan()
        dataSource.setCountMakan(current + value)
    }

    override fun getStepsStart(): Float = dataSource.getStepsStart()

    override fun setStepsStart(value: Float) = dataSource.setStepsStart(value)

    override fun isNewDay(): Boolean = dataSource.isNewDay()

    override fun resetDailyActivity() = dataSource.resetAll()
}