package com.kps.trackmyweight.domain.calc

import kotlin.math.abs

data class PlateLoad(
    val platesPerSide: List<Float>,
    val achievedKg: Float,
    val deltaKg: Float,
) {
    val isExact: Boolean get() = abs(deltaKg) < 0.01f
}

/**
 * Répartition des disques par côté pour atteindre un poids cible sur barre.
 *
 * L'algorithme est glouton : on empile les plus gros disques disponibles jusqu'à
 * ne plus pouvoir approcher la cible, puis on descend dans les tailles.
 */
object PlateCalculator {

    /** Disques olympiques les plus courants (kg). */
    val DEFAULT_PLATES = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f, 0.5f)

    /**
     * @param targetKg poids total cible barre incluse
     * @param barKg poids de la barre (20 par défaut pour olympique)
     * @param availablePlates disques disponibles, en kg
     */
    fun compute(
        targetKg: Float,
        barKg: Float = 20f,
        availablePlates: List<Float> = DEFAULT_PLATES,
    ): PlateLoad {
        val need = ((targetKg - barKg) / 2f).coerceAtLeast(0f)
        val plates = availablePlates.sortedDescending()
        val chosen = mutableListOf<Float>()
        var remaining = need
        for (plate in plates) {
            while (remaining >= plate - 0.001f) {
                chosen += plate
                remaining -= plate
            }
        }
        val loadedPerSide = chosen.sum()
        val achieved = barKg + 2f * loadedPerSide
        return PlateLoad(
            platesPerSide = chosen,
            achievedKg = achieved,
            deltaKg = achieved - targetKg,
        )
    }
}
