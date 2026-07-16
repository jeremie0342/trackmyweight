package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.domain.calc.NutritionTargets
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Regroupe la finalisation d'un onboarding : profil + objectif actif + phase de régime
 * initiale dans une transaction unique.
 */
@Singleton
class OnboardingRepository @Inject constructor(
    private val db: TrackMyWeightDatabase,
    private val userRepo: UserProfileRepository,
    private val goalRepo: GoalRepository,
    private val gymRepo: GymRepository,
    private val nutritionDao: NutritionDao,
) {
    suspend fun completeOnboarding(
        profile: UserProfileEntity,
        goal: GoalEntity,
        targets: NutritionTargets,
        gymName: String?,
        equipmentIds: Set<Long>,
    ) {
        db.withTransaction {
            userRepo.save(profile)
            goalRepo.switchActive(goal)
            if (!gymName.isNullOrBlank() && equipmentIds.isNotEmpty()) {
                gymRepo.createGymWithEquipment(gymName, equipmentIds, makeDefault = true)
            }
            nutritionDao.switchActivePhase(
                DietPhaseEntity(
                    startDate = todayLocal(),
                    phase = goal.phase.toDietPhaseKind(),
                    targetKcal = targets.targetKcal,
                    targetProteinG = targets.targetProteinG,
                    targetCarbsG = targets.targetCarbsG,
                    targetFatsG = targets.targetFatsG,
                    isActive = true,
                    createdAt = Clock.System.now(),
                )
            )
        }
    }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun GoalPhase.toDietPhaseKind(): DietPhaseKind = when (this) {
        GoalPhase.CUT -> DietPhaseKind.CUT_MODERATE
        GoalPhase.RECOMP -> DietPhaseKind.RECOMP
        GoalPhase.BULK -> DietPhaseKind.BULK_LEAN
        GoalPhase.MAINTENANCE -> DietPhaseKind.MAINTENANCE
    }
}
