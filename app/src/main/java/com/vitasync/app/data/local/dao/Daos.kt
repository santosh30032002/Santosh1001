package com.vitasync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vitasync.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    /** REPLACE so a re-sync of the same [sourceKey] updates in place. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE startTime >= :from AND startTime < :to ORDER BY startTime DESC")
    fun observeRange(from: Long, to: Long): Flow<List<ActivityEntity>>

    @Query("SELECT COALESCE(SUM(steps),0) FROM activities WHERE startTime >= :from AND startTime < :to")
    fun observeSteps(from: Long, to: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(calories),0) FROM activities WHERE startTime >= :from AND startTime < :to")
    fun observeCalories(from: Long, to: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(distanceMeters),0) FROM activities WHERE startTime >= :from AND startTime < :to")
    fun observeDistance(from: Long, to: Long): Flow<Double>
}

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleep: SleepEntity): Long

    @Query("SELECT * FROM sleep ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SleepEntity>>

    @Query("SELECT * FROM sleep WHERE startTime >= :from AND startTime < :to ORDER BY startTime DESC")
    fun observeRange(from: Long, to: Long): Flow<List<SleepEntity>>
}

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(water: WaterEntity): Long

    @Query("SELECT * FROM water WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeRange(from: Long, to: Long): Flow<List<WaterEntity>>

    @Query("SELECT COALESCE(SUM(milliliters),0) FROM water WHERE timestamp >= :from AND timestamp < :to")
    fun observeTotal(from: Long, to: Long): Flow<Int>
}

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Query("SELECT * FROM food WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeRange(from: Long, to: Long): Flow<List<FoodEntity>>

    @Query("SELECT COALESCE(SUM(calories),0) FROM food WHERE timestamp >= :from AND timestamp < :to")
    fun observeCalories(from: Long, to: Long): Flow<Double>
}

@Dao
interface DeviceDao {
    @Insert
    suspend fun insert(device: DeviceEntity): Long

    @Query("UPDATE devices SET lastSync = :time, connected = :connected WHERE id = :id")
    suspend fun updateSync(id: Long, time: Long, connected: Boolean)

    @Query("SELECT * FROM devices ORDER BY lastSync DESC")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE address = :address LIMIT 1")
    suspend fun findByAddress(address: String): DeviceEntity?
}
