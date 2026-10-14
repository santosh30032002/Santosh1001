package com.vitasync.app

import android.app.Application
import com.vitasync.app.coach.ProfileStore
import com.vitasync.app.data.local.AppDatabase
import com.vitasync.app.data.repository.DeviceRepository
import com.vitasync.app.data.repository.HealthRepository
import com.vitasync.app.device.DeviceAdapterRegistry

/**
 * Simple manual dependency container - no DI framework needed for a scaffold.
 * Swap this for Hilt later if the app grows.
 */
class VitaSyncApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val healthRepository: HealthRepository by lazy {
        HealthRepository(
            database.activityDao(),
            database.sleepDao(),
            database.waterDao(),
            database.foodDao(),
            database.fastingDao(),
            database.viceDao()
        )
    }

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(database.deviceDao(), healthRepository)
    }

    val profileStore: ProfileStore by lazy { ProfileStore(this) }

    override fun onCreate() {
        super.onCreate()
        DeviceAdapterRegistry.init()
    }
}
