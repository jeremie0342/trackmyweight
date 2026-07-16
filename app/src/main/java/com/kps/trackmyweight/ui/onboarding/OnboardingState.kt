package com.kps.trackmyweight.ui.onboarding

import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.domain.calc.NutritionTargets
import kotlinx.datetime.LocalDate

/** Étapes de l'onboarding, ordonnées. */
enum class OnboardingStep(val order: Int, val progress: Float) {
    WELCOME(0, 0f),
    IDENTITY(1, 1f / 6f),
    GOAL(2, 2f / 6f),
    ACTIVITY(3, 3f / 6f),
    GYM(4, 4f / 6f),
    COACH_MODE(5, 5f / 6f),
    RECAP(6, 1f),
}

/**
 * État complet de l'onboarding. Immutable — chaque event produit une nouvelle instance.
 */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    // Identity
    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    val heightCm: Float? = null,
    val currentWeightKg: Float? = null,
    // Goal
    val targetWeightKg: Float? = null,
    val targetDate: LocalDate? = null,
    val phaseOverride: GoalPhase? = null,
    // Activity
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    // Gym
    val gymName: String = "Ma salle",
    val selectedEquipmentIds: Set<Long> = emptySet(),
    val skipGym: Boolean = false,
    // Coach mode
    val coachModeEnabled: Boolean = false,
    // Computed on recap
    val computedTargets: NutritionTargets? = null,
    // Meta
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDone: Boolean = false,
) {
    val canProceedFromIdentity: Boolean
        get() = sex != null && birthDate != null &&
            heightCm != null && heightCm > 100f && heightCm < 250f &&
            currentWeightKg != null && currentWeightKg > 30f && currentWeightKg < 300f

    val canProceedFromGoal: Boolean
        get() = targetWeightKg != null && targetWeightKg in 30f..300f &&
            targetDate != null
}
