package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.PrKind

data class PrCandidate(
    val kind: PrKind,
    val value: Float,
    val referenceValue: Float? = null,
)

/**
 * Détecte les records personnels dépassés par un nouveau set.
 *
 * Types couverts :
 *  - MAX_WEIGHT_ANY_REPS : plus lourd jamais soulevé pour cet exercice
 *  - ONE_RM_EST : meilleur 1RM estimé
 *  - MAX_REPS_AT_WEIGHT : plus de reps qu'auparavant à un poids donné
 */
object PrDetector {

    /**
     * @param newWeightKg poids du nouveau set
     * @param newReps reps du nouveau set
     * @param currentMaxWeight PR actuel MAX_WEIGHT_ANY_REPS ou null si aucun
     * @param currentOneRm PR actuel ONE_RM_EST ou null
     * @param currentMaxRepsAtWeight pour ce poids exact, meilleures reps enregistrées, ou null
     */
    fun detect(
        newWeightKg: Float,
        newReps: Int,
        currentMaxWeight: Float?,
        currentOneRm: Float?,
        currentMaxRepsAtWeight: Int?,
    ): List<PrCandidate> {
        if (newWeightKg <= 0f || newReps <= 0) return emptyList()
        val prs = mutableListOf<PrCandidate>()

        // Poids maximal
        if (currentMaxWeight == null || newWeightKg > currentMaxWeight) {
            prs += PrCandidate(PrKind.MAX_WEIGHT_ANY_REPS, newWeightKg)
        }

        // 1RM estimé
        val newOneRm = OneRepMax.average(newWeightKg, newReps)
        if (currentOneRm == null || newOneRm > currentOneRm + 0.1f) {
            prs += PrCandidate(PrKind.ONE_RM_EST, newOneRm)
        }

        // Reps au même poids
        if (currentMaxRepsAtWeight != null && newReps > currentMaxRepsAtWeight) {
            prs += PrCandidate(
                PrKind.MAX_REPS_AT_WEIGHT,
                value = newReps.toFloat(),
                referenceValue = newWeightKg,
            )
        }

        return prs
    }
}
