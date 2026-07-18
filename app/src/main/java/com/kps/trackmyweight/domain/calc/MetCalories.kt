package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.CardioType
import kotlin.math.roundToInt

/**
 * Estimation des calories brûlées d'après les tables MET (Compendium of Physical Activities).
 * Formule : kcal = MET × poids_kg × durée_h
 *
 * Les valeurs choisies correspondent à une intensité "modérée à soutenue" typique.
 * L'utilisateur peut ajuster manuellement le RPE ce qui pourrait moduler le MET
 * (implémenté ici via un facteur RPE optionnel).
 */
object MetCalories {

    private fun baseMet(type: CardioType): Float = when (type) {
        CardioType.WALK -> 3.5f
        CardioType.RUN -> 9.8f
        CardioType.BIKE -> 7.5f
        CardioType.ROWER -> 7.0f
        CardioType.ELLIPTICAL -> 5.0f
        CardioType.JUMP_ROPE -> 12.3f
        CardioType.HIIT -> 10.0f
        CardioType.LISS -> 5.0f
        CardioType.SWIM -> 8.0f
        CardioType.BATTLE_ROPES -> 8.3f
        CardioType.JUMPING_JACKS -> 8.0f
        CardioType.BURPEES -> 10.0f
        CardioType.MOUNTAIN_CLIMBERS -> 8.0f
        CardioType.STAIR_MASTER -> 9.0f
        CardioType.OTHER -> 6.0f
    }

    /**
     * @param type type de cardio
     * @param durationSec durée totale de l'effort
     * @param bodyWeightKg poids corporel actuel
     * @param avgRpe RPE moyen 1-10 (module le MET de ±20%)
     */
    fun estimate(
        type: CardioType,
        durationSec: Int,
        bodyWeightKg: Float,
        avgRpe: Float? = null,
    ): Int {
        if (durationSec <= 0 || bodyWeightKg <= 0f) return 0
        val met = baseMet(type)
        val rpeFactor = avgRpe?.let { rpe ->
            // RPE 6-7 = 1.0, <6 abaisse, >7 monte, borné ±20%
            (0.8f + ((rpe - 6f) / 4f) * 0.4f).coerceIn(0.8f, 1.2f)
        } ?: 1f
        val hours = durationSec / 3600f
        return (met * rpeFactor * bodyWeightKg * hours).roundToInt()
    }

    /** Musculation en général ≈ 5 MET. Utile pour compter l'énergie brûlée en séance. */
    fun estimateStrengthTraining(durationSec: Int, bodyWeightKg: Float): Int {
        if (durationSec <= 0 || bodyWeightKg <= 0f) return 0
        val hours = durationSec / 3600f
        return (5f * bodyWeightKg * hours).roundToInt()
    }
}
