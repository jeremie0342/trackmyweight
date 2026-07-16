package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cible nutritionnelle personnalisée. Pure data, calculée depuis les inputs.
 */
data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val targetKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatsG: Int,
    val weeklyRateKg: Float,
    val recommendedPhase: GoalPhase,
) {
    val deficitKcal: Int get() = tdee - targetKcal
}

/**
 * Calculs nutritionnels 100% pure Kotlin (aucune dépendance Android). Testés en JVM.
 *
 * Formules :
 *  - BMR : Mifflin-St Jeor (le plus précis en pratique clinique)
 *  - TDEE : BMR × facteur d'activité
 *  - Protéines : 1.6-2.2 g/kg poids corporel, ajusté selon phase
 *  - Lipides : ≥ 0.8 g/kg, cible 25-30% des kcal totales
 *  - Glucides : reste des kcal disponibles
 *  - Déficit / surplus : borné pour préserver la masse musculaire
 */
object NutritionCalculator {

    /** Mifflin-St Jeor (1990). */
    fun bmr(sex: Sex, weightKg: Float, heightCm: Float, ageYears: Int): Int {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * ageYears
        val adjusted = when (sex) {
            Sex.MALE -> base + 5f
            Sex.FEMALE -> base - 161f
        }
        return adjusted.roundToInt()
    }

    fun activityFactor(level: ActivityLevel): Float = when (level) {
        ActivityLevel.SEDENTARY -> 1.2f
        ActivityLevel.LIGHTLY_ACTIVE -> 1.375f
        ActivityLevel.MODERATELY_ACTIVE -> 1.55f
        ActivityLevel.VERY_ACTIVE -> 1.725f
        ActivityLevel.EXTRA_ACTIVE -> 1.9f
    }

    fun tdee(bmr: Int, level: ActivityLevel): Int =
        (bmr * activityFactor(level)).roundToInt()

    /**
     * Cible protéines selon la phase.
     *  - Cut : 2.0-2.2 g/kg (préserver masse musculaire malgré déficit)
     *  - Recomp : 1.8-2.0 g/kg
     *  - Bulk : 1.6-1.8 g/kg (suffisant, l'excédent devient graisse)
     *  - Maintenance : 1.6 g/kg
     */
    fun proteinTargetG(weightKg: Float, phase: GoalPhase): Int = when (phase) {
        GoalPhase.CUT -> (weightKg * 2.1f).roundToInt()
        GoalPhase.RECOMP -> (weightKg * 1.9f).roundToInt()
        GoalPhase.BULK -> (weightKg * 1.7f).roundToInt()
        GoalPhase.MAINTENANCE -> (weightKg * 1.6f).roundToInt()
    }

    /**
     * Rythme hebdomadaire recommandé selon écart poids/cible et durée disponible.
     * Borné pour préserver la masse musculaire :
     *  - Cut : max 1% du poids corporel par semaine, soft cap 0.8kg
     *  - Bulk : max 0.5% par semaine, soft cap 0.4kg
     *  - Recomp : 0.2-0.3kg par semaine
     */
    fun computeWeeklyRateKg(
        currentWeightKg: Float,
        targetWeightKg: Float,
        today: LocalDate,
        targetDate: LocalDate,
    ): Float {
        val diff = targetWeightKg - currentWeightKg
        val days = today.daysUntil(targetDate).coerceAtLeast(7)
        val weeks = days / 7f
        val rawRate = diff / weeks

        val maxCutRate = -min(currentWeightKg * 0.01f, 0.8f)
        val maxBulkRate = min(currentWeightKg * 0.005f, 0.4f)
        return rawRate.coerceIn(maxCutRate, maxBulkRate)
    }

    /** Suggère la phase optimale selon l'écart poids/cible. */
    fun suggestPhase(currentWeightKg: Float, targetWeightKg: Float): GoalPhase {
        val diff = targetWeightKg - currentWeightKg
        val diffPct = diff / currentWeightKg
        return when {
            diffPct <= -0.03f -> GoalPhase.CUT
            diffPct >= 0.03f -> GoalPhase.BULK
            kotlin.math.abs(diffPct) < 0.005f -> GoalPhase.MAINTENANCE
            else -> GoalPhase.RECOMP
        }
    }

    /**
     * Calcule les cibles complètes à partir d'un poids, taille, âge, activité et objectif.
     * L'utilisateur peut ensuite les ajuster manuellement.
     */
    fun compute(
        sex: Sex,
        weightKg: Float,
        heightCm: Float,
        ageYears: Int,
        activityLevel: ActivityLevel,
        targetWeightKg: Float,
        today: LocalDate,
        targetDate: LocalDate,
        overridePhase: GoalPhase? = null,
    ): NutritionTargets {
        val bmr = bmr(sex, weightKg, heightCm, ageYears)
        val tdee = tdee(bmr, activityLevel)
        val phase = overridePhase ?: suggestPhase(weightKg, targetWeightKg)
        val weeklyRate = computeWeeklyRateKg(weightKg, targetWeightKg, today, targetDate)

        // Un kg de graisse ≈ 7700 kcal. Le déficit/surplus quotidien découle du rythme visé.
        val dailyKcalDelta = ((weeklyRate * 7700f) / 7f).roundToInt()
        val targetKcal = when (phase) {
            GoalPhase.MAINTENANCE -> tdee
            GoalPhase.RECOMP -> tdee - min(300, max(100, -dailyKcalDelta))
            GoalPhase.CUT -> (tdee + dailyKcalDelta).coerceAtLeast((bmr * 1.1f).roundToInt())
            GoalPhase.BULK -> (tdee + dailyKcalDelta).coerceAtMost(tdee + 500)
        }

        val protein = proteinTargetG(weightKg, phase)
        val fats = max((weightKg * 0.9f).roundToInt(), (targetKcal * 0.25f / 9f).roundToInt())
        val kcalFromPandF = protein * 4 + fats * 9
        val carbs = ((targetKcal - kcalFromPandF) / 4f).coerceAtLeast(0f).roundToInt()

        return NutritionTargets(
            bmr = bmr,
            tdee = tdee,
            targetKcal = targetKcal,
            targetProteinG = protein,
            targetCarbsG = carbs,
            targetFatsG = fats,
            weeklyRateKg = weeklyRate,
            recommendedPhase = phase,
        )
    }
}
