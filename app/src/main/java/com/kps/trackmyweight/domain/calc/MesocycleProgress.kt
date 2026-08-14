package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.ProgramEntity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Où l'on en est dans le mésocycle en cours.
 *
 * Un mésocycle est un bloc d'entraînement de quelques semaines qu'on répète :
 * savoir qu'on est en semaine 4 sur 5 change la façon d'aborder la séance —
 * c'est le moment de pousser avant la décharge, pas d'entamer un cycle.
 */
data class MesocycleProgress(
    val currentWeek: Int,
    val totalWeeks: Int,
) {
    /** Fraction écoulée, bornée à 1 quand le programme déborde de sa durée. */
    val fraction: Float
        get() = if (totalWeeks <= 0) 0f else (currentWeek.toFloat() / totalWeeks).coerceIn(0f, 1f)

    /** Dernière semaine du bloc : souvent celle de la décharge. */
    val isFinalWeek: Boolean get() = currentWeek >= totalWeeks

    /** Le bloc est terminé, il faudrait en planifier un nouveau. */
    val isOverdue: Boolean get() = currentWeek > totalWeeks

    companion object {
        /**
         * Semaine courante depuis le début du programme, 1-indexée.
         *
         * Bornée à 1 au minimum : une date antérieure au démarrage donnerait une
         * semaine 0 ou négative, ce qui n'a aucun sens à afficher.
         */
        fun of(program: ProgramEntity, today: LocalDate): MesocycleProgress {
            val elapsedDays = program.startDate.daysUntil(today)
            val week = (elapsedDays / 7) + 1
            return MesocycleProgress(
                currentWeek = week.coerceAtLeast(1),
                totalWeeks = program.mesocycleWeeks.coerceAtLeast(1),
            )
        }
    }
}
