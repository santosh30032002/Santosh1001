package com.vitasync.app.data.model

/**
 * Intermittent-fasting protocols the app can track. [fastHours] is the daily
 * fasting window; eating happens in the remaining hours.
 */
enum class FastingProtocol(
    val label: String,
    val fastHours: Int,
    val eatHours: Int,
    val note: String
) {
    H14_10("14:10", 14, 10, "Gentle starter - good for your first week."),
    H16_8("16:8", 16, 8, "The most popular. Skip breakfast or dinner."),
    H18_6("18:6", 18, 6, "Tighter window once 16:8 feels easy."),
    H20_4("20:4", 20, 4, "Advanced - eat in one short window.")
}
