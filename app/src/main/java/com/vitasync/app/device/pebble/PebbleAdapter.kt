package com.vitasync.app.device.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.vitasync.app.device.DeviceAdapter
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import com.vitasync.app.device.model.SyncBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pebble Steel (and Pebble Time / 2) over Bluetooth Classic.
 *
 * This class performs a one-shot sync. For a persistent live link use
 * [PebbleSyncService], which keeps the socket open and streams updates.
 */
class PebbleAdapter : DeviceAdapter {

    override val adapterType = AdapterType.PEBBLE
    override val displayName = "Pebble (Steel, Time, 2 - classic Bluetooth)"

    private fun btAdapter(context: Context): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun hasPermission(context: Context): Boolean {
        val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        return connect == PackageManager.PERMISSION_GRANTED &&
            scan == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun isAvailable(context: Context): Boolean =
        btAdapter(context)?.isEnabled == true

    override suspend fun hasPermissions(context: Context): Boolean = hasPermission(context)

    override suspend fun discover(context: Context): List<DiscoveredDevice> {
        if (!hasPermission(context)) return emptyList()
        val adapter = btAdapter(context) ?: return emptyList()
        return adapter.bondedDevices
            .filter { it.name?.contains("Pebble", ignoreCase = true) == true }
            .map { DiscoveredDevice(it.name ?: "Pebble", it.address, AdapterType.PEBBLE) }
    }

    override suspend fun connect(context: Context, device: DiscoveredDevice): Boolean =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext false
            val adapter = btAdapter(context) ?: return@withContext false
            val socket = runCatching {
                adapter.cancelDiscovery()
                adapter.getRemoteDevice(device.address)
                    .createRfcommSocketToServiceRecord(PebbleProtocol.SPP_UUID)
            }.getOrNull() ?: return@withContext false
            runCatching { socket.connect() }
                .onSuccess { runCatching { socket.close() } }
                .isSuccess
        }

    override suspend fun sync(
        context: Context,
        device: DiscoveredDevice,
        since: Long
    ): SyncBundle = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext SyncBundle()
        val adapter = btAdapter(context) ?: return@withContext SyncBundle()

        val socket = runCatching {
            adapter.cancelDiscovery()
            adapter.getRemoteDevice(device.address)
                .createRfcommSocketToServiceRecord(PebbleProtocol.SPP_UUID)
        }.getOrNull() ?: return@withContext SyncBundle()

        try {
            socket.connect()
            val transport = PebbleTransport(socket)
            val decoder = PebbleHealthDecoder()

            transport.write(
                PebbleProtocol.ENDPOINT_PHONEVERSION,
                PebbleProtocol.phoneVersionAndroid()
            )

            val deadline = System.currentTimeMillis() + SYNC_WINDOW_MS
            var requestedSessions = false
            while (System.currentTimeMillis() < deadline) {
                val msg = transport.readMessage(READ_TIMEOUT_MS) ?: break
                when (msg.first) {
                    PebbleProtocol.ENDPOINT_PHONEVERSION -> transport.write(
                        PebbleProtocol.ENDPOINT_PHONEVERSION,
                        PebbleProtocol.phoneVersionAndroid()
                    )
                    PebbleProtocol.ENDPOINT_FIRMWAREVERSION -> if (!requestedSessions) {
                        transport.write(
                            PebbleProtocol.ENDPOINT_DATALOG,
                            PebbleProtocol.reportDataLogSessions()
                        )
                        requestedSessions = true
                    }
                    PebbleProtocol.ENDPOINT_DATALOG ->
                        handleDatalogMessage(msg.second, transport, decoder)
                }
            }
            decoder.snapshot()
        } catch (e: Exception) {
            SyncBundle()
        } finally {
            runCatching { socket.close() }
        }
    }

    private companion object {
        const val SYNC_WINDOW_MS = 25_000L
        const val READ_TIMEOUT_MS = 4_000
    }
}
