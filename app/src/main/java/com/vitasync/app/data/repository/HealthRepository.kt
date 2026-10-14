package com.vitasync.app.data.repository

import com.vitasync.app.data.local.dao.*
import com.vitasync.app.data.local.entity.*
import com.vitasync.app.data.model.AlcoholDrink
import com.vitasync.app.data.model.ViceType
import com.vitasync.app.util.TimeUtils
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the user's health records. The UI never talks to
 * Room directly and never talks to a watch directly - it goes through here.
 */
class HealthRepository(
    private val activityDao: ActivityDao,
    private val sleepDao: SleepDao,
    private val waterDao: WaterDao,
    private val foodDao: FoodDao,
    private val fastingDao: FastingDao,
    private val viceDao: ViceDao
) {
    private val today: Long get() = TimeUtils.startOfToday()
    private val tomorrow: Long get() = TimeUtils.startOfTomorrow()

    // Activities / movement
    fun activitiesToday(): Flow<List<ActivityEntity>> = activityDao.observeRange(today, tomorrow)
    fun allActivities(): Flow<List<ActivityEntity>> = activityDao.observeAll()
    fun stepsToday(): Flow<Int> = activityDao.observeSteps(today, tomorrow)
    fun caloriesToday(): Flow<Double> = activityDao.observeCalories(today, tomorrow)
    fun distanceToday(): Flow<Double> = activityDao.observeDistance(today, tomorrow)
    suspend fun logActivity(a: ActivityEntity) = activityDao.insert(a)

    // Sleep
    fun sleepToday(): Flow<List<SleepEntity>> = sleepDao.observeRange(today, tomorrow)
    fun allSleep(): Flow<List<SleepEntity>> = sleepDao.observeAll()
    suspend fun logSleep(s: SleepEntity) = sleepDao.insert(s)

    // Water
    fun waterToday(): Flow<List<WaterEntity>> = waterDao.observeRange(today, tomorrow)
    fun waterTotalToday(): Flow<Int> = waterDao.observeTotal(today, tomorrow)
    suspend fun logWater(ml: Int, at: Long = System.currentTimeMillis()) =
        waterDao.insert(WaterEntity(timestamp = at, milliliters = ml))

    // Food
    fun foodToday(): Flow<List<FoodEntity>> = foodDao.observeRange(today, tomorrow)
    fun foodCaloriesToday(): Flow<Double> = foodDao.observeCalories(today, tomorrow)
    suspend fun logFood(f: FoodEntity) = foodDao.insert(f)

    // Fasting
    fun activeFast(): Flow<FastingEntity?> = fastingDao.observeActive()
    fun fastsToday(): Flow<List<FastingEntity>> = fastingDao.observeRange(today, tomorrow)
    fun allFasts(): Flow<List<FastingEntity>> = fastingDao.observeAll()
    suspend fun startFast(protocol: String, targetHours: Int): Long =
        fastingDao.insert(
            FastingEntity(
                startTime = System.currentTimeMillis(),
                targetHours = targetHours,
                protocol = protocol
            )
        )
    suspend fun endFast(id: Long) = fastingDao.finish(id, System.currentTimeMillis())

    // Vices
    fun vicesToday(): Flow<List<ViceEntity>> = viceDao.observeRange(today, tomorrow)
    fun allVices(): Flow<List<ViceEntity>> = viceDao.observeAll()

    suspend fun logCigarettes(count: Int, note: String = "") =
        viceDao.insert(
            ViceEntity(
                type = ViceType.CIGARETTE.name,
                timestamp = System.currentTimeMillis(),
                quantity = count,
                unitLabel = ViceType.CIGARETTE.unit,
                detail = note
            )
        )

    suspend fun logAlcohol(drink: AlcoholDrink, count: Int) =
        viceDao.insert(
            ViceEntity(
                type = ViceType.ALCOHOL.name,
                timestamp = System.currentTimeMillis(),
                quantity = count,
                unitLabel = ViceType.ALCOHOL.unit,
                detail = drink.label,
                calories = drink.kcalPerDrink * count
            )
        )
}
