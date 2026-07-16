package com.kps.trackmyweight.domain.calc

data class MealProtein(val mealLabel: String, val proteinG: Float)

enum class DistributionQuality {
    EXCELLENT,   // ≥ 3 repas ≥ 25g
    GOOD,        // 2 repas ≥ 25g
    UNBALANCED,  // 1 seul repas concentre la majorité
    INSUFFICIENT,// pas assez de protéines totales
}

data class DistributionVerdict(
    val mealsAboveThreshold: Int,
    val quality: DistributionQuality,
    val advice: String,
    val totalProteinG: Float,
)

/**
 * Analyse la répartition des protéines sur les repas du jour.
 * Synthèse protéique optimale : 3-5 doses de ≥ 25g dans la journée.
 */
object ProteinDistribution {

    fun analyze(
        meals: List<MealProtein>,
        dailyTargetG: Int,
        thresholdPerMealG: Float = 25f,
    ): DistributionVerdict {
        val total = meals.sumOf { it.proteinG.toDouble() }.toFloat()
        val aboveThreshold = meals.count { it.proteinG >= thresholdPerMealG }

        val quality = when {
            total < dailyTargetG * 0.75f -> DistributionQuality.INSUFFICIENT
            aboveThreshold >= 3 -> DistributionQuality.EXCELLENT
            aboveThreshold == 2 -> DistributionQuality.GOOD
            else -> DistributionQuality.UNBALANCED
        }
        val advice = when (quality) {
            DistributionQuality.EXCELLENT -> "Répartition idéale, continue."
            DistributionQuality.GOOD -> "Bien réparti — vise 3+ repas à 25g pour un peu plus."
            DistributionQuality.UNBALANCED -> "Répartis mieux : au moins 25g à chaque repas."
            DistributionQuality.INSUFFICIENT -> "Ajoute une source de protéines : il te manque ${(dailyTargetG - total).coerceAtLeast(0f).toInt()}g."
        }
        return DistributionVerdict(
            mealsAboveThreshold = aboveThreshold,
            quality = quality,
            advice = advice,
            totalProteinG = total,
        )
    }
}
