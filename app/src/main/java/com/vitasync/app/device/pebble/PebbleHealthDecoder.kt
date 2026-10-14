package com.vitasync.app.device.pebble

import com.vitasync.app.data.local.entity.ActivityEntity
import com.vitasync.app.data.local.entity.SleepEntity
import com.vitasync.app.data.model.ActivityType
import com.vitasync.app.device.model.SyncBundle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId

/**
 * Decodes the Pebble Health DataLogging blobs into our entities.
 *
 * Layouts are the community-reverse-engineered ones from the Gadgetbridge
 * project. All little-endian.
 *
 *   tag 81 (steps)   itemSize 99  - 15 minutes of per-minute step counts
 *   tag 83 (sleep)   itemSize 18  - one night: bed start/end + deep seconds
 *   tag 84 (overlay) itemSize 26  - sleep stage segments
 *
 * A single decoder instance covers one connection. [snapshot] returns the
 * *cumulative* totals decoded so far without clearing them, so a long-lived
 * connection can stream updates to the database: because each day's steps are
 * written under a stable [ActivityEntity.sourceKey], re-writing simply replaces
 * the running total rather than adding to it.
 */
class PebbleHealthDecoder {

    private data class Session(val id: Int, val tag: Int, val itemSize: Int)

    private val sessions = HashMap<Int, Session>()
    private val stepSamples = mutableListOf<Pair<Long, Int>>()   // (epochSeconds, steps)
    private val sleepRecords = mutableListOf<SleepEntity>()
    private var overlayDeepSeconds = 0

    fun openSession(id: Int, tag: Int, itemSize: Int) {
        sessions[id] = Session(id, tag, itemSize)
    }

    fun closeSession(id: Int) {
        sessions.remove(id)
    }

    /** Returns true if the message was understood (so we ACK rather than NACK). */
    fun handleData(id: Int, data: ByteArray): Boolean {
        val session = sessions[id] ?: return false
        if (session.itemSize <= 0) return false
        return when (session.tag) {
            PebbleProtocol.TAG_HEALTH_STEPS -> decodeSteps(data, session.itemSize)
            PebbleProtocol.TAG_HEALTH_SLEEP -> decodeSleep(data, session.itemSize)
            PebbleProtocol.TAG_HEALTH_OVERLAY -> decodeOverlay(data, session.itemSize)
            else -> true // ack + ignore sessions we don't care about (analytics, HR, ...)
        }
    }

    // ---- tag 81: steps ----
    private fun decodeSteps(data: ByteArray, itemSize: Int): Boolean {
        if (data.size % itemSize != 0) return false
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val packetCount = data.size / itemSize
        for (p in 0 until packetCount) {
            buf.position(p * itemSize)
            val version = buf.short.toInt()
            if (version !in intArrayOf(5, 6, 7, 12, 13)) return false
            var timestamp = buf.int.toLong()
            buf.get()                       // unknown
            val recordLength = buf.get().toInt() and 0xFF
            val recordNum = buf.get().toInt() and 0xFF
            val recordsStart = buf.position()
            for (r in 0 until recordNum) {
                val pos = recordsStart + r * recordLength
                if (pos + 5 > data.size) break
                val steps = data[pos].toInt() and 0xFF
                stepSamples += timestamp to steps
                timestamp += 60
            }
        }
        return true
    }

    // ---- tag 83: sleep ----
    private fun decodeSleep(data: ByteArray, itemSize: Int): Boolean {
        if (data.size % itemSize != 0) return false
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val count = data.size / itemSize
        for (i in 0 until count) {
            buf.position(i * itemSize)
            buf.short                       // version
            buf.int                         // offset to UTC (seconds)
            val bedStart = buf.int.toLong() // epoch seconds
            val bedEnd = buf.int.toLong()
            val deepSeconds = buf.int
            if (bedEnd <= bedStart) continue
            val durationMin = ((bedEnd - bedStart) / 60).toInt()
            sleepRecords += SleepEntity(
                startTime = bedStart * 1000L,
                endTime = bedEnd * 1000L,
                durationMinutes = durationMin,
                deepMinutes = (deepSeconds / 60).coerceAtMost(durationMin),
                sourceName = "Pebble Steel",
                sourceKey = "pebble:sleep:$bedStart"
            )
        }
        return true
    }

    // ---- tag 84: sleep overlay (deep/light segments) ----
    private fun decodeOverlay(data: ByteArray, itemSize: Int): Boolean {
        if (data.size % itemSize != 0) return false
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val count = data.size / itemSize
        for (i in 0 until count) {
            buf.position(i * itemSize)
            buf.short                       // version
            buf.short                       // unknown
            val type = buf.short.toInt()    // 1 = sleep, 2 = deep sleep
            buf.int                         // offsetUTC
            buf.int                         // timestampStart
            val durationSeconds = buf.int
            if (type == 2) overlayDeepSeconds += durationSeconds
        }
        return true
    }

    /**
     * Cumulative view of everything decoded so far, grouped by day for steps.
     * Does NOT clear internal buffers - safe to call repeatedly on a live link.
     */
    fun snapshot(): SyncBundle {
        val activities = mutableListOf<ActivityEntity>()
        if (stepSamples.isNotEmpty()) {
            stepSamples.groupBy { dayKey(it.first) }.forEach { (date, samples) ->
                val totalSteps = samples.sumOf { it.second }
                if (totalSteps <= 0) return@forEach
                val first = samples.minOf { it.first }
                val last = samples.maxOf { it.first }
                val minutes = ((last - first) / 60.0).coerceAtLeast(1.0)
                val kcal = ActivityType.WALKING.met * 3.5 * 70.0 / 200.0 * minutes
                activities += ActivityEntity(
                    type = ActivityType.WALKING.label,
                    startTime = first * 1000L,
                    endTime = (last + 60) * 1000L,
                    durationSeconds = (minutes * 60).toLong(),
                    steps = totalSteps,
                    calories = kcal,
                    sourceName = "Pebble Steel",
                    sourceKey = "pebble:steps:$date"
                )
            }
        }
        return SyncBundle(activities = activities, sleep = sleepRecords.toList())
    }

    /** Alias kept for the one-shot adapter path. */
    fun toBundle(): SyncBundle = snapshot()

    private fun dayKey(epochSeconds: Long): String =
        Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}
