package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import kotlin.math.max
import kotlin.math.roundToInt

data class WarmupSet(val weightKg: Float, val reps: Int, val restSec: Int)

/**
 * Génère les séries d'échauffement pour un top set donné.
 *
 * Pour un compound lourd :
 *  1) barre à vide × 8, 60s
 *  2) 40 % × 8, 90s
 *  3) 60 % × 5, 90s
 *  4) 80 % × 3, 120s
 *  5) 90 % × 1, 120s
 *
 * Pour une isolation :
 *  1) 50 % × 10, 30s
 *  2) 75 % × 6, 60s
 */
object WarmupCalculator {

    fun generate(
        topSetKg: Float,
        mechanics: ExerciseMechanics,
        barKg: Float = 20f,
    ): List<WarmupSet> {
        if (topSetKg <= 0f) return emptyList()
        return when (mechanics) {
            ExerciseMechanics.COMPOUND -> {
                // Un compound utilise une barre. Si le top set n'excède pas la barre, pas d'échauffement.
                if (topSetKg <= barKg) emptyList()
                else listOf(
                    WarmupSet(weightKg = barKg, reps = 8, restSec = 60),
                    warmupPct(topSetKg, 0.40f, reps = 8, restSec = 90),
                    warmupPct(topSetKg, 0.60f, reps = 5, restSec = 90),
                    warmupPct(topSetKg, 0.80f, reps = 3, restSec = 120),
                    warmupPct(topSetKg, 0.90f, reps = 1, restSec = 120),
                ).filter { it.weightKg >= barKg }
            }
            ExerciseMechanics.ISOLATION -> listOf(
                warmupPct(topSetKg, 0.50f, reps = 10, restSec = 30),
                warmupPct(topSetKg, 0.75f, reps = 6, restSec = 60),
            ).filter { it.weightKg > 0f }
        }
    }

    private fun warmupPct(topSetKg: Float, pct: Float, reps: Int, restSec: Int): WarmupSet {
        val raw = topSetKg * pct
        val rounded = OneRepMax.roundToPlate(max(0f, raw))
        return WarmupSet(weightKg = rounded, reps = reps, restSec = restSec)
    }
}

/**
 * Chrono repos par défaut selon la nature de l'exercice.
 */
object RestTime {
    fun defaultSecFor(mechanics: ExerciseMechanics): Int = when (mechanics) {
        ExerciseMechanics.COMPOUND -> 180
        ExerciseMechanics.ISOLATION -> 90
    }
}
