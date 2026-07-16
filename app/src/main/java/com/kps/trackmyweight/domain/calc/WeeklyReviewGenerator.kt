package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import kotlin.math.abs

data class WeeklySummary(
    val weekStart: LocalDate,
    val adherencePct: Float,
    val weightDeltaKg: Float,
    val sessionsCount: Int,
    val cardioCount: Int,
    val avgProteinG: Float,
    val avgKcal: Float,
    val avgSleepMin: Float,
    val avgReadiness: Float,
    val totalSteps: Int,
    val totalVolumeKg: Float,
    val narrative: String,
)

/**
 * Génère la synthèse texte d'une semaine à partir des métriques calculées.
 */
object WeeklyReviewGenerator {

    fun generate(
        weekStart: LocalDate,
        adherencePct: Float,
        weightDeltaKg: Float,
        sessionsCount: Int,
        cardioCount: Int,
        avgProteinG: Float,
        avgKcal: Float,
        avgSleepMin: Float,
        avgReadiness: Float,
        totalSteps: Int,
        totalVolumeKg: Float,
        proteinTarget: Int,
        kcalTarget: Int,
    ): WeeklySummary {
        val sb = StringBuilder()

        // Adhérence
        sb.append("Adhérence : ${adherencePct.toInt()}%. ")

        // Poids
        val trend = when {
            weightDeltaKg <= -0.6f -> "Bonne perte ce sem."
            weightDeltaKg <= -0.2f -> "Perte modérée cette semaine."
            weightDeltaKg < 0.2f -> "Poids stable."
            weightDeltaKg < 0.6f -> "Léger gain cette semaine."
            else -> "Gain notable cette semaine."
        }
        sb.append("$trend (${"%+.1f".format(weightDeltaKg)}kg). ")

        // Séances
        sb.append("$sessionsCount séance${if (sessionsCount > 1) "s" else ""} muscu")
        if (cardioCount > 0) sb.append(" + $cardioCount cardio")
        sb.append(". ")

        // Nutrition
        val proteinPct = if (proteinTarget > 0) (avgProteinG / proteinTarget * 100).toInt() else 0
        sb.append("Protéines à $proteinPct% de cible en moyenne. ")

        // Sommeil
        val sleepH = avgSleepMin / 60f
        if (sleepH > 0f) {
            when {
                sleepH >= 7.5f -> sb.append("Sommeil solide (${"%.1f".format(sleepH)}h). ")
                sleepH >= 6.5f -> sb.append("Sommeil correct (${"%.1f".format(sleepH)}h). ")
                else -> sb.append("Sommeil insuffisant (${"%.1f".format(sleepH)}h) — priorité à améliorer. ")
            }
        }

        // Readiness
        if (avgReadiness > 0f) {
            when {
                avgReadiness >= 4f -> sb.append("Forme excellente en moyenne. ")
                avgReadiness >= 3f -> sb.append("Forme moyenne. ")
                else -> sb.append("Forme basse — envisage un deload. ")
            }
        }

        return WeeklySummary(
            weekStart = weekStart,
            adherencePct = adherencePct,
            weightDeltaKg = weightDeltaKg,
            sessionsCount = sessionsCount,
            cardioCount = cardioCount,
            avgProteinG = avgProteinG,
            avgKcal = avgKcal,
            avgSleepMin = avgSleepMin,
            avgReadiness = avgReadiness,
            totalSteps = totalSteps,
            totalVolumeKg = totalVolumeKg,
            narrative = sb.toString().trim(),
        )
    }
}
