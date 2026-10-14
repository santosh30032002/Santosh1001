package com.vitasync.app.coach

/**
 * The offline "AI coach" engine.
 *
 * It is a transparent rule + knowledge-base engine rather than a remote model:
 * it computes energy targets from the user's profile (Mifflin-St Jeor BMR and an
 * activity multiplier), assembles an Indian meal plan to hit those targets, and
 * turns the day's logged data into plain-language coaching.
 *
 * If you later want a real LLM in the loop, implement [CoachLlm] and pass it in;
 * the rules here then become the fallback / grounding context. Never hard-code an
 * API key in the app - load it from user settings or a build config field.
 */
data class CoachProfile(
    val weightKg: Double = 72.0,
    val heightCm: Double = 170.0,
    val age: Int = 30,
    val male: Boolean = true,
    val activityFactor: Double = 1.375,   // lightly active
    val goalKgPerWeek: Double = 0.5,
    val vegetarian: Boolean = false,
    val stepGoal: Int = 8000,
    val waterGoalMl: Int = 2500
)

data class PlannedItem(val name: String, val serving: String, val kcal: Int)
data class Meal(val name: String, val targetKcal: Int, val items: List<PlannedItem>)
data class MealPlan(val targetKcal: Int, val meals: List<Meal>)

enum class Severity { GOOD, INFO, WARN }
data class CoachInsight(val severity: Severity, val title: String, val body: String)

data class CoachSnapshot(
    val profile: CoachProfile,
    val targetKcal: Int,
    val eatenKcal: Double,
    val burnedKcal: Double,
    val steps: Int,
    val waterMl: Int,
    val sleepMinutes: Int,
    val fastHoursActive: Double,
    val fastProtocol: String?,
    val cigarettesToday: Int,
    val alcoholDrinksToday: Int,
    val alcoholKcalToday: Double
)

object CoachEngine {

    fun bmr(p: CoachProfile): Double =
        10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.male) 5 else -161

    fun tdee(p: CoachProfile): Double = bmr(p) * p.activityFactor

    /** Daily calorie target for the chosen rate of loss (7700 kcal ~ 1 kg). */
    fun targetCalories(p: CoachProfile): Int =
        (tdee(p) - p.goalKgPerWeek * 7700.0 / 7.0).coerceAtLeast(1200.0).toInt()

    /** Build an Indian meal plan that lands near [targetKcal]. */
    fun dailyPlan(targetKcal: Int, vegetarian: Boolean): MealPlan {
        val proteins = IndianFoods.byCategory(FoodCategory.PROTEIN).filter { !vegetarian || it.veg }
        val lunchPool = IndianFoods.byCategory(FoodCategory.STAPLE) +
            IndianFoods.byCategory(FoodCategory.DAL) +
            IndianFoods.byCategory(FoodCategory.VEG) +
            proteins
        val dinnerPool = IndianFoods.byCategory(FoodCategory.STAPLE) +
            IndianFoods.byCategory(FoodCategory.VEG) +
            IndianFoods.byCategory(FoodCategory.DAL)
        val breakfastPool = IndianFoods.byCategory(FoodCategory.BREAKFAST) +
            IndianFoods.byCategory(FoodCategory.FRUIT)
        val snackPool = IndianFoods.byCategory(FoodCategory.SNACK) +
            IndianFoods.byCategory(FoodCategory.FRUIT) +
            IndianFoods.byCategory(FoodCategory.BEVERAGE)

        val b = (targetKcal * 0.25).toInt()
        val l = (targetKcal * 0.35).toInt()
        val s = (targetKcal * 0.10).toInt()
        val d = (targetKcal * 0.30).toInt()

        return MealPlan(
            targetKcal = targetKcal,
            meals = listOf(
                Meal("Breakfast", b, fill(breakfastPool, b, 3)),
                Meal("Lunch", l, fill(lunchPool, l, 3)),
                Meal("Snack", s, fill(snackPool, s, 2)),
                Meal("Dinner", d, fill(dinnerPool, d, 3))
            )
        )
    }

    private fun fill(pool: List<FoodItem>, budget: Int, maxItems: Int): List<PlannedItem> {
        val out = mutableListOf<PlannedItem>()
        var remaining = budget
        for (item in pool) {
            if (out.size >= maxItems) break
            if (item.kcal <= remaining) {
                out += PlannedItem(item.name, item.serving, item.kcal)
                remaining -= item.kcal
            }
        }
        if (out.isEmpty() && pool.isNotEmpty()) {
            val f = pool.minBy { it.kcal }
            out += PlannedItem(f.name, f.serving, f.kcal)
        }
        return out
    }

    /** Turn the day's data into short, supportive coaching points. */
    fun insights(s: CoachSnapshot): List<CoachInsight> {
        val out = mutableListOf<CoachInsight>()
        val net = s.eatenKcal - s.burnedKcal
        val deficit = s.targetKcal - s.eatenKcal + s.burnedKcal

        if (deficit >= 0) {
            out += CoachInsight(
                Severity.GOOD, "On track for today",
                "You have about ${deficit.toInt()} kcal of headroom against your ${s.targetKcal} kcal target. " +
                    "Keep it steady - consistency beats a perfect day."
            )
        } else {
            out += CoachInsight(
                Severity.WARN, "Slightly over target",
                "You're about ${(-deficit).toInt()} kcal over. A 25-minute brisk walk burns ~100 kcal, " +
                    "or you could swap tonight's fried snack for roasted chana."
            )
        }

        if (s.steps < s.profile.stepGoal) {
            val need = s.profile.stepGoal - s.steps
            out += CoachInsight(
                Severity.INFO, "Steps",
                "$need steps to reach ${s.profile.stepGoal}. A 15-minute walk after dinner covers roughly 1,500."
            )
        } else {
            out += CoachInsight(Severity.GOOD, "Steps done", "You hit ${s.steps} steps today. Nice.")
        }

        if (s.fastProtocol != null && s.fastHoursActive > 0) {
            out += CoachInsight(
                Severity.INFO, "Fasting",
                "You're ${s.fastHoursActive.toInt()} h into your ${s.fastProtocol} fast. Water, black coffee " +
                    "and plain tea won't break it."
            )
        }

        if (s.waterMl < s.profile.waterGoalMl) {
            out += CoachInsight(
                Severity.INFO, "Hydration",
                "${(s.profile.waterGoalMl - s.waterMl)} ml to go. Thirst is often mistaken for hunger."
            )
        }

        if (s.sleepMinutes in 1..(6 * 60)) {
            out += CoachInsight(
                Severity.WARN, "Short sleep",
                "Under 6 hours of sleep raises the hunger hormone ghrelin and makes sugary food harder to resist. " +
                    "Try to protect 7 hours."
            )
        }

        // Vice-aware coaching, quantified.
        if (s.cigarettesToday > 0) {
            out += CoachInsight(
                Severity.WARN, "Cigarettes today: ${s.cigarettesToday}",
                "Smoking raises your resting heart rate and lowers aerobic capacity - regular smokers average " +
                    "roughly 7% lower VO2max, so the same workout burns less and recovery takes longer. " +
                    "It works against the weight-loss goal even though it can blunt appetite. " +
                    "Free help in India: call the National Tobacco Quitline on 1800-112-356."
            )
        }
        if (s.alcoholDrinksToday > 0) {
            val minutes = (s.alcoholKcalToday / 10.0).toInt()
            out += CoachInsight(
                Severity.WARN, "Alcohol today: ${s.alcoholDrinksToday} drink(s)",
                "About ${s.alcoholKcalToday.toInt()} kcal - roughly $minutes minutes of running. Alcohol is burned " +
                    "before fat, so fat oxidation pauses while it's in your system, and it fragments deep sleep. " +
                    "Cutting drinks is one of the fastest wins for a deficit."
            )
        }

        return out
    }
}

/**
 * Optional hook for a real language model. Implement this and supply an API key
 * from settings; keep [CoachEngine] as the offline fallback.
 */
interface CoachLlm {
    suspend fun suggest(prompt: String): String
}
