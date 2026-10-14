package com.vitasync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One fasting session. [endTime] is null while the fast is running. */
@Entity(tableName = "fasting")
data class FastingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val targetHours: Int,
    val protocol: String,
    val note: String = ""
)
