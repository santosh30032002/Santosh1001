package com.vitasync.app.coach

/**
 * A small offline knowledge base of common Indian foods, used by the coach to
 * build meal plans and suggest swaps. Values are typical home-cooked portions
 * and are approximate - they are guidance, not a lab measurement.
 */
enum class FoodCategory { BREAKFAST, STAPLE, DAL, VEG, PROTEIN, DAIRY, SNACK, FRUIT, BEVERAGE, SWEET }

data class FoodItem(
    val name: String,
    val serving: String,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val category: FoodCategory,
    val veg: Boolean = true
)

object IndianFoods {
    val all: List<FoodItem> = listOf(
        // Breakfast
        FoodItem("Poha (flattened rice)", "1 bowl", 250, 5.0, 45.0, 6.0, FoodCategory.BREAKFAST),
        FoodItem("Upma", "1 bowl", 250, 6.0, 40.0, 8.0, FoodCategory.BREAKFAST),
        FoodItem("Idli", "2 pieces", 140, 4.0, 28.0, 1.0, FoodCategory.BREAKFAST),
        FoodItem("Plain dosa", "1 piece", 133, 3.0, 22.0, 4.0, FoodCategory.BREAKFAST),
        FoodItem("Masala dosa", "1 piece", 250, 5.0, 38.0, 9.0, FoodCategory.BREAKFAST),
        FoodItem("Uttapam", "1 piece", 200, 5.0, 33.0, 5.0, FoodCategory.BREAKFAST),
        FoodItem("Moong dal chilla", "2 pieces", 150, 10.0, 18.0, 4.0, FoodCategory.BREAKFAST),
        FoodItem("Besan chilla", "2 pieces", 170, 9.0, 18.0, 7.0, FoodCategory.BREAKFAST),
        FoodItem("Dhokla", "2 pieces", 160, 6.0, 26.0, 4.0, FoodCategory.BREAKFAST),
        FoodItem("Thepla", "2 pieces", 260, 6.0, 34.0, 11.0, FoodCategory.BREAKFAST),
        FoodItem("Vegetable oats", "1 bowl", 190, 7.0, 30.0, 5.0, FoodCategory.BREAKFAST),

        // Staples
        FoodItem("Roti / chapati", "1 medium", 100, 3.0, 20.0, 1.0, FoodCategory.STAPLE),
        FoodItem("Phulka", "1 piece", 70, 2.5, 15.0, 0.5, FoodCategory.STAPLE),
        FoodItem("Aloo paratha", "1 piece", 300, 6.0, 40.0, 12.0, FoodCategory.STAPLE),
        FoodItem("Steamed rice", "1 bowl", 200, 4.0, 45.0, 0.5, FoodCategory.STAPLE),
        FoodItem("Khichdi", "1 bowl", 250, 8.0, 42.0, 5.0, FoodCategory.STAPLE),
        FoodItem("Biryani (veg)", "1 bowl", 400, 9.0, 55.0, 15.0, FoodCategory.STAPLE),

        // Dal / legumes
        FoodItem("Dal (toor/moong)", "1 bowl", 150, 9.0, 20.0, 4.0, FoodCategory.DAL),
        FoodItem("Rajma", "1 bowl", 220, 11.0, 32.0, 5.0, FoodCategory.DAL),
        FoodItem("Chole (chana masala)", "1 bowl", 250, 11.0, 35.0, 8.0, FoodCategory.DAL),
        FoodItem("Sambar", "1 bowl", 100, 5.0, 14.0, 3.0, FoodCategory.DAL),
        FoodItem("Sprouts salad", "1 bowl", 120, 9.0, 16.0, 2.0, FoodCategory.DAL),

        // Vegetables
        FoodItem("Mixed veg sabzi", "1 bowl", 120, 3.0, 14.0, 6.0, FoodCategory.VEG),
        FoodItem("Bhindi masala", "1 bowl", 150, 3.0, 12.0, 10.0, FoodCategory.VEG),
        FoodItem("Palak paneer", "1 bowl", 280, 12.0, 10.0, 20.0, FoodCategory.VEG),
        FoodItem("Green salad", "1 plate", 50, 2.0, 8.0, 1.0, FoodCategory.VEG),

        // Protein
        FoodItem("Paneer (raw)", "100 g", 265, 18.0, 4.0, 20.0, FoodCategory.PROTEIN),
        FoodItem("Egg (boiled)", "1 egg", 78, 6.0, 0.6, 5.0, FoodCategory.PROTEIN, veg = false),
        FoodItem("Egg omelette", "2 eggs", 220, 13.0, 2.0, 17.0, FoodCategory.PROTEIN, veg = false),
        FoodItem("Chicken curry", "1 bowl", 250, 22.0, 6.0, 15.0, FoodCategory.PROTEIN, veg = false),
        FoodItem("Grilled chicken breast", "150 g", 240, 45.0, 0.0, 5.0, FoodCategory.PROTEIN, veg = false),
        FoodItem("Fish curry", "1 bowl", 200, 20.0, 5.0, 11.0, FoodCategory.PROTEIN, veg = false),
        FoodItem("Soya chunks (dry)", "50 g", 170, 26.0, 12.0, 1.0, FoodCategory.PROTEIN),
        FoodItem("Tofu", "100 g", 80, 8.0, 2.0, 5.0, FoodCategory.PROTEIN),

        // Dairy
        FoodItem("Curd / dahi", "1 bowl", 100, 5.0, 6.0, 5.0, FoodCategory.DAIRY),
        FoodItem("Raita", "1 bowl", 80, 4.0, 6.0, 4.0, FoodCategory.DAIRY),
        FoodItem("Buttermilk (chaas)", "1 glass", 40, 2.0, 4.0, 1.0, FoodCategory.DAIRY),
        FoodItem("Sweet lassi", "1 glass", 220, 6.0, 32.0, 6.0, FoodCategory.DAIRY),

        // Snacks
        FoodItem("Roasted chana", "30 g", 120, 6.0, 18.0, 2.0, FoodCategory.SNACK),
        FoodItem("Roasted peanuts", "30 g", 170, 7.0, 6.0, 14.0, FoodCategory.SNACK),
        FoodItem("Samosa", "1 piece", 260, 4.0, 30.0, 14.0, FoodCategory.SNACK),
        FoodItem("Pakora / bhajji", "4 pieces", 200, 5.0, 18.0, 12.0, FoodCategory.SNACK),
        FoodItem("Vada pav", "1 piece", 300, 7.0, 42.0, 11.0, FoodCategory.SNACK),
        FoodItem("Pav bhaji", "1 plate", 400, 9.0, 50.0, 17.0, FoodCategory.SNACK),
        FoodItem("Chana chaat", "1 bowl", 200, 8.0, 30.0, 5.0, FoodCategory.SNACK),

        // Fruit
        FoodItem("Banana", "1 medium", 105, 1.3, 27.0, 0.3, FoodCategory.FRUIT),
        FoodItem("Apple", "1 medium", 95, 0.5, 25.0, 0.3, FoodCategory.FRUIT),
        FoodItem("Papaya", "1 bowl", 60, 1.0, 15.0, 0.4, FoodCategory.FRUIT),
        FoodItem("Guava", "1 medium", 68, 2.6, 14.0, 1.0, FoodCategory.FRUIT),
        FoodItem("Mango", "1 medium", 150, 1.4, 38.0, 0.6, FoodCategory.FRUIT),

        // Beverages
        FoodItem("Chai with sugar", "1 cup", 90, 2.0, 12.0, 3.0, FoodCategory.BEVERAGE),
        FoodItem("Black coffee", "1 cup", 5, 0.3, 0.0, 0.0, FoodCategory.BEVERAGE),
        FoodItem("Coconut water", "1 glass", 45, 0.5, 9.0, 0.5, FoodCategory.BEVERAGE),
        FoodItem("Nimbu pani (with sugar)", "1 glass", 60, 0.2, 15.0, 0.0, FoodCategory.BEVERAGE),

        // Sweets
        FoodItem("Gulab jamun", "1 piece", 150, 2.0, 22.0, 6.0, FoodCategory.SWEET),
        FoodItem("Jalebi", "100 g", 350, 2.0, 60.0, 12.0, FoodCategory.SWEET),
        FoodItem("Ghee", "1 tsp", 45, 0.0, 0.0, 5.0, FoodCategory.SWEET)
    )

    fun byCategory(c: FoodCategory): List<FoodItem> = all.filter { it.category == c }

    /** A lighter alternative for a common indulgence, for swap suggestions. */
    val swaps: List<Pair<String, String>> = listOf(
        "Samosa" to "Roasted chana (30 g) - saves ~140 kcal",
        "Sweet lassi" to "Buttermilk / chaas - saves ~180 kcal",
        "Pav bhaji" to "Chana chaat - saves ~200 kcal",
        "Aloo paratha" to "Moong dal chilla - saves ~150 kcal",
        "Masala dosa" to "Plain dosa + sambar - saves ~120 kcal",
        "Chai with sugar" to "Black coffee or unsweetened chai - saves ~70 kcal",
        "Gulab jamun" to "Papaya bowl - saves ~90 kcal",
        "Fried pakora" to "Dhokla - saves ~40 kcal",
        "Steamed rice (big bowl)" to "Phulka x2 - saves ~60 kcal",
        "Jalebi" to "Guava - saves ~280 kcal"
    )
}
