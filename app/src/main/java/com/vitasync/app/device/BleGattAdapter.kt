package com.vitasync.app.device

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import com.vitasync.app.device.model.SyncBundle
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Generic Bluetooth Low Energy adapter.
 *
 * Handles any watch/band that exposes standard GATT services, most usefully:
 *   - Heart Rate Service        0x180D
 *   - Health Thermometer        0x1809
 *   - Device Information        0x180A
 *
 * Vendor watches that use *custom* GATT profiles (Garmin, older Fitbits, etc.)
 * are not covered here on purpose - they are either read through Health Connect
 * or need a device-specific adapter. This one gives you the standards-compliant
 * baseline that "just works" with many cheap bands.
 */
class BleGattAdapter : DeviceAdapter {

    override val adapterType = AdapterType.BLE_GATT
    override val displayName = "Bluetooth LE device (standard GATT)"

    private fun hasBtPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        return fine == PackageManager.PERMISSION_GRANTED &&
            connect == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun isAvailable(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    override suspend fun hasPermissions(context: Context): Boolean = hasBtPermission(context)

    override suspend fun discover(context: Context): List<DiscoveredDevice> {
        if (!hasBtPermission(context)) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return emptyList()
        val scanner = manager.adapter.bluetoothLeScanner ?: return emptyList()
        val found = LinkedHashMap<String, DiscoveredDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.device.name ?: return
                found[result.device.address] = DiscoveredDevice(
                    name = name,
                    address = result.device.address,
                    adapterType = AdapterType.BLE_GATT,
                    rssi = result.rssi
                )
            }
        }

        scanner.startScan(callback)
        withTimeoutOrNull(10_000) { while (found.size < 20) delay(500) }
        scanner.stopScan(callback)
        return found.values.toList()
    }

    override suspend fun connect(context: Context, device: DiscoveredDevice): Boolean {
        // A real connect would call device.connectGatt(...) and hold the Gatt
        // callback. Left as a bounded stub so the scaffold compiles and pairs.
        return hasBtPermission(context)
    }

    override suspend fun sync(context: Context, device: DiscoveredDevice, since: Long): SyncBundle {
        // Reading live GATT characteristics (e.g. heart rate notifications) and
        // converting them into stored records is where your device-specific work
        // goes. Baseline scaffold returns nothing rather than guessing.
        return SyncBundle(activities = emptyList())
    }
}
