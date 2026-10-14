package com.vitasync.app.device

import android.content.Context
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import com.vitasync.app.device.model.SyncBundle

/**
 * THE central abstraction of this app.
 *
 * Every smart watch - Pebble, a Garmin, a generic BLE band, or anything that
 * feeds Android Health Connect - is supported by implementing this one
 * interface. The UI, the repository and the database know nothing about any
 * specific watch. Adding support for a new device = write one class that
 * implements this, then register it in [DeviceAdapterRegistry].
 */
interface DeviceAdapter {

    val adapterType: AdapterType

    /** Shown in the "Add device" list, e.g. "Pebble (classic Bluetooth)". */
    val displayName: String

    /** Is this integration usable on this phone right now (API level, app installed)? */
    suspend fun isAvailable(context: Context): Boolean

    /** Has the user granted the runtime permissions this adapter needs? */
    suspend fun hasPermissions(context: Context): Boolean

    /** Find pairable devices. May be empty for cloud/bridge sources. */
    suspend fun discover(context: Context): List<DiscoveredDevice>

    /** Establish a link and remember the device. */
    suspend fun connect(context: Context, device: DiscoveredDevice): Boolean

    /** Pull records newer than [since] (epoch millis). */
    suspend fun sync(context: Context, device: DiscoveredDevice, since: Long): SyncBundle
}
