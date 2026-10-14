package com.vitasync.app.data.model

enum class ViceType(val label: String, val unit: String) {
    CIGARETTE("Cigarette", "cigarettes"),
    ALCOHOL("Alcohol", "drinks")
}

/** Common drinks with a rough calorie value per standard serving. */
enum class AlcoholDrink(val label: String, val serving: String, val kcalPerDrink: Double) {
    BEER("Beer", "330 ml bottle", 140.0),
    WINE("Wine", "150 ml glass", 120.0),
    SPIRITS("Whisky / rum / vodka", "30 ml peg", 70.0),
    COCKTAIL("Cocktail", "1 glass", 200.0)
}
