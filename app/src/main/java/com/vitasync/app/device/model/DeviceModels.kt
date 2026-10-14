package com.vitasync.app.device.model

import com.vitasync.app.data.local.entity.ActivityEntity
import com.vitasync.app.data.local.entity.SleepEntity

/** Every integration family the app knows how to speak. */
enum class AdapterType { HEALTH_CONNECT, BLE_GATT, PEBBLE }

/** A watch/source found during discovery. */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val adapterType: AdapterType,
    val rssi: Int? = null
)

/** Everything a device handed back during one sync. */
data class SyncBundle(
    val activities: List<ActivityEntity> = emptyList(),
    val sleep: List<SleepEntity> = emptyList()
)
