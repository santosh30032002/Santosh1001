package com.vitasync.app.device.pebble

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-wide observable state of the live Pebble link, so the UI can show
 * "connected / last synced" without talking to the service directly.
 */
object PebbleLink {
    val connected = MutableStateFlow(false)
    val lastSync = MutableStateFlow(0L)
    val recordsSynced = MutableStateFlow(0)
}
