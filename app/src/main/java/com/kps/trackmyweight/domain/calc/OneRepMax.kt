package com.kps.trackmyweight.domain.calc

import kotlin.math.roundToInt

/**
 * Estimateurs de 1RM à partir d'une série (poids + reps).
 * Epley : le plus commun, précis dans 1-10 reps.
 * Brzycki : légèrement plus précis pour ≤5 reps.
 */
object OneRepMax {

    /** 1RM = weight * (1 + reps/30) */
    fun epley(weightKg: Float, reps: Int): Float {
        if (reps <= 0 || weightKg <= 0f) return 0f
        if (reps == 1) return weightKg
        return weightKg * (1f + reps / 30f)
    }

    /** 1RM = weight * 36 / (37 - reps). Non défini pour reps ≥ 37. */
    fun brzycki(weightKg: Float, reps: Int): Float {
        if (reps <= 0 || weightKg <= 0f || reps >= 37) return 0f
        if (reps == 1) return weightKg
        return weightKg * 36f / (37f - reps)
    }

    /** Moyenne des deux estimateurs (plus robuste). */
    fun average(weightKg: Float, reps: Int): Float {
        val e = epley(weightKg, reps)
        val b = brzycki(weightKg, reps)
        return if (b > 0f) (e + b) / 2f else e
    }

    /** Poids nécessaire pour X reps à partir d'un 1RM connu (formule Epley inverse). */
    fun weightForReps(oneRmKg: Float, reps: Int): Float {
        if (oneRmKg <= 0f || reps <= 0) return 0f
        if (reps == 1) return oneRmKg
        return oneRmKg / (1f + reps / 30f)
    }

    /** Arrondi au 0.25 kg le plus proche (pour la muscu). */
    fun roundToPlate(kg: Float): Float = (kg * 4f).roundToInt() / 4f
}
