package com.vitasync.app.coach

import android.content.Context

/** Persists the coach profile in SharedPreferences (small, single record). */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("vitasync_coach", Context.MODE_PRIVATE)

    fun load(): CoachProfile = CoachProfile(
        weightKg = prefs.getFloat("weight", 72f).toDouble(),
        heightCm = prefs.getFloat("height", 170f).toDouble(),
        age = prefs.getInt("age", 30),
        male = prefs.getBoolean("male", true),
        activityFactor = prefs.getFloat("activity", 1.375f).toDouble(),
        goalKgPerWeek = prefs.getFloat("goal", 0.5f).toDouble(),
        vegetarian = prefs.getBoolean("veg", false),
        stepGoal = prefs.getInt("stepGoal", 8000),
        waterGoalMl = prefs.getInt("waterGoal", 2500)
    )

    fun save(p: CoachProfile) {
        prefs.edit()
            .putFloat("weight", p.weightKg.toFloat())
            .putFloat("height", p.heightCm.toFloat())
            .putInt("age", p.age)
            .putBoolean("male", p.male)
            .putFloat("activity", p.activityFactor.toFloat())
            .putFloat("goal", p.goalKgPerWeek.toFloat())
            .putBoolean("veg", p.vegetarian)
            .putInt("stepGoal", p.stepGoal)
            .putInt("waterGoal", p.waterGoalMl)
            .apply()
    }
}
