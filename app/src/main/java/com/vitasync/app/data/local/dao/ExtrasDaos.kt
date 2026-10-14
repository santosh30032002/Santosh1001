package com.vitasync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vitasync.app.data.local.entity.FastingEntity
import com.vitasync.app.data.local.entity.ViceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    @Insert
    suspend fun insert(fasting: FastingEntity): Long

    @Query("UPDATE fasting SET endTime = :end WHERE id = :id")
    suspend fun finish(id: Long, end: Long)

    @Query("SELECT * FROM fasting WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<FastingEntity?>

    @Query("SELECT * FROM fasting ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FastingEntity>>

    @Query("SELECT * FROM fasting WHERE startTime >= :from AND startTime < :to ORDER BY startTime DESC")
    fun observeRange(from: Long, to: Long): Flow<List<FastingEntity>>
}

@Dao
interface ViceDao {
    @Insert
    suspend fun insert(vice: ViceEntity): Long

    @Query("SELECT * FROM vices ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ViceEntity>>

    @Query("SELECT * FROM vices WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeRange(from: Long, to: Long): Flow<List<ViceEntity>>
}
