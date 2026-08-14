package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannedWeekTest {

    private fun day(dayOfWeek: Int, templateId: Long? = 1L, isRest: Boolean = false, rotationGroupId: Long? = null) =
        ProgramDayEntity(
            programId = 1,
            dayOfWeek = dayOfWeek,
            templateId = templateId,
            rotationGroupId = rotationGroupId,
            isRest = isRest,
        )

    private fun group(dayOfWeek: Int) = TemplateRotationGroupEntity(name = "Groupe $dayOfWeek", dayOfWeek = dayOfWeek)

    private fun plan(days: List<ProgramDayEntity> = emptyList(), groups: List<TemplateRotationGroupEntity> = emptyList()) =
        PlannedWeek.trainingDaysPerWeek(days, groups)

    @Test fun `sans programme ni rotation, aucun objectif`() {
        assertNull(plan())
    }

    @Test fun `un programme de trois jours donne trois`() {
        assertEquals(3, plan(days = listOf(day(1), day(3), day(5))))
    }

    @Test fun `les jours de repos ne comptent pas`() {
        val semaine = listOf(day(1), day(2, isRest = true, templateId = null), day(4), day(7, isRest = true, templateId = null))
        assertEquals(2, plan(days = semaine))
    }

    @Test fun `un jour de programme vide ne compte pas`() {
        // Ni repos ni seance : une case laissee vide a la saisie. La compter
        // gonflerait l'objectif avec des jours ou rien n'est prevu.
        assertEquals(1, plan(days = listOf(day(1), day(3, templateId = null))))
    }

    @Test fun `un jour pointant une rotation compte comme une seance`() {
        assertEquals(2, plan(days = listOf(day(1), day(3, templateId = null, rotationGroupId = 7L))))
    }

    @Test fun `deux lignes le meme jour ne font qu'une seance`() {
        assertEquals(1, plan(days = listOf(day(1), day(1, templateId = 2L))))
    }

    @Test fun `une semaine entierement en repos donne zero, pas null`() {
        // Zero et « pas de plan » doivent rester distinguables : seul le second
        // justifie un repli sur la valeur par defaut.
        val repos = (1..7).map { day(it, isRest = true, templateId = null) }
        assertEquals(0, plan(days = repos))
    }

    @Test fun `sans programme, la rotation prend le relais`() {
        assertEquals(3, plan(groups = listOf(group(2), group(4), group(6))))
    }

    @Test fun `le programme prime sur la rotation`() {
        // Meme ordre de priorite que todaysPlan : un programme actif definit le
        // planning complet, la rotation n'est qu'un repli.
        val avecLesDeux = plan(
            days = listOf(day(1), day(3)),
            groups = listOf(group(1), group(2), group(3), group(4), group(5)),
        )
        assertEquals(2, avecLesDeux)
    }

    @Test fun `la valeur par defaut reste celle d'avant`() {
        // Elle etait ecrite en dur dans AnalyticsRepository. La changer
        // modifierait l'adherence affichee a ceux qui n'ont rien planifie,
        // alors que le correctif ne porte que sur ceux qui ont un plan.
        assertEquals(5, PlannedWeek.DEFAULT_TRAINING_DAYS)
    }
}
