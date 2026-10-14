package com.vitasync.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [sourceKey] gives a record a stable identity so repeated syncs from the same
 * watch REPLACE rather than accumulate (see the unique index below). It is null
 * for manually-entered rows, and SQLite treats NULLs as distinct, so manual
 * entries never collide with each other.
 */
@Entity(tableName = "activities", indices = [Index(value = ["sourceKey"], unique = true)])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,
    val avgHeartRate: Int = 0,
    val sourceName: String = "Manual",
    val sourceKey: String? = null
)
