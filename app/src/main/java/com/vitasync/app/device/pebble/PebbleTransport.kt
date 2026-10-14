package com.vitasync.app.device.pebble

import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream

/**
 * Blocking framing transport over a connected RFCOMM [BluetoothSocket].
 * Run all calls on a background thread (see [PebbleAdapter], which uses Dispatchers.IO).
 */
class PebbleTransport(socket: BluetoothSocket) {

    private val input: InputStream = socket.inputStream
    private val output: OutputStream = socket.outputStream

    fun write(endpoint: Int, payload: ByteArray) {
        output.write(PebbleProtocol.frame(endpoint, payload))
        output.flush()
    }

    /**
     * Read one framed message, waiting up to [timeoutMs] for it to start.
     * Returns (endpoint, payload), or null if nothing arrived in time / stream ended.
     */
    fun readMessage(timeoutMs: Int): Pair<Int, ByteArray>? {
        val deadline = System.currentTimeMillis() + timeoutMs
        // Wait for the 4-byte header to become available.
        while (input.available() < 4) {
            if (System.currentTimeMillis() > deadline) return null
            Thread.sleep(15)
        }
        val header = readFully(4) ?: return null
        val length = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
        val endpoint = ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
        if (length < 0 || length > 8192) return null
        val payload = if (length > 0) readFully(length) ?: return null else ByteArray(0)
        return endpoint to payload
    }

    private fun readFully(n: Int): ByteArray? {
        val out = ByteArray(n)
        var off = 0
        while (off < n) {
            val r = input.read(out, off, n - off)
            if (r < 0) return null
            off += r
        }
        return out
    }
}
