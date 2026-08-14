package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity

/**
 * Combien de séances la semaine prévoit, d'après le planning de l'utilisateur.
 *
 * L'adhérence hebdomadaire comparait les séances faites à un objectif écrit en
 * dur — 5 — alors que le programme et la rotation savent exactement ce qui est
 * prévu. Quelqu'un qui s'entraîne 3 fois par semaine par choix se voyait donc
 * en permanence à 3/5.
 *
 * L'ordre de priorité reprend celui de `todaysPlan` : un programme actif définit
 * le planning complet et prime ; à défaut, la rotation autonome ; à défaut,
 * rien, et l'appelant décide quoi faire de ce vide.
 */
object PlannedWeek {

    /**
     * Objectif retenu quand l'utilisateur n'a ni programme ni rotation.
     *
     * C'est la valeur qui était codée en dur. La garder évite de changer
     * silencieusement l'adhérence affichée à ceux qui n'ont rien planifié — le
     * correctif porte sur ceux qui ont un plan, pas sur les autres.
     */
    const val DEFAULT_TRAINING_DAYS = 5

    /**
     * Nombre de jours d'entraînement prévus par semaine, ou `null` si aucun
     * planning n'existe.
     *
     * Un `null` explicite plutôt qu'un 0 : « aucun plan » et « plan de repos
     * complet » sont deux situations différentes, et seule la première justifie
     * un repli sur une valeur par défaut.
     */
    fun trainingDaysPerWeek(
        programDays: List<ProgramDayEntity>,
        rotationGroups: List<TemplateRotationGroupEntity>,
    ): Int? = fromProgram(programDays) ?: fromRotation(rotationGroups)

    /**
     * Un jour compte s'il n'est pas marqué repos **et** s'il désigne quelque
     * chose à faire. Un jour de programme sans template ni rotation est une
     * case laissée vide à la saisie, pas une séance.
     */
    private fun fromProgram(days: List<ProgramDayEntity>): Int? {
        if (days.isEmpty()) return null
        return days
            .filter { !it.isRest && (it.templateId != null || it.rotationGroupId != null) }
            // Distinct : rien n'empeche deux lignes sur le meme jour, ca reste
            // une seule seance dans la semaine.
            .distinctBy { it.dayOfWeek }
            .size
    }

    private fun fromRotation(groups: List<TemplateRotationGroupEntity>): Int? {
        if (groups.isEmpty()) return null
        return groups.distinctBy { it.dayOfWeek }.size
    }
}
