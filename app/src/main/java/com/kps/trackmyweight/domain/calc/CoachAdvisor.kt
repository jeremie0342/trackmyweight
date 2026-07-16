package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.GoalPhase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class CoachAdviceKind {
    STAGNATION_CUT,
    STAGNATION_BULK,
    REFEED_DUE,
    DELOAD_DUE,
    ACCELERATE_LOSS,
    OVER_MRV_VOLUME,
    LOW_PROTEIN,
    LOW_SLEEP,
    LOW_READINESS,
    GOOD_PACE,
}

data class CoachAdvice(
    val kind: CoachAdviceKind,
    val title: String,
    val message: String,
    val priority: Int,   // 1 = highest
)

/**
 * Analyse la semaine et propose des ajustements. Priorisé pour éviter de spammer l'utilisateur.
 * Renvoie une liste ordonnée par priorité descendante (la plus importante d'abord).
 */
object CoachAdvisor {

    fun advise(
        phase: GoalPhase,
        weeklyRateKg: Float,
        weeksInCurrentPhase: Int,
        avgReadiness: Float,
        avgSleepMin: Float,
        avgProteinG: Float,
        proteinTargetG: Int,
        volumeVerdictsOverMrv: List<VolumeVerdict>,
        stagnationDays: Int,
        goalTargetDate: LocalDate,
        today: LocalDate,
    ): List<CoachAdvice> {
        val advices = mutableListOf<CoachAdvice>()

        // 1. Refeed / diet break après 8+ semaines de cut
        if (phase == GoalPhase.CUT && weeksInCurrentPhase >= 8) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.REFEED_DUE,
                title = "Semaine de diet break recommandée",
                message = "Tu es en cut depuis $weeksInCurrentPhase semaines. Une semaine à maintenance restaure la leptine, la thyroïde et facilite la suite.",
                priority = 1,
            )
        }

        // 2. Deload : readiness basse + fatigue accumulée
        if (avgReadiness in 0.01f..2.5f) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.DELOAD_DUE,
                title = "Deload conseillée",
                message = "Ton readiness moyen est de %.1f/5. Une semaine deload à 60%% des charges habituelles remettra tout en place.".format(avgReadiness),
                priority = 1,
            )
        }

        // 3. Sommeil bas
        if (avgSleepMin in 0.01f..(6.5f * 60f)) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.LOW_SLEEP,
                title = "Sommeil à améliorer",
                message = "Moyenne %.1fh — vise ≥ 7h30 pour optimiser récupération, hormones et perte de gras.".format(avgSleepMin / 60f),
                priority = 2,
            )
        }

        // 4. Stagnation en cut
        if (phase == GoalPhase.CUT && stagnationDays >= 14) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.STAGNATION_CUT,
                title = "Stagnation détectée",
                message = "Ton poids est stable depuis $stagnationDays jours. Retire 100 kcal ou ajoute 1000-2000 pas quotidiens.",
                priority = 2,
            )
        }

        // 5. Perte trop rapide en cut → protection muscle
        if (phase == GoalPhase.CUT && weeklyRateKg < -0.9f) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.ACCELERATE_LOSS,
                title = "Perte trop rapide",
                message = "%.2f kg/sem — tu risques de perdre du muscle. Rajoute 100-200 kcal.".format(weeklyRateKg),
                priority = 2,
            )
        }

        // 6. Volume au-dessus MRV
        volumeVerdictsOverMrv.take(2).forEach { v ->
            advices += CoachAdvice(
                kind = CoachAdviceKind.OVER_MRV_VOLUME,
                title = "Volume ${v.muscleGroup.name} au-dessus du MRV",
                message = "${v.currentSets} séries cette semaine (MRV ${v.landmarks.mrv}). Retire ${-v.suggestedSetsDelta} séries pour récupérer.",
                priority = 3,
            )
        }

        // 7. Protéines basses
        if (avgProteinG < proteinTargetG * 0.85f) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.LOW_PROTEIN,
                title = "Protéines insuffisantes",
                message = "Moyenne %.0fg vs cible %d. Ajoute une source par repas (thon boîte, wagashi, œufs, haricots).".format(avgProteinG, proteinTargetG),
                priority = 3,
            )
        }

        // 8. Bon rythme
        if (advices.isEmpty() && weeklyRateKg in -0.6f..-0.2f && phase == GoalPhase.CUT) {
            advices += CoachAdvice(
                kind = CoachAdviceKind.GOOD_PACE,
                title = "Bon rythme, continue",
                message = "Tu es dans la zone idéale. Reste régulier.",
                priority = 5,
            )
        }

        return advices.sortedBy { it.priority }
    }
}
