package com.vitasync.app.device

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.vitasync.app.data.local.entity.ActivityEntity
import com.vitasync.app.data.local.entity.SleepEntity
import com.vitasync.app.data.model.ActivityType
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import com.vitasync.app.device.model.SyncBundle
import java.time.Instant

/**
 * Reads from Android Health Connect - the OS-level health data hub.
 *
 * This is the practical answer to "one app, any watch": most modern watches
 * (Samsung, Fitbit, Garmin, Wear OS, Xiaomi/Amazfit via their apps, Google Fit)
 * write their data into Health Connect. By reading Health Connect, this app
 * picks up data from ALL of them through a single integration.
 *
 * A watch that does not publish to Health Connect is handled by a dedicated
 * adapter (see [BleGattAdapter], [PebbleAdapter]).
 */
class HealthConnectAdapter : DeviceAdapter {

    override val adapterType = AdapterType.HEALTH_CONNECT
    override val displayName = "Health Connect (Samsung, Fitbit, Garmin, Wear OS, ...)"

    override suspend fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override suspend fun hasPermissions(context: Context): Boolean = runCatching {
        val client = HealthConnectClient.getOrCreate(context)
        client.permissionController.getGrantedPermissions().containsAll(requiredPermissions())
    }.getOrDefault(false)

    override suspend fun discover(context: Context): List<DiscoveredDevice> =
        listOf(DiscoveredDevice("Health Connect", "health-connect", AdapterType.HEALTH_CONNECT))

    override suspend fun connect(context: Context, device: DiscoveredDevice): Boolean =
        hasPermissions(context)

    override suspend fun sync(context: Context, device: DiscoveredDevice, since: Long): SyncBundle {
        val client = HealthConnectClient.getOrCreate(context)
        val start = Instant.ofEpochMilli(since)
        val end = Instant.now()
        val filter = TimeRangeFilter.between(start, end)

        val activities = mutableListOf<ActivityEntity>()
        val sleep = mutableListOf<SleepEntity>()

        // 1) Exercise sessions -> typed activities
        runCatching {
            client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = filter)
            ).records.forEach { s ->
                val minutes = (s.endTime.epochSecond - s.startTime.epochSecond) / 60.0
                activities += ActivityEntity(
                    type = mapExercise(s.exerciseType).label,
                    startTime = s.startTime.toEpochMilli(),
                    endTime = s.endTime.toEpochMilli(),
                    durationSeconds = s.endTime.epochSecond - s.startTime.epochSecond,
                    calories = estimateCalories(mapExercise(s.exerciseType), minutes),
                    sourceName = "Health Connect"
                )
            }
        }

        // 2) Sleep sessions
        runCatching {
            client.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = filter)
            ).records.forEach { s ->
                val minutes = ((s.endTime.epochSecond - s.startTime.epochSecond) / 60).toInt()
                sleep += SleepEntity(
                    startTime = s.startTime.toEpochMilli(),
                    endTime = s.endTime.toEpochMilli(),
                    durationMinutes = minutes,
                    sourceName = "Health Connect"
                )
            }
        }

        // 3) Aggregate steps/distance/calories -> one walking summary for the range
        runCatching {
            val agg = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = filter
                )
            )
            val steps = agg[StepsRecord.COUNT_TOTAL] ?: 0L
            val distance = agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            val energy = agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            if (steps > 0) {
                activities += ActivityEntity(
                    type = ActivityType.WALKING.label,
                    startTime = since,
                    endTime = end.toEpochMilli(),
                    durationSeconds = (end.epochSecond - start.epochSecond),
                    steps = steps.toInt(),
                    distanceMeters = distance,
                    calories = energy,
                    sourceName = "Health Connect"
                )
            }
        }

        return SyncBundle(activities = activities, sleep = sleep)
    }

    private fun mapExercise(type: Int): ActivityType = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityType.RUNNING
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityType.WALKING
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ActivityType.CYCLING
        else -> ActivityType.OTHER
    }

    private fun estimateCalories(type: ActivityType, minutes: Double, weightKg: Double = 70.0): Double =
        type.met * 3.5 * weightKg / 200.0 * minutes

    companion object {
        fun requiredPermissions(): Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class)
        )
    }
}
