package com.vitasync.app.device

import android.content.Context
import com.vitasync.app.device.model.AdapterType

/**
 * Central list of active adapters. The app registers its built-in adapters on
 * startup; a future plugin (or a white-label build) can register more without
 * touching anything else.
 */
object DeviceAdapterRegistry {

    private val adapters = mutableListOf<DeviceAdapter>()

    fun register(adapter: DeviceAdapter) {
        if (adapters.none { it.adapterType == adapter.adapterType }) adapters += adapter
    }

    fun all(): List<DeviceAdapter> = adapters.toList()

    fun forType(type: AdapterType): DeviceAdapter? =
        adapters.firstOrNull { it.adapterType == type }

    /** Called once from Application.onCreate(). */
    fun init() {
        if (adapters.isNotEmpty()) return
        register(HealthConnectAdapter())
        register(BleGattAdapter())
        register(PebbleAdapter())
    }
}
