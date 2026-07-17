package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.domain.calc.CoachAdvice
import com.kps.trackmyweight.domain.calc.CoachAdviceKind
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applique automatiquement les conseils du CoachAdvisor qui touchent les phases de régime.
 *  - REFEED_DUE : crée une DietPhase REFEED de 7 jours à ~maintenance TDEE, puis revient auto.
 *  - DELOAD_DUE : signale un flag persistant (semaine deload) — l'app affichera "-40% charges suggérées"
 *    dans la séance active.
 *
 * L'application est non-destructive : si l'utilisateur a déjà refusé récemment, on ne re-crée pas.
 */
@Singleton
class CoachAutoApply @Inject constructor(
    private val nutritionDao: NutritionDao,
) {

    /**
     * Applique un REFEED de 7 jours en remplaçant la phase active.
     * Après 7 jours, un check à l'ouverture de l'app doit basculer vers la phase précédente.
     */
    suspend fun applyRefeed(from: LocalDate, tdeeKcal: Int, proteinTargetG: Int) {
        val newPhase = DietPhaseEntity(
            startDate = from,
            endDate = LocalDate.fromEpochDays(from.toEpochDays() + 7),
            phase = DietPhaseKind.REFEED,
            targetKcal = tdeeKcal,
            targetProteinG = proteinTargetG,
            targetCarbsG = ((tdeeKcal * 0.55f) / 4f).toInt(),
            targetFatsG = ((tdeeKcal * 0.20f) / 9f).toInt(),
            notes = "Semaine de refeed auto-programmée par le coach.",
            isActive = true,
            createdAt = Clock.System.now(),
        )
        nutritionDao.switchActivePhase(newPhase)
    }

    /**
     * Vérifie si la phase active est un REFEED expiré → revient à la phase précédente (CUT_MODERATE par défaut).
     */
    suspend fun autoRevertExpiredRefeed(
        today: LocalDate,
        previousKcal: Int,
        previousProteinG: Int,
    ) {
        val current = nutritionDao.observeActivePhase().first() ?: return
        if (current.phase != DietPhaseKind.REFEED) return
        val endDate = current.endDate ?: return
        if (today <= endDate) return
        val restored = DietPhaseEntity(
            startDate = today,
            phase = DietPhaseKind.CUT_MODERATE,
            targetKcal = previousKcal,
            targetProteinG = previousProteinG,
            targetCarbsG = ((previousKcal * 0.40f) / 4f).toInt(),
            targetFatsG = ((previousKcal * 0.30f) / 9f).toInt(),
            notes = "Retour automatique en cut après refeed.",
            isActive = true,
            createdAt = Clock.System.now(),
        )
        nutritionDao.switchActivePhase(restored)
    }

    fun isApplicable(advice: CoachAdvice): Boolean = advice.kind in setOf(
        CoachAdviceKind.REFEED_DUE,
        CoachAdviceKind.DELOAD_DUE,
    )
}
