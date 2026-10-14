package com.vitasync.app.device.pebble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Pebble protocol constants and framing.
 *
 * Values here are taken from Gadgetbridge's `PebbleProtocol.java` (the reference
 * implementation for talking to a Pebble) so they match what a real watch expects.
 *
 * Transport: Bluetooth *Classic* RFCOMM over the SPP UUID. Messages are framed
 * as [length (2 bytes, big-endian)][endpoint (2 bytes, big-endian)][payload].
 * (The `0xFEED`/`0xBEEF` wrapper you may see elsewhere is only used by
 * Gadgetbridge's optional TCP bridge, not over Bluetooth.)
 */
object PebbleProtocol {

    /** Bluetooth Classic serial-port service (SPP) used by every Pebble. */
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    // ---- Endpoints (subset we use) ----
    const val ENDPOINT_FIRMWAREVERSION = 16
    const val ENDPOINT_PHONEVERSION = 17
    const val ENDPOINT_SYSTEMMESSAGE = 18
    /** DataLogging endpoint - carries Pebble Health steps/sleep blobs. */
    const val ENDPOINT_DATALOG = 6778

    // ---- DataLogging commands (first payload byte) ----
    const val DATALOG_OPENSESSION: Byte = 0x01
    const val DATALOG_SENDDATA: Byte = 0x02
    const val DATALOG_CLOSE: Byte = 0x03
    const val DATALOG_TIMEOUT: Byte = 0x07
    const val DATALOG_REPORTSESSIONS: Byte = 0x84.toByte()
    const val DATALOG_ACK: Byte = 0x85.toByte()
    const val DATALOG_NACK: Byte = 0x86.toByte()

    // ---- Pebble Health session tags (all from system UUID 0) ----
    const val TAG_HEALTH_STEPS = 81
    const val TAG_HEALTH_SLEEP = 83
    const val TAG_HEALTH_OVERLAY = 84
    const val TAG_HEALTH_HR = 85

    private const val PHONEVERSION_APPVERSION_MAGIC: Byte = 2
    private const val PHONEVERSION_REMOTE_OS_ANDROID: Byte = 2

    /** Wrap a payload for [endpoint] into a length-prefixed Pebble message. */
    fun frame(endpoint: Int, payload: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(4 + payload.size)
        buf.order(ByteOrder.BIG_ENDIAN)
        buf.putShort(payload.size.toShort())     // length = payload length only
        buf.putShort(endpoint.toShort())
        buf.put(payload)
        return buf.array()
    }

    /**
     * The "phone version" handshake message (3.x layout, 25-byte payload).
     * The watch asks for this on connect and will not proceed without it.
     */
    fun phoneVersionAndroid(): ByteArray {
        val buf = ByteBuffer.allocate(25)
        buf.order(ByteOrder.BIG_ENDIAN)
        buf.put(0x01.toByte())
        buf.putInt(-1)                          // 0xffffffff
        buf.putInt(0)
        buf.putInt(PHONEVERSION_REMOTE_OS_ANDROID.toInt())
        buf.put(PHONEVERSION_APPVERSION_MAGIC)
        buf.put(4)                              // major
        buf.put(1)                              // minor
        buf.put(1)                              // patch
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putLong(0x29afL)                    // capability flags
        return buf.array()
    }

    /** Ask the watch to (re)send all its open DataLogging sessions. */
    fun reportDataLogSessions(): ByteArray = byteArrayOf(DATALOG_REPORTSESSIONS)

    /** ACK/NACK a datalog message by its session handle. */
    fun datalogReply(handle: Byte, reply: Byte): ByteArray = byteArrayOf(reply, handle)
}
