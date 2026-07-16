package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.exp
import kotlin.math.max

data class ProjectionResult(
    val projectedWeightAtTarget: Float,
    val trendKgPerWeek: Float,
    val etaForTarget: LocalDate?,
    val confidence: Float,   // 0..1
    val hasPlateauExpected: Boolean,
    val narrative: String,
)

/**
 * Projection non-linéaire qui tient compte du fait que la perte de poids ralentit
 * naturellement à mesure qu'on approche de la cible (courbe exponentielle décroissante).
 *
 * Modèle simplifié : W(t) = W_target + (W_current - W_target) × exp(-k × t)
 * où k est ajusté depuis la tendance linéaire récente.
 */
object NonLinearProjection {

    fun project(
        points: List<DatedValue>,
        targetWeightKg: Float,
        targetDate: LocalDate,
        today: LocalDate,
    ): ProjectionResult {
        val fit = TimeSeries.linearFit(points) ?: return default(targetWeightKg, targetDate, today)
        val slopePerDay = fit.first
        val currentWeight = points.maxByOrNull { it.date }?.value ?: fit.second
        val trendPerWeek = slopePerDay * 7f
        val hasPlateauExpected = kotlin.math.abs(trendPerWeek) < 0.15f

        // Détermine k pour que la variation initiale colle à la pente linéaire.
        // Modèle W(t) = W_target + (W_current − W_target) × exp(−k × t).
        // Dérivée en t=0 : −k × (W_current − W_target) = slopePerDay.
        // Donc k = −slopePerDay / (W_current − W_target). k>0 quand on converge vers la cible.
        val diff = currentWeight - targetWeightKg   // positif si on doit descendre
        val k = if (kotlin.math.abs(diff) > 0.1f) {
            (-slopePerDay / diff).coerceIn(-0.05f, 0.05f)
        } else 0f

        val daysToTarget = today.daysUntil(targetDate)
        val projectedAtTarget = targetWeightKg + diff * exp(-k * daysToTarget.toFloat())

        // ETA : jour où l'on arrive à ±0.3 kg de la cible.
        val eta = if (k > 0f && kotlin.math.abs(diff) > 0.3f) {
            val t = kotlin.math.ln(kotlin.math.abs(diff) / 0.3f) / k
            if (t.isFinite() && t > 0) LocalDate.fromEpochDays(today.toEpochDays() + t.toInt()) else null
        } else null

        val confidence = confidenceFrom(points.size, hasPlateauExpected)
        val narrative = buildNarrative(currentWeight, targetWeightKg, projectedAtTarget, trendPerWeek, hasPlateauExpected)

        return ProjectionResult(
            projectedWeightAtTarget = projectedAtTarget,
            trendKgPerWeek = trendPerWeek,
            etaForTarget = eta,
            confidence = confidence,
            hasPlateauExpected = hasPlateauExpected,
            narrative = narrative,
        )
    }

    private fun confidenceFrom(sampleSize: Int, plateau: Boolean): Float {
        val base = when {
            sampleSize < 5 -> 0.2f
            sampleSize < 14 -> 0.5f
            sampleSize < 28 -> 0.7f
            else -> 0.85f
        }
        return if (plateau) max(0f, base - 0.1f) else base
    }

    private fun buildNarrative(
        current: Float, target: Float, projected: Float,
        weeklyRate: Float, plateau: Boolean,
    ): String {
        val delta = kotlin.math.abs(projected - target)
        return when {
            delta < 0.3f -> "Sur cette tendance, tu atteins ta cible à la date visée."
            projected > target -> "Sur cette tendance, tu finiras ~${"%.1f".format(delta)}kg au-dessus de ta cible. ${if (plateau) "Plateau détecté — ajuste tes calories ou ton NEAT." else "Il faut accélérer un peu."}"
            else -> "Sur cette tendance, tu passeras ~${"%.1f".format(delta)}kg en-dessous de ta cible avant la date. Prends garde à ne pas perdre trop de muscle."
        }
    }

    private fun default(target: Float, targetDate: LocalDate, today: LocalDate) = ProjectionResult(
        projectedWeightAtTarget = target,
        trendKgPerWeek = 0f,
        etaForTarget = null,
        confidence = 0f,
        hasPlateauExpected = false,
        narrative = "Pas encore assez de pesées pour projeter.",
    )
}
