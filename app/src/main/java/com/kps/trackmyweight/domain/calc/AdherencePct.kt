package com.kps.trackmyweight.domain.calc

/**
 * Calcule un pourcentage d'adhérence global sur une semaine.
 * Repose sur la somme pondérée de plusieurs signaux (0..1 chacun) :
 *  - Séances muscu réalisées vs objectif
 *  - Cardio réalisé vs objectif
 *  - Pesées loguées vs jours
 *  - Habitudes cochées vs cibles
 *  - Sommeil moyen atteint (≥7h par défaut)
 *  - Protéines cibles atteintes (jours où on est ≥ 90% de la cible)
 */
data class AdherenceInputs(
    val workoutsDone: Int,
    val workoutsTarget: Int,
    val cardioDone: Int,
    val cardioTarget: Int,
    val weighInsCount: Int,
    val daysInWindow: Int,
    val habitsDone: Int,
    val habitsPossible: Int,
    val daysWithGoodSleep: Int,
    val daysWithProteinHit: Int,
)

object AdherencePct {
    fun compute(input: AdherenceInputs): Float {
        val weights = listOf<Pair<Float, Float>>(
            ratio(input.workoutsDone, input.workoutsTarget) to 0.25f,
            ratio(input.cardioDone, input.cardioTarget) to 0.15f,
            ratio(input.weighInsCount, input.daysInWindow) to 0.10f,
            ratio(input.habitsDone, input.habitsPossible) to 0.20f,
            ratio(input.daysWithGoodSleep, input.daysInWindow) to 0.15f,
            ratio(input.daysWithProteinHit, input.daysInWindow) to 0.15f,
        )
        val totalWeight = weights.sumOf { it.second.toDouble() }.toFloat()
        val weighted = weights.sumOf { (it.first * it.second).toDouble() }.toFloat()
        return if (totalWeight > 0f) (weighted / totalWeight) * 100f else 0f
    }

    private fun ratio(done: Int, target: Int): Float =
        if (target <= 0) 0f else (done.toFloat() / target).coerceIn(0f, 1f)
}
