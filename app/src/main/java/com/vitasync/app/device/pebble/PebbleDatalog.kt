package com.vitasync.app.device.pebble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Shared DataLogging message handler used by both the one-shot sync
 * ([PebbleAdapter]) and the live foreground service ([PebbleSyncService]).
 */
internal fun handleDatalogMessage(
    payload: ByteArray,
    transport: PebbleTransport,
    decoder: PebbleHealthDecoder
) {
    if (payload.size < 2) return
    val command = payload[0]
    val id = payload[1].toInt() and 0xFF
    when (command) {
        PebbleProtocol.DATALOG_OPENSESSION -> {
            if (payload.size >= 29) {
                // [cmd][id][uuid 16][timestamp 4 LE][tag 4 LE][itemType 1][itemSize 2 LE]
                val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
                buf.position(18)
                buf.int                                  // session timestamp
                val tag = buf.int
                buf.get()                                // item type
                val itemSize = buf.short.toInt() and 0xFFFF
                decoder.openSession(id, tag, itemSize)
            }
            transport.write(
                PebbleProtocol.ENDPOINT_DATALOG,
                PebbleProtocol.datalogReply(id.toByte(), PebbleProtocol.DATALOG_ACK)
            )
        }
        PebbleProtocol.DATALOG_SENDDATA -> {
            val ok = if (payload.size > 10) {
                decoder.handleData(id, payload.copyOfRange(10, payload.size))
            } else true
            transport.write(
                PebbleProtocol.ENDPOINT_DATALOG,
                PebbleProtocol.datalogReply(
                    id.toByte(),
                    if (ok) PebbleProtocol.DATALOG_ACK else PebbleProtocol.DATALOG_NACK
                )
            )
        }
        PebbleProtocol.DATALOG_CLOSE -> {
            decoder.closeSession(id)
            transport.write(
                PebbleProtocol.ENDPOINT_DATALOG,
                PebbleProtocol.datalogReply(id.toByte(), PebbleProtocol.DATALOG_ACK)
            )
        }
        // DATALOG_TIMEOUT and anything else: ignore.
    }
}
