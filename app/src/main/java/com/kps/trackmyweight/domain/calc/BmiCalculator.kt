package com.kps.trackmyweight.domain.calc

/** Catégorisation OMS. */
enum class BmiCategory {
    UNDERWEIGHT,       // < 18.5
    NORMAL,            // 18.5 - 24.9
    OVERWEIGHT,        // 25 - 29.9
    OBESITY_CLASS_I,   // 30 - 34.9
    OBESITY_CLASS_II,  // 35 - 39.9
    OBESITY_CLASS_III, // ≥ 40
}

object BmiCalculator {
    fun compute(weightKg: Float, heightCm: Float): Float {
        require(heightCm > 0f) { "height must be positive" }
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    fun categorize(bmi: Float): BmiCategory = when {
        bmi < 18.5f -> BmiCategory.UNDERWEIGHT
        bmi < 25f -> BmiCategory.NORMAL
        bmi < 30f -> BmiCategory.OVERWEIGHT
        bmi < 35f -> BmiCategory.OBESITY_CLASS_I
        bmi < 40f -> BmiCategory.OBESITY_CLASS_II
        else -> BmiCategory.OBESITY_CLASS_III
    }
}
