package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.CardioType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessScoreTest {
    @Test fun `all high inputs → EXCELLENT`() {
        val v = ReadinessScore.compute(ReadinessInputs(5, 5, 5, 5))
        assertEquals(ReadinessLevel.EXCELLENT, v.level)
        assertEquals(5f, v.score, 0.01f)
        assertEquals(4, v.filledDimensions)
    }

    @Test fun `all low inputs → POOR`() {
        val v = ReadinessScore.compute(ReadinessInputs(1, 1, 1, 1))
        assertEquals(ReadinessLevel.POOR, v.level)
        assertTrue(v.advice.contains("Repos"))
    }

    @Test fun `partial inputs still produce a score`() {
        val v = ReadinessScore.compute(ReadinessInputs(4, 4, null, null))
        assertEquals(2, v.filledDimensions)
        assertEquals(4f, v.score, 0.01f)
    }

    @Test fun `no inputs → POOR + prompt`() {
        val v = ReadinessScore.compute(ReadinessInputs(null, null, null, null))
        assertEquals(ReadinessLevel.POOR, v.level)
        assertEquals(0, v.filledDimensions)
    }

    @Test fun `moderate → MODERATE`() {
        val v = ReadinessScore.compute(ReadinessInputs(3, 3, 3, 3))
        assertEquals(ReadinessLevel.MODERATE, v.level)
    }
}

class MetCaloriesTest {
    @Test fun `run 30 minutes at 84kg burns around 420 kcal`() {
        val kcal = MetCalories.estimate(CardioType.RUN, 30 * 60, 84f)
        // MET=9.8, 84 * 9.8 * 0.5 = 411.6
        assertTrue("expected 400-425 kcal, got $kcal", kcal in 400..425)
    }

    @Test fun `walk 60 minutes at 84kg burns ~294 kcal`() {
        val kcal = MetCalories.estimate(CardioType.WALK, 60 * 60, 84f)
        assertTrue("expected 280-310 kcal, got $kcal", kcal in 280..310)
    }

    @Test fun `high RPE bumps kcal up`() {
        val low = MetCalories.estimate(CardioType.BIKE, 30 * 60, 84f, avgRpe = 4f)
        val high = MetCalories.estimate(CardioType.BIKE, 30 * 60, 84f, avgRpe = 9f)
        assertTrue("high RPE should burn more than low", high > low)
    }

    @Test fun `zero duration returns zero`() {
        assertEquals(0, MetCalories.estimate(CardioType.RUN, 0, 80f))
    }

    @Test fun `strength training 60 min at 84kg ~420 kcal`() {
        val kcal = MetCalories.estimateStrengthTraining(60 * 60, 84f)
        assertEquals(420, kcal)
    }
}

class SleepDurationTest {
    @Test fun `computes minutes between bedtime and wake`() {
        val bed = Instant.parse("2026-07-15T23:00:00Z")
        val wake = Instant.parse("2026-07-16T06:30:00Z")
        assertEquals(7 * 60 + 30, SleepDuration.minutesBetween(bed, wake))
    }

    @Test fun `returns 0 when wake before or equals bedtime`() {
        val t = Instant.parse("2026-07-16T06:30:00Z")
        assertEquals(0, SleepDuration.minutesBetween(t, t))
        assertEquals(0, SleepDuration.minutesBetween(t, Instant.parse("2026-07-16T05:00:00Z")))
    }

    @Test fun `format renders hHmm`() {
        assertEquals("7h30", SleepDuration.format(450))
        assertEquals("6h05", SleepDuration.format(365))
        assertEquals("0h45", SleepDuration.format(45))
    }
}
