package com.vitasync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A logged cigarette or drink. [calories] is only meaningful for alcohol. */
@Entity(tableName = "vices")
data class ViceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val quantity: Int,
    val unitLabel: String,
    val detail: String = "",
    val calories: Double = 0.0
)
