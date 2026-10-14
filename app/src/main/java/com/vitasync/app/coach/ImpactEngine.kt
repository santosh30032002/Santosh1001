package com.vitasync.app.coach

/**
 * Quantifies the effect of smoking and drinking on a weight-loss journey.
 *
 * These are population-level estimates, not personal predictions - they are
 * meant to make the cost visible, not to alarm. Figures are drawn from the
 * published ranges noted in the README.
 */
data class ImpactLine(val label: String, val value: String, val detail: String)

object ImpactEngine {

    /** Rough per-cigarette price in INR, used for the money line. */
    private const val RUPEE_PER_CIGARETTE = 15

    fun cigaretteImpact(todayCount: Int, weekCount: Int): List<ImpactLine> {
        if (todayCount == 0 && weekCount == 0) return emptyList()
        val lines = mutableListOf<ImpactLine>()
        lines += ImpactLine(
            "Resting heart rate",
            "+5 to 10 bpm",
            "Nicotine keeps your heart working harder even at rest."
        )
        lines += ImpactLine(
            "Aerobic capacity (VO2max)",
            "up to ~7% lower",
            "Carbon monoxide ties up oxygen, so muscles get less of it during exercise."
        )
        lines += ImpactLine(
            "Calories per workout",
            "~5-8% less",
            "Lower oxygen delivery means the same session burns fewer calories."
        )
        lines += ImpactLine(
            "Recovery after exercise",
            "slower",
            "Heart-rate recovery is measurably delayed in smokers."
        )
        val weeklyMoney = weekCount * RUPEE_PER_CIGARETTE
        if (weeklyMoney > 0) {
            lines += ImpactLine(
                "Weekly spend",
                "~Rs $weeklyMoney",
                "About Rs ${weeklyMoney * 52} a year at this rate."
            )
        }
        return lines
    }

    fun alcoholImpact(drinks: Int, kcal: Double): List<ImpactLine> {
        if (drinks <= 0) return emptyList()
        val runMinutes = (kcal / 10.0).toInt()
        return listOf(
            ImpactLine(
                "Empty calories",
                "${kcal.toInt()} kcal",
                "Alcohol gives ~7 kcal per gram with no protein, fibre or nutrients."
            ),
            ImpactLine(
                "Equivalent exercise",
                "~$runMinutes min running",
                "For a 70 kg person at a moderate pace."
            ),
            ImpactLine(
                "Fat burning",
                "paused ~${drinks} h",
                "Alcohol is oxidised first, so fat oxidation drops while it is in your system."
            ),
            ImpactLine(
                "Deep sleep",
                "reduced",
                "A nightcap fragments deep sleep - and poor sleep drives next-day cravings."
            ),
            ImpactLine(
                "Appetite",
                "higher",
                "Alcohol lowers restraint, which is why late-night snacking follows."
            )
        )
    }
}
