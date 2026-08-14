package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.ProgramEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MesocycleProgressTest {

    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun program(start: LocalDate, weeks: Int = 5) = ProgramEntity(
        id = 1,
        name = "Bloc",
        isCoachProgram = false,
        startDate = start,
        mesocycleWeeks = weeks,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test fun `first day is week one`() {
        val start = LocalDate(2026, 8, 3)
        assertEquals(1, MesocycleProgress.of(program(start), start).currentWeek)
    }

    @Test fun `last day of week one is still week one`() {
        val start = LocalDate(2026, 8, 3)
        assertEquals(1, MesocycleProgress.of(program(start), LocalDate(2026, 8, 9)).currentWeek)
    }

    @Test fun `day eight starts week two`() {
        val start = LocalDate(2026, 8, 3)
        assertEquals(2, MesocycleProgress.of(program(start), LocalDate(2026, 8, 10)).currentWeek)
    }

    @Test fun `a date before the start still reads as week one`() {
        // Une semaine 0 ou negative n'a aucun sens a afficher.
        val start = LocalDate(2026, 8, 3)
        assertEquals(1, MesocycleProgress.of(program(start), LocalDate(2026, 7, 20)).currentWeek)
    }

    @Test fun `final week is flagged`() {
        val start = LocalDate(2026, 8, 3)
        val week5 = LocalDate(2026, 8, 31)
        val progress = MesocycleProgress.of(program(start, weeks = 5), week5)
        assertEquals(5, progress.currentWeek)
        assertTrue(progress.isFinalWeek)
        assertFalse(progress.isOverdue)
    }

    @Test fun `going past the block is flagged as overdue`() {
        val start = LocalDate(2026, 8, 3)
        val week7 = LocalDate(2026, 9, 14)
        val progress = MesocycleProgress.of(program(start, weeks = 5), week7)
        assertEquals(7, progress.currentWeek)
        assertTrue(progress.isOverdue)
    }

    @Test fun `fraction is capped at one when overdue`() {
        val start = LocalDate(2026, 8, 3)
        val progress = MesocycleProgress.of(program(start, weeks = 4), LocalDate(2026, 9, 21))
        assertEquals(1f, progress.fraction, 0.001f)
    }

    @Test fun `fraction is proportional mid block`() {
        val start = LocalDate(2026, 8, 3)
        val week2 = LocalDate(2026, 8, 10)
        assertEquals(0.5f, MesocycleProgress.of(program(start, weeks = 4), week2).fraction, 0.001f)
    }

    @Test fun `a zero week program does not divide by zero`() {
        val start = LocalDate(2026, 8, 3)
        val progress = MesocycleProgress.of(program(start, weeks = 0), start)
        assertEquals(1, progress.totalWeeks)
        assertEquals(1f, progress.fraction, 0.001f)
    }
}
