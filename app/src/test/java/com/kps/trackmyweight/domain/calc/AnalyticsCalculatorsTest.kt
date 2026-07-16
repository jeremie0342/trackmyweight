package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PearsonCorrelationTest {
    @Test fun `perfect positive returns 1`() {
        val r = PearsonCorrelation.compute(listOf(1f, 2f, 3f, 4f), listOf(2f, 4f, 6f, 8f))
        assertEquals(1f, r.r, 0.001f)
        assertEquals(CorrelationStrength.STRONG, r.strength)
    }

    @Test fun `perfect negative returns -1`() {
        val r = PearsonCorrelation.compute(listOf(1f, 2f, 3f, 4f), listOf(8f, 6f, 4f, 2f))
        assertEquals(-1f, r.r, 0.001f)
    }

    @Test fun `constant series returns 0`() {
        val r = PearsonCorrelation.compute(listOf(5f, 5f, 5f, 5f), listOf(1f, 2f, 3f, 4f))
        assertEquals(0f, r.r, 0.001f)
        assertEquals(CorrelationStrength.NEGLIGIBLE, r.strength)
    }

    @Test fun `moderate correlation is classified`() {
        val r = PearsonCorrelation.compute(
            listOf(1f, 2f, 3f, 4f, 5f, 6f),
            listOf(1.2f, 1.8f, 3.5f, 3.9f, 5.1f, 5.5f),
        )
        assertTrue(r.r > 0.9f)
    }

    @Test fun `single point returns negligible`() {
        val r = PearsonCorrelation.compute(listOf(1f), listOf(2f))
        assertEquals(CorrelationStrength.NEGLIGIBLE, r.strength)
    }
}

class AdherencePctTest {
    @Test fun `perfect adherence returns 100`() {
        val a = AdherencePct.compute(AdherenceInputs(
            workoutsDone = 6, workoutsTarget = 6,
            cardioDone = 2, cardioTarget = 2,
            weighInsCount = 7, daysInWindow = 7,
            habitsDone = 40, habitsPossible = 40,
            daysWithGoodSleep = 7, daysWithProteinHit = 7,
        ))
        assertEquals(100f, a, 0.5f)
    }

    @Test fun `partial adherence weights correctly`() {
        val a = AdherencePct.compute(AdherenceInputs(
            workoutsDone = 3, workoutsTarget = 6,
            cardioDone = 1, cardioTarget = 2,
            weighInsCount = 5, daysInWindow = 7,
            habitsDone = 20, habitsPossible = 40,
            daysWithGoodSleep = 3, daysWithProteinHit = 4,
        ))
        assertTrue("expected 40-60%%, got $a", a in 40f..60f)
    }

    @Test fun `zero targets ignored gracefully`() {
        val a = AdherencePct.compute(AdherenceInputs(
            workoutsDone = 0, workoutsTarget = 0,
            cardioDone = 0, cardioTarget = 0,
            weighInsCount = 0, daysInWindow = 0,
            habitsDone = 0, habitsPossible = 0,
            daysWithGoodSleep = 0, daysWithProteinHit = 0,
        ))
        assertEquals(0f, a, 0.01f)
    }
}

class NonLinearProjectionTest {
    private val start = LocalDate(2026, 7, 1)

    @Test fun `with steady loss projects near target`() {
        val points = (0..30).map { DatedValue(LocalDate.fromEpochDays(start.toEpochDays() + it), 84f - it * 0.05f) }
        val result = NonLinearProjection.project(
            points = points,
            targetWeightKg = 75f,
            targetDate = LocalDate(2027, 1, 1),
            today = LocalDate.fromEpochDays(start.toEpochDays() + 30),
        )
        assertTrue("projected within 5kg of target", kotlin.math.abs(result.projectedWeightAtTarget - 75f) < 5f)
        assertTrue("has ETA", result.etaForTarget != null)
        assertTrue("confidence should be positive", result.confidence > 0f)
    }

    @Test fun `insufficient data returns default`() {
        val result = NonLinearProjection.project(
            points = emptyList(),
            targetWeightKg = 75f,
            targetDate = LocalDate(2027, 1, 1),
            today = start,
        )
        assertEquals(0f, result.confidence, 0.001f)
    }

    @Test fun `plateau detected when weekly rate near 0`() {
        val points = (0..30).map {
            DatedValue(LocalDate.fromEpochDays(start.toEpochDays() + it), 80f + (if (it % 2 == 0) 0.1f else -0.1f))
        }
        val result = NonLinearProjection.project(points, 75f, LocalDate(2027, 1, 1), LocalDate.fromEpochDays(start.toEpochDays() + 30))
        assertTrue(result.hasPlateauExpected)
    }
}

class WeeklyReviewGeneratorTest {
    @Test fun `narrative mentions all key metrics`() {
        val s = WeeklyReviewGenerator.generate(
            weekStart = LocalDate(2026, 7, 13),
            adherencePct = 85f,
            weightDeltaKg = -0.4f,
            sessionsCount = 4,
            cardioCount = 2,
            avgProteinG = 145f,
            avgKcal = 2600f,
            avgSleepMin = 450f,
            avgReadiness = 4f,
            totalSteps = 65000,
            totalVolumeKg = 12000f,
            proteinTarget = 150,
            kcalTarget = 2500,
        )
        assertTrue(s.narrative.contains("85%"))
        assertTrue(s.narrative.contains("séance"))
        assertTrue(s.narrative.contains("cardio"))
    }

    @Test fun `narrative flags low sleep`() {
        val s = WeeklyReviewGenerator.generate(
            weekStart = LocalDate(2026, 7, 13),
            adherencePct = 80f, weightDeltaKg = 0f,
            sessionsCount = 3, cardioCount = 1,
            avgProteinG = 140f, avgKcal = 2500f,
            avgSleepMin = 360f,   // 6h
            avgReadiness = 3f, totalSteps = 50000, totalVolumeKg = 10000f,
            proteinTarget = 150, kcalTarget = 2500,
        )
        assertTrue(s.narrative.contains("insuffisant"))
    }
}

class CoachAdvisorTest {
    private val today = LocalDate(2026, 7, 16)
    private val goalDate = LocalDate(2027, 1, 15)

    @Test fun `refeed advice when 8+ weeks in cut`() {
        val advices = CoachAdvisor.advise(
            phase = GoalPhase.CUT,
            weeklyRateKg = -0.3f,
            weeksInCurrentPhase = 8,
            avgReadiness = 4f, avgSleepMin = 480f,
            avgProteinG = 150f, proteinTargetG = 150,
            volumeVerdictsOverMrv = emptyList(),
            stagnationDays = 0,
            goalTargetDate = goalDate, today = today,
        )
        assertTrue(advices.any { it.kind == CoachAdviceKind.REFEED_DUE })
    }

    @Test fun `deload advice when readiness is low`() {
        val advices = CoachAdvisor.advise(
            phase = GoalPhase.CUT,
            weeklyRateKg = -0.3f,
            weeksInCurrentPhase = 3,
            avgReadiness = 2f, avgSleepMin = 480f,
            avgProteinG = 150f, proteinTargetG = 150,
            volumeVerdictsOverMrv = emptyList(),
            stagnationDays = 0,
            goalTargetDate = goalDate, today = today,
        )
        assertTrue(advices.any { it.kind == CoachAdviceKind.DELOAD_DUE })
    }

    @Test fun `stagnation advice in cut after 14 days`() {
        val advices = CoachAdvisor.advise(
            phase = GoalPhase.CUT,
            weeklyRateKg = 0f,
            weeksInCurrentPhase = 3,
            avgReadiness = 4f, avgSleepMin = 480f,
            avgProteinG = 150f, proteinTargetG = 150,
            volumeVerdictsOverMrv = emptyList(),
            stagnationDays = 15,
            goalTargetDate = goalDate, today = today,
        )
        assertTrue(advices.any { it.kind == CoachAdviceKind.STAGNATION_CUT })
    }

    @Test fun `good pace advice when nothing wrong`() {
        val advices = CoachAdvisor.advise(
            phase = GoalPhase.CUT,
            weeklyRateKg = -0.4f,
            weeksInCurrentPhase = 3,
            avgReadiness = 4f, avgSleepMin = 480f,
            avgProteinG = 155f, proteinTargetG = 150,
            volumeVerdictsOverMrv = emptyList(),
            stagnationDays = 0,
            goalTargetDate = goalDate, today = today,
        )
        assertTrue(advices.any { it.kind == CoachAdviceKind.GOOD_PACE })
    }
}
