package com.vitasync.app.data.repository

import android.content.Context
import com.vitasync.app.data.local.dao.DeviceDao
import com.vitasync.app.data.local.entity.DeviceEntity
import com.vitasync.app.device.DeviceAdapterRegistry
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import kotlinx.coroutines.flow.Flow

/**
 * Bridges the UI and the pluggable watch adapters. To support a new watch you
 * add an adapter to [DeviceAdapterRegistry]; this class does not change.
 */
class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val healthRepository: HealthRepository
) {
    fun observeDevices(): Flow<List<DeviceEntity>> = deviceDao.observeAll()

    suspend fun discover(context: Context): List<DiscoveredDevice> {
        val results = mutableListOf<DiscoveredDevice>()
        for (adapter in DeviceAdapterRegistry.all()) {
            if (adapter.isAvailable(context)) {
                results += runCatching { adapter.discover(context) }.getOrDefault(emptyList())
            }
        }
        return results
    }

    suspend fun pair(context: Context, device: DiscoveredDevice): Boolean {
        val adapter = DeviceAdapterRegistry.forType(device.adapterType) ?: return false
        val ok = runCatching { adapter.connect(context, device) }.getOrDefault(false)
        if (ok) {
            val existing = deviceDao.findByAddress(device.address)
            if (existing == null) {
                deviceDao.insert(
                    DeviceEntity(
                        name = device.name,
                        address = device.address,
                        adapterType = device.adapterType.name,
                        lastSync = System.currentTimeMillis(),
                        connected = true
                    )
                )
            } else {
                deviceDao.updateSync(existing.id, System.currentTimeMillis(), true)
            }
        }
        return ok
    }

    /**
     * Pull everything new from a paired device and persist it locally.
     * Returns a human-readable summary.
     */
    suspend fun sync(context: Context, entity: DeviceEntity): String {
        val adapter = DeviceAdapterRegistry.forType(AdapterType.valueOf(entity.adapterType))
            ?: return "No adapter for ${entity.adapterType}"
        val device = DiscoveredDevice(entity.name, entity.address, adapter.adapterType)
        val bundle = runCatching { adapter.sync(context, device, entity.lastSync) }
            .getOrElse { return "Sync failed: ${it.message}" }

        bundle.activities.forEach { healthRepository.logActivity(it) }
        bundle.sleep.forEach { healthRepository.logSleep(it) }

        deviceDao.updateSync(entity.id, System.currentTimeMillis(), true)
        return "Synced ${bundle.activities.size} activities, ${bundle.sleep.size} sleep records"
    }
}
