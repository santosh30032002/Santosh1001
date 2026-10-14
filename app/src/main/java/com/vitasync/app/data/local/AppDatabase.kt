package com.vitasync.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vitasync.app.data.local.dao.*
import com.vitasync.app.data.local.entity.*

@Database(
    entities = [
        ActivityEntity::class,
        SleepEntity::class,
        WaterEntity::class,
        FoodEntity::class,
        DeviceEntity::class,
        FastingEntity::class,
        ViceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun sleepDao(): SleepDao
    abstract fun waterDao(): WaterDao
    abstract fun foodDao(): FoodDao
    abstract fun deviceDao(): DeviceDao
    abstract fun fastingDao(): FastingDao
    abstract fun viceDao(): ViceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vitasync.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
