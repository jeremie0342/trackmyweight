package com.kps.trackmyweight.domain.calc

import kotlin.math.roundToInt

/**
 * Une ligne de la table de charges de travail : « à 80 % de ton max, tu mets
 * 96 kg, et ça devrait passer sur environ 8 répétitions ».
 */
data class WorkingLoadRow(
    val percentOfMax: Int,
    val weightKg: Float,
    /** Répétitions attendues à cette intensité (indicatif). */
    val estimatedReps: Int,
    /** Répartition des disques par côté, si l'exercice se charge sur barre. */
    val plates: PlateLoad? = null,
)

/**
 * Déduit les charges de travail d'un 1RM de référence.
 *
 * Les pourcentages retenus couvrent les usages courants : force lourde (90-85 %),
 * force-hypertrophie (80-75 %), hypertrophie (70-65 %) et travail léger (60 %).
 *
 * Les répétitions attendues sont l'inverse d'Epley : `reps = 30 × (1/pct − 1)`.
 * C'est une estimation, pas une prescription — elle varie fortement selon
 * l'exercice et l'individu.
 */
object WorkingLoad {

    val DEFAULT_PERCENTAGES = listOf(90, 85, 80, 75, 70, 65, 60)

    /**
     * @param oneRmKg 1RM de référence. Une valeur nulle ou négative renvoie une liste vide.
     * @param barKg poids de la barre si [onBar], pour le calcul des disques.
     * @param onBar true si l'exercice se charge sur une barre (affiche les disques).
     */
    fun table(
        oneRmKg: Float,
        percentages: List<Int> = DEFAULT_PERCENTAGES,
        onBar: Boolean = false,
        barKg: Float = 20f,
        availablePlates: List<Float> = PlateCalculator.DEFAULT_PLATES,
    ): List<WorkingLoadRow> {
        if (oneRmKg <= 0f) return emptyList()
        return percentages
            .filter { it in 1..100 }
            .sortedDescending()
            .map { pct ->
                val raw = oneRmKg * pct / 100f
                val weight = OneRepMax.roundToPlate(raw)
                WorkingLoadRow(
                    percentOfMax = pct,
                    weightKg = weight,
                    estimatedReps = repsAtPercent(pct),
                    plates = if (onBar && weight > barKg) {
                        PlateCalculator.compute(weight, barKg, availablePlates)
                    } else {
                        null
                    },
                )
            }
    }

    /**
     * Répétitions attendues à un pourcentage du max, par inversion d'Epley.
     * Bornée à 1 : à 100 % on ne fait qu'une répétition, par définition.
     */
    fun repsAtPercent(percent: Int): Int {
        if (percent >= 100) return 1
        if (percent <= 0) return 0
        val fraction = percent / 100f
        return (30f * (1f / fraction - 1f)).roundToInt().coerceAtLeast(1)
    }

    /**
     * Progression relative entre deux mesures de max, en pourcentage.
     * Renvoie null si la référence de départ est absente ou nulle — on ne peut
     * pas exprimer une progression par rapport à rien.
     */
    fun progressionPercent(fromKg: Float?, toKg: Float?): Float? {
        if (fromKg == null || toKg == null || fromKg <= 0f) return null
        return (toKg - fromKg) / fromKg * 100f
    }
}
