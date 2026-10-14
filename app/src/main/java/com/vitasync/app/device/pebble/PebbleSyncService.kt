package com.vitasync.app.device.pebble

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vitasync.app.MainActivity
import com.vitasync.app.R
import com.vitasync.app.VitaSyncApp
import com.vitasync.app.data.repository.HealthRepository
import com.vitasync.app.device.model.SyncBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Holds the Pebble RFCOMM link open in the background so the watch's periodic
 * health pushes (roughly every 15 minutes while connected) are captured as they
 * arrive, instead of only during a manual sync.
 *
 * Each decoded record is written to the database immediately; because records
 * carry a stable sourceKey, the Today dashboard's Room flows update live and
 * never double-count.
 */
class PebbleSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var running = false
    private var address: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                running = false
                scope.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                address = intent?.getStringExtra(EXTRA_ADDRESS) ?: address
                createChannel()
                startForeground(NOTIF_ID, buildNotification("Connecting to Pebble..."))
                if (!running) {
                    running = true
                    scope.launch { loop() }
                }
            }
        }
        return START_STICKY
    }

    private suspend fun loop() {
        val repo = (application as VitaSyncApp).healthRepository
        val addr = address ?: return
        while (running && scope.isActive) {
            try {
                pump(addr, repo)
            } catch (_: Exception) {
                // fall through to reconnect
            }
            PebbleLink.connected.value = false
            notify("Pebble disconnected - retrying")
            if (running) delay(RECONNECT_MS)
        }
    }

    private suspend fun pump(addr: String, repo: HealthRepository) {
        if (!hasBtPermission()) return
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return
        val socket = adapter.getRemoteDevice(addr)
            .createRfcommSocketToServiceRecord(PebbleProtocol.SPP_UUID)

        try {
            adapter.cancelDiscovery()
            socket.connect()
            PebbleLink.connected.value = true
            notify("Live - steps & sleep syncing")

            val transport = PebbleTransport(socket)
            val decoder = PebbleHealthDecoder()

            // Identify ourselves, then keep asking the watch to flush its sessions.
            transport.write(PebbleProtocol.ENDPOINT_PHONEVERSION, PebbleProtocol.phoneVersionAndroid())

            var lastRequest = 0L
            var idle = 0
            while (running && scope.isActive) {
                val now = System.currentTimeMillis()
                if (now - lastRequest > REQUEST_INTERVAL_MS) {
                    transport.write(PebbleProtocol.ENDPOINT_DATALOG, PebbleProtocol.reportDataLogSessions())
                    lastRequest = now
                }

                val msg = transport.readMessage(READ_TIMEOUT_MS)
                if (msg == null) {
                    // Nothing for a while: give up and reconnect rather than hang.
                    if (++idle > MAX_IDLE_READS) break
                    continue
                }
                idle = 0

                when (msg.first) {
                    PebbleProtocol.ENDPOINT_PHONEVERSION -> transport.write(
                        PebbleProtocol.ENDPOINT_PHONEVERSION,
                        PebbleProtocol.phoneVersionAndroid()
                    )
                    PebbleProtocol.ENDPOINT_DATALOG -> {
                        handleDatalogMessage(msg.second, transport, decoder)
                        persist(repo, decoder.snapshot())
                    }
                }
            }
        } finally {
            PebbleLink.connected.value = false
            runCatching { socket.close() }
        }
    }

    private suspend fun persist(repo: HealthRepository, bundle: SyncBundle) {
        if (bundle.activities.isEmpty() && bundle.sleep.isEmpty()) return
        bundle.activities.forEach { repo.logActivity(it) }
        bundle.sleep.forEach { repo.logSleep(it) }
        PebbleLink.lastSync.value = System.currentTimeMillis()
        PebbleLink.recordsSynced.value += bundle.activities.size + bundle.sleep.size
    }

    private fun hasBtPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    // ---- notification plumbing ----
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pebble live sync",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PebbleSyncService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VitaSync - Pebble Steel")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    private fun notify(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.vitasync.app.PEBBLE_START"
        const val ACTION_STOP = "com.vitasync.app.PEBBLE_STOP"
        const val EXTRA_ADDRESS = "address"

        private const val CHANNEL_ID = "pebble_live_sync"
        private const val NOTIF_ID = 4101
        private const val READ_TIMEOUT_MS = 4_000
        private const val REQUEST_INTERVAL_MS = 15 * 60 * 1000L  // ask for a flush every 15 min
        private const val MAX_IDLE_READS = 20                    // ~80s of silence -> reconnect
        private const val RECONNECT_MS = 10_000L
    }
}
