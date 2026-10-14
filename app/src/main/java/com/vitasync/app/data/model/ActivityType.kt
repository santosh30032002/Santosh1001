package com.vitasync.app.data.model

/**
 * The activity kinds the app understands. [met] is a rough metabolic-equivalent
 * value used to estimate calories when a source device does not report them
 * (kcal ~= MET * 3.5 * kg / 200 * minutes).
 */
enum class ActivityType(val label: String, val met: Double) {
    WALKING("Walking", 3.5),
    JOGGING("Jogging", 7.0),
    RUNNING("Running", 9.8),
    CYCLING("Cycling", 7.5),
    OTHER("Other", 4.0);

    companion object {
        fun fromLabel(label: String): ActivityType =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: OTHER
    }
}
