package com.contsol.ayra.data.source.local.database.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.contsol.ayra.data.source.local.database.entity.PhotoLogEntity

interface PhotoLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoLog: PhotoLogEntity): Long

    @Query("SELECT * FROM PhotoLogEntity")
    suspend fun getPhotoLogs(): PhotoLogEntity?

    @Query("SELECT * FROM PhotoLogEntity WHERE id = :id")
    suspend fun getPhotoLogById(id: Long): PhotoLogEntity?

    @Update
    suspend fun updatePhotoLog(photoLog: PhotoLogEntity)

    @Query("DELETE FROM PhotoLogEntity WHERE id = :id")
    suspend fun deletePhotoLog(id: Long)
}