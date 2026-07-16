package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.GoalPhase
import kotlin.math.abs
import kotlin.math.roundToInt

data class CalorieAdjustmentAdvice(
    val newTargetKcal: Int,
    val deltaKcal: Int,
    val stepsSuggestionDaily: Int,
    val reason: String,
)

/**
 * Ajuste la cible calorique hebdomadaire selon la tendance réelle du poids.
 * Règles simples (adaptable) :
 *
 *  Cut :
 *   - Perte < 0.2 kg/sem depuis 2 semaines → -100 kcal ou +1000 pas
 *   - Perte > 0.8 kg/sem → +100 kcal (trop rapide, risque perte muscle)
 *  Bulk :
 *   - Gain < 0.1 kg/sem → +100 kcal
 *   - Gain > 0.4 kg/sem → -100 kcal (trop de graisse)
 *  Recomp / Maintenance : pas d'ajustement automatique agressif.
 */
object CalorieAdapter {

    fun advise(
        phase: GoalPhase,
        currentTargetKcal: Int,
        weeklyRateActualKg: Float,
    ): CalorieAdjustmentAdvice? {
        return when (phase) {
            GoalPhase.CUT -> adviseCut(currentTargetKcal, weeklyRateActualKg)
            GoalPhase.BULK -> adviseBulk(currentTargetKcal, weeklyRateActualKg)
            GoalPhase.RECOMP, GoalPhase.MAINTENANCE -> {
                if (abs(weeklyRateActualKg) > 0.5f) {
                    CalorieAdjustmentAdvice(
                        newTargetKcal = currentTargetKcal + (if (weeklyRateActualKg > 0) -100 else 100),
                        deltaKcal = if (weeklyRateActualKg > 0) -100 else 100,
                        stepsSuggestionDaily = 0,
                        reason = "Ta tendance actuelle diverge trop d'une maintenance stricte.",
                    )
                } else null
            }
        }
    }

    private fun adviseCut(currentTargetKcal: Int, rate: Float): CalorieAdjustmentAdvice? {
        val expected = -0.4f
        return when {
            rate > -0.2f -> CalorieAdjustmentAdvice(
                newTargetKcal = currentTargetKcal - 100,
                deltaKcal = -100,
                stepsSuggestionDaily = 1000,
                reason = "Perte trop lente (${"%.2f".format(rate)} kg/sem). Retire 100 kcal ou ajoute 1000 pas.",
            )
            rate < -0.8f -> CalorieAdjustmentAdvice(
                newTargetKcal = currentTargetKcal + 100,
                deltaKcal = 100,
                stepsSuggestionDaily = 0,
                reason = "Perte trop rapide (${"%.2f".format(rate)} kg/sem). Risque de perdre du muscle — ajoute 100 kcal.",
            )
            else -> null
        }
    }

    private fun adviseBulk(currentTargetKcal: Int, rate: Float): CalorieAdjustmentAdvice? {
        return when {
            rate < 0.1f -> CalorieAdjustmentAdvice(
                newTargetKcal = currentTargetKcal + 100,
                deltaKcal = 100,
                stepsSuggestionDaily = 0,
                reason = "Prise trop lente (${"%.2f".format(rate)} kg/sem). Ajoute 100 kcal.",
            )
            rate > 0.4f -> CalorieAdjustmentAdvice(
                newTargetKcal = currentTargetKcal - 100,
                deltaKcal = -100,
                stepsSuggestionDaily = 0,
                reason = "Prise trop rapide (${"%.2f".format(rate)} kg/sem). Réduis de 100 kcal.",
            )
            else -> null
        }
    }

    /** Calcule le rythme hebdomadaire réel à partir de 2+ points de poids (moyenne mobile idéalement). */
    fun weeklyRateFrom(points: List<DatedValue>): Float {
        val fit = TimeSeries.linearFit(points) ?: return 0f
        val slopePerDay = fit.first
        return slopePerDay * 7f
    }
}
