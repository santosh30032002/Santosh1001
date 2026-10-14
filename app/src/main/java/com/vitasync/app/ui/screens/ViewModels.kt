package com.vitasync.app.ui.screens

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vitasync.app.VitaSyncApp
import com.vitasync.app.coach.*
import com.vitasync.app.data.local.entity.*
import com.vitasync.app.data.model.ActivityType
import com.vitasync.app.data.model.AlcoholDrink
import com.vitasync.app.data.model.FastingProtocol
import com.vitasync.app.data.model.ViceType
import com.vitasync.app.device.model.AdapterType
import com.vitasync.app.device.model.DiscoveredDevice
import com.vitasync.app.device.pebble.PebbleLink
import com.vitasync.app.device.pebble.PebbleSyncService
import com.vitasync.app.util.TimeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VMFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = app as VitaSyncApp
        val repo = container.healthRepository
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repo)
            modelClass.isAssignableFrom(ActivityViewModel::class.java) -> ActivityViewModel(repo)
            modelClass.isAssignableFrom(WaterViewModel::class.java) -> WaterViewModel(repo)
            modelClass.isAssignableFrom(FoodViewModel::class.java) -> FoodViewModel(repo)
            modelClass.isAssignableFrom(SleepViewModel::class.java) -> SleepViewModel(repo)
            modelClass.isAssignableFrom(DevicesViewModel::class.java) -> DevicesViewModel(app, container.deviceRepository)
            modelClass.isAssignableFrom(FastingViewModel::class.java) -> FastingViewModel(repo)
            modelClass.isAssignableFrom(VicesViewModel::class.java) -> VicesViewModel(repo)
            modelClass.isAssignableFrom(CoachViewModel::class.java) -> CoachViewModel(app, repo)
            else -> error("Unknown ViewModel $modelClass")
        } as T
    }
}

// ---------------- Dashboard ----------------

data class DashboardState(
    val steps: Int = 0,
    val calories: Double = 0.0,
    val distanceKm: Double = 0.0,
    val waterMl: Int = 0,
    val foodKcal: Double = 0.0,
    val sleepMinutes: Int = 0,
    val pebbleLive: Boolean = false,
    val pebbleLastSync: Long = 0L
)

class DashboardViewModel(repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    private val movement = combine(
        repo.stepsToday(), repo.caloriesToday(), repo.distanceToday()
    ) { steps, cal, dist -> Triple(steps, cal, dist) }

    private val intake = combine(
        repo.waterTotalToday(), repo.foodCaloriesToday(), repo.sleepToday()
    ) { water, food, sleep -> Triple(water, food, sleep.sumOf { it.durationMinutes }) }

    val state: StateFlow<DashboardState> = combine(
        movement, intake, PebbleLink.connected, PebbleLink.lastSync
    ) { move, eat, live, lastSync ->
        DashboardState(
            steps = move.first,
            calories = move.second,
            distanceKm = move.third / 1000.0,
            waterMl = eat.first,
            foodKcal = eat.second,
            sleepMinutes = eat.third,
            pebbleLive = live,
            pebbleLastSync = lastSync
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

// ---------------- Existing loggers ----------------

class ActivityViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    val activities = repo.activitiesToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun log(type: String, minutes: Int, steps: Int, distanceMeters: Double, calories: Double) {
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            repo.logActivity(
                ActivityEntity(
                    type = type, startTime = end - minutes * 60_000L, endTime = end,
                    durationSeconds = minutes * 60L, steps = steps,
                    distanceMeters = distanceMeters, calories = calories, sourceName = "Manual"
                )
            )
        }
    }
}

class WaterViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    val totalToday = repo.waterTotalToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val entries = repo.waterToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun add(ml: Int) = viewModelScope.launch { repo.logWater(ml) }
}

class FoodViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    val entries = repo.foodToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val caloriesToday = repo.foodCaloriesToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    fun log(name: String, meal: String, kcal: Double, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            repo.logFood(FoodEntity(timestamp = System.currentTimeMillis(), name = name, mealType = meal,
                calories = kcal, proteinGrams = protein, carbsGrams = carbs, fatGrams = fat))
        }
    }
}

class SleepViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    val entries = repo.sleepToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun log(minutes: Int, deep: Int, rem: Int) {
        viewModelScope.launch {
            val end = TimeUtils.startOfToday()
            repo.logSleep(SleepEntity(startTime = end - minutes * 60_000L, endTime = end,
                durationMinutes = minutes, deepMinutes = deep,
                lightMinutes = (minutes - deep - rem).coerceAtLeast(0), remMinutes = rem, sourceName = "Manual"))
        }
    }
}

// ---------------- Fasting ----------------

class FastingViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {
    val active = repo.activeFast().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val today = repo.fastsToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start(protocol: FastingProtocol) = viewModelScope.launch {
        repo.startFast(protocol.label, protocol.fastHours)
    }
    fun stop(id: Long) = viewModelScope.launch { repo.endFast(id) }
}

// ---------------- Vices ----------------

data class VicesUiState(
    val cigarettesToday: Int = 0,
    val drinksToday: Int = 0,
    val alcoholKcalToday: Double = 0.0,
    val weekCigarettes: Int = 0,
    val entries: List<ViceEntity> = emptyList(),
    val cigaretteImpact: List<ImpactLine> = emptyList(),
    val alcoholImpact: List<ImpactLine> = emptyList()
)

class VicesViewModel(private val repo: com.vitasync.app.data.repository.HealthRepository) : ViewModel() {

    val state: StateFlow<VicesUiState> = combine(
        repo.vicesToday(), repo.allVices()
    ) { today, all ->
        val cigsToday = today.filter { it.type == ViceType.CIGARETTE.name }.sumOf { it.quantity }
        val drinksToday = today.filter { it.type == ViceType.ALCOHOL.name }.sumOf { it.quantity }
        val alcoholKcal = today.filter { it.type == ViceType.ALCOHOL.name }.sumOf { it.calories }
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 3600_000L
        val cigsWeek = all.filter { it.type == ViceType.CIGARETTE.name && it.timestamp >= weekAgo }
            .sumOf { it.quantity }
        VicesUiState(
            cigarettesToday = cigsToday,
            drinksToday = drinksToday,
            alcoholKcalToday = alcoholKcal,
            weekCigarettes = cigsWeek,
            entries = today,
            cigaretteImpact = ImpactEngine.cigaretteImpact(cigsToday, cigsWeek),
            alcoholImpact = ImpactEngine.alcoholImpact(drinksToday, alcoholKcal)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VicesUiState())

    fun logCigarettes(count: Int) = viewModelScope.launch { repo.logCigarettes(count) }
    fun logAlcohol(drink: AlcoholDrink, count: Int) = viewModelScope.launch { repo.logAlcohol(drink, count) }
}

// ---------------- Coach ----------------

data class CoachUiState(
    val profile: CoachProfile = CoachProfile(),
    val bmr: Int = 0,
    val tdee: Int = 0,
    val targetKcal: Int = 0,
    val plan: MealPlan = MealPlan(0, emptyList()),
    val insights: List<CoachInsight> = emptyList(),
    val cigaretteImpact: List<ImpactLine> = emptyList(),
    val alcoholImpact: List<ImpactLine> = emptyList()
)

class CoachViewModel(
    app: Application,
    private val repo: com.vitasync.app.data.repository.HealthRepository
) : AndroidViewModel(app) {

    private val store = (app as VitaSyncApp).profileStore
    private val profileFlow = MutableStateFlow(store.load())

    private val day = combine(
        repo.foodCaloriesToday(), repo.caloriesToday(), repo.stepsToday()
    ) { food, burned, steps -> Triple(food, burned, steps) }

    private val context = combine(
        repo.waterTotalToday(), repo.sleepToday(), repo.activeFast()
    ) { water, sleep, fast -> Triple(water, sleep.sumOf { it.durationMinutes }, fast) }

    val state: StateFlow<CoachUiState> = combine(
        day, context, repo.vicesToday(), profileFlow
    ) { d, c, vices, profile ->
        val target = CoachEngine.targetCalories(profile)
        val cigs = vices.filter { it.type == ViceType.CIGARETTE.name }.sumOf { it.quantity }
        val drinks = vices.filter { it.type == ViceType.ALCOHOL.name }.sumOf { it.quantity }
        val alcoholKcal = vices.filter { it.type == ViceType.ALCOHOL.name }.sumOf { it.calories }
        val fast = c.third
        val fastHours = fast?.let {
            ((System.currentTimeMillis() - it.startTime) / 3600_000.0)
        } ?: 0.0

        val snapshot = CoachSnapshot(
            profile = profile,
            targetKcal = target,
            eatenKcal = d.first,
            burnedKcal = d.second,
            steps = d.third,
            waterMl = c.first,
            sleepMinutes = c.second,
            fastHoursActive = fastHours,
            fastProtocol = fast?.protocol,
            cigarettesToday = cigs,
            alcoholDrinksToday = drinks,
            alcoholKcalToday = alcoholKcal
        )
        CoachUiState(
            profile = profile,
            bmr = CoachEngine.bmr(profile).toInt(),
            tdee = CoachEngine.tdee(profile).toInt(),
            targetKcal = target,
            plan = CoachEngine.dailyPlan(target, profile.vegetarian),
            insights = CoachEngine.insights(snapshot),
            cigaretteImpact = ImpactEngine.cigaretteImpact(cigs, cigs),
            alcoholImpact = ImpactEngine.alcoholImpact(drinks, alcoholKcal)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState())

    fun updateProfile(profile: CoachProfile) {
        store.save(profile)
        profileFlow.value = profile
    }
}

// ---------------- Devices ----------------

data class DevicesUiState(
    val paired: List<DeviceEntity> = emptyList(),
    val discovered: List<DiscoveredDevice> = emptyList(),
    val scanning: Boolean = false,
    val message: String? = null,
    val liveConnected: Boolean = false
)

class DevicesViewModel(
    private val app: Application,
    private val repo: com.vitasync.app.data.repository.DeviceRepository
) : AndroidViewModel(app) {

    private val discovered = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    private val scanning = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<DevicesUiState> = combine(
        repo.observeDevices(), discovered, scanning, message, PebbleLink.connected
    ) { paired, found, scan, msg, live -> DevicesUiState(paired, found, scan, msg, live) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun scan() = viewModelScope.launch {
        scanning.value = true; message.value = null
        discovered.value = repo.discover(app)
        scanning.value = false
        message.value = if (discovered.value.isEmpty())
            "No devices found. Pair the watch in Android Bluetooth settings first."
        else "Found ${discovered.value.size} device(s)."
    }
    fun pair(device: DiscoveredDevice) = viewModelScope.launch {
        message.value = if (repo.pair(app, device)) "Paired ${device.name}" else "Could not pair ${device.name}"
    }
    fun sync(entity: DeviceEntity) = viewModelScope.launch { message.value = repo.sync(app, entity) }

    fun startLiveSync(entity: DeviceEntity) {
        if (entity.adapterType != AdapterType.PEBBLE.name) {
            message.value = "Live sync is currently implemented for Pebble"; return
        }
        ContextCompat.startForegroundService(app, Intent(app, PebbleSyncService::class.java).apply {
            action = PebbleSyncService.ACTION_START
            putExtra(PebbleSyncService.EXTRA_ADDRESS, entity.address)
        })
        message.value = "Live sync started for ${entity.name}"
    }
    fun stopLiveSync() {
        app.startService(Intent(app, PebbleSyncService::class.java).setAction(PebbleSyncService.ACTION_STOP))
        message.value = "Live sync stopped"
    }
}
