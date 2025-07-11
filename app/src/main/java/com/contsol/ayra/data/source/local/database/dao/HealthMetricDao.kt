package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contsol.ayra.data.source.local.database.entity.HealthMetricEntity

@Dao
interface HealthMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(healthMetric: HealthMetricEntity): Long

    @Query("SELECT * FROM HealthMetricEntity")
    suspend fun getMetrics(): List<HealthMetricEntity>?

    @Query("SELECT * FROM HealthMetricEntity WHERE timestamp = :date")
    suspend fun getMetricForDate(date: Long): HealthMetricEntity?

    @Query("SELECT * FROM HealthMetricEntity WHERE timestamp >= :startOfToday AND timestamp < :startOfTomorrow")
    suspend fun getTodayMetric(startOfToday: Long, startOfTomorrow: Long): HealthMetricEntity?

    @Update
    suspend fun updateMetric(healthMetric: HealthMetricEntity)
}