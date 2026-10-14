# VitaSync

A device-agnostic Android health app. Log water, food, walking, jogging,
running, cycling and sleep — and sync the same data from **any** smart watch
through a pluggable adapter layer.

Built as a starting point, not a finished product. Everything compiles as a
standard Android Studio project; the watch layer is designed so adding a new
device means writing **one class**, not touching the app.

---

## The core idea: one app, any watch

A single app cannot natively speak every vendor's private watch protocol —
Garmin, Fitbit, Samsung and Apple each use closed, proprietary protocols, and
Pebble uses its own old Bluetooth Classic protocol. So VitaSync uses a
**three-layer strategy**:

| Layer | Adapter | Covers |
|-------|---------|--------|
| Universal bridge | `HealthConnectAdapter` | Samsung, Fitbit, Garmin, Wear OS, Google Fit, Amazfit and anything else that publishes to Android **Health Connect** |
| Standard BLE | `BleGattAdapter` | Cheap bands exposing standard GATT services (Heart Rate 0x180D, etc.) |
| Direct device | `PebbleAdapter` | Pebble Steel / Time / 2 over Bluetooth Classic |

Health Connect is the important one. It is Android's OS-level health data hub:
most modern watches write into it, so reading Health Connect once gives you
data from all of them. The other adapters exist for watches that do *not* use
Health Connect.

```
UI (Compose)  →  ViewModels  →  Repositories  →  Room DB
                                     │
                                     └─► DeviceAdapterRegistry
                                            ├─ HealthConnectAdapter
                                            ├─ BleGattAdapter
                                            └─ PebbleAdapter   ← add more here
```

Every adapter implements the same interface (`device/DeviceAdapter.kt`):

```kotlin
interface DeviceAdapter {
    val adapterType: AdapterType
    val displayName: String
    suspend fun isAvailable(context: Context): Boolean
    suspend fun hasPermissions(context: Context): Boolean
    suspend fun discover(context: Context): List<DiscoveredDevice>
    suspend fun connect(context: Context, device: DiscoveredDevice): Boolean
    suspend fun sync(context: Context, device: DiscoveredDevice, since: Long): SyncBundle
}
```

To add a new watch: implement `DeviceAdapter`, register it in
`DeviceAdapterRegistry.init()`. The UI, database and repository are unchanged.

---

## What is real vs. scaffolded

Honest status so you know what you're opening:

**Fully working**
- Full app shell: Jetpack Compose UI, bottom-nav, six screens
- Local persistence with Room (activities, sleep, water, food, devices)
- Manual logging for every category
- Dashboard aggregation
- Health Connect adapter logic (reads exercise, sleep, and aggregates steps /
  distance / calories) — needs the Health Connect app on the device to run
- Bluetooth discovery for BLE devices and for bonded Pebble watches
- **Pebble health sync** - a real Bluetooth-Classic (RFCOMM) transport, the
  PhoneVersion handshake, and Pebble Health DataLogging decoding for steps and
  sleep (see `device/pebble/`)
- A foreground service (`PebbleSyncService`) that holds the Pebble link open in
  the background and streams steps/sleep into the database as they arrive
- A live Today dashboard: the Pebble status card and the step/sleep figures
  update as the service writes (records are upserted by a stable key, so
  repeated syncs never double-count)
- Fasting tracker (14:10, 16:8, 18:6, 20:4) with a live count-up timer
- An offline AI coach: Indian meal plans, smart swaps and daily coaching from a
  built-in food knowledge base
- Habit tracking for cigarettes and alcohol, with a quantified impact panel and
  India quit-support resources
- The adapter registry / repository / sync pipeline

**Scaffolded (clear TODO markers in code)**
- `BleGattAdapter.sync()` — reading live GATT characteristics into records

**Not included (by design)**
- Food database / barcode scanning (HealthifyMe-scale feature)
- Cloud sync, accounts, social features
- Charts beyond the dashboard numbers

---

## About the Pebble Steel specifically

Two facts worth knowing up front:

1. **The original Pebble Steel has no heart-rate sensor.** Only the Pebble 2
   added one. So the health data a Steel can actually give you is **steps and
   sleep** (via Pebble Health), not continuous heart rate.
2. **Pebble is Bluetooth Classic, not BLE.** It speaks a custom protocol over
   RFCOMM: length-prefixed messages ([length][endpoint][payload]) and a
   "DataLogging" endpoint that stores Pebble Health blobs.

VitaSync implements the protocol directly in `device/pebble/`:

- `PebbleProtocol.kt` - endpoints, DataLogging commands, health tags and the
  length-prefixed framing (values ported from Gadgetbridge's
  `PebbleProtocol.java`, so they match a real watch).
- `PebbleTransport.kt` - blocking RFCOMM transport with message framing.
- `PebbleAdapter.kt` - connect, PhoneVersion handshake, request datalog
  sessions, read loop, ACK/NACK.
- `PebbleHealthDecoder.kt` - decodes the tag 81 (steps) and tag 83 (sleep)
  blobs into the app's entities.

The most complete public reference for this protocol remains the open-source
**Gadgetbridge** app, which this code is based on.

---

## Live sync (foreground service)

On the **Devices** screen, a paired Pebble gets two buttons:

- **Sync once** — a one-shot pull (opens the socket, reads a ~25 s window, closes).
- **Live sync** — starts `PebbleSyncService`, a foreground service that keeps the
  socket open, re-requests the watch's sessions every 15 minutes, and writes each
  decoded record straight to the database. A low-priority notification shows the
  link state and carries a Stop action.

Because every watch-sourced record carries a stable `sourceKey`, the service can
re-emit a day's running step total as often as it likes: the unique index turns
those writes into replacements rather than additions. That is what lets the Today
dashboard update live without inflating the numbers.

The service declares `foregroundServiceType="connectedDevice"`; on Android 13+
the app also asks for notification permission so the status notification shows.

---

## Fasting, the AI coach, and habit tracking

**Fasting** tracks 14:10, 16:8, 18:6 and 20:4 windows with a live count-up timer
and a history list.

**The AI coach** (`coach/CoachEngine.kt`) is deliberately transparent rather than
a black box. It is an offline rule + knowledge-base engine:

- Energy targets come from Mifflin-St Jeor BMR and an activity multiplier, so the
target responds to the user's own weight, height, age and goal rate.
- Meal plans are assembled from `coach/IndianFoods.kt` - about 55 common Indian
dishes with approximate calories and macros, across breakfast, staples, dal,
sabzi, protein, dairy, snacks, fruit, beverages and sweets.
- Coaching points are rules over the day's data: calorie headroom, steps,
  hydration, sleep, fasting window, and the habit log.

There is a `CoachLlm` interface for plugging in a real model. If you do, keep the
rules as the grounding/fallback and load any API key from user settings or a
build config field - never hard-code it.

**Habits** logs cigarettes and drinks. `coach/ImpactEngine.kt` turns them into
quantified lines - resting heart rate, VO2max, calories per workout and recovery
for cigarettes; empty calories, equivalent exercise, the fat-oxidation pause and
deep-sleep impact for alcohol. When cigarettes are logged the app surfaces
India's free National Tobacco Quitline (1800-112-356). These figures are
population-level estimates for motivation, not personal predictions or medical
advice.

---

## Building it

Requirements: Android Studio (Ladybug or newer), JDK 17, Android SDK 35.

1. Open the `VitaSync` folder in Android Studio.
2. Let Gradle sync (it will download the AndroidX / Compose / Room / Health
   Connect dependencies).
3. Run on a device or emulator with **API 26+**.
4. On first launch, go to **Devices → Grant Health Connect access**, and pair
   your watch in Android's Bluetooth settings before tapping **Scan**.

Note: Health Connect requires the separate *Health Connect* app (preinstalled
on Android 14+, installable from Play on 9–13).

Dependency versions in `app/build.gradle.kts` are current as of late 2024 —
AndroidX releases move fast, so bump them if Android Studio suggests it.

---

## Project layout

```
app/src/main/java/com/vitasync/app/
├── VitaSyncApp.kt              Application + manual DI container
├── MainActivity.kt
├── data/
│   ├── local/                  Room: entities, DAOs, AppDatabase
│   ├── model/                  ActivityType and friends
│   └── repository/             HealthRepository, DeviceRepository
├── device/                     ← the watch layer
│   ├── DeviceAdapter.kt        the interface every watch implements
│   ├── DeviceAdapterRegistry.kt
│   ├── HealthConnectAdapter.kt universal bridge
│   ├── BleGattAdapter.kt       standard BLE
│   ├── pebble/                 PebbleAdapter, PebbleProtocol, PebbleTransport,
│   │                           PebbleHealthDecoder
│   └── model/                  DiscoveredDevice, SyncBundle
├── ui/
│   ├── AppNavHost.kt           bottom-nav navigation
│   ├── theme/                  Material 3 theme
│   └── screens/                Dashboard, Activity, Water, Food, Sleep, Devices
└── util/                       date helpers
```

## Continuous integration (Codemagic)

`codemagic.yaml` in the project root defines two workflows:

- **android-debug** - builds an installable debug APK. No secrets needed, so it
  runs as soon as the file is pushed.
- **android-release** - builds a signed `.aab` + `.apk`. Requires an Android
  keystore uploaded under Team settings > Code signing identities > Android
  keystores, using the reference name `keystore_reference` (rename it in both
  places if you prefer).

Versioning: the release workflow passes `-PversionCode=$BUILD_NUMBER` and
`-PversionName=1.0.$BUILD_NUMBER`; `app/build.gradle.kts` reads those and falls
back to its defaults for local builds. The keystore is wired in through the
`CM_KEYSTORE_PATH` / `CM_KEYSTORE_PASSWORD` / `CM_KEY_ALIAS` / `CM_KEY_PASSWORD`
variables Codemagic exports, so nothing secret is stored in the repo.

The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
is included, so CI can run `./gradlew` with no extra setup.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room · Coroutines/Flow · Navigation
Compose · Android Health Connect client · MVVM · Codemagic CI
