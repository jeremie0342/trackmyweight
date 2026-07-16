package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneRepMaxTest {
    @Test fun `epley on 1 rep returns input weight`() {
        assertEquals(100f, OneRepMax.epley(100f, 1), 0.01f)
    }

    @Test fun `epley on 5 reps applies formula`() {
        // 100 * (1 + 5/30) = 116.67
        assertEquals(116.67f, OneRepMax.epley(100f, 5), 0.02f)
    }

    @Test fun `brzycki on 5 reps applies formula`() {
        // 100 * 36/32 = 112.5
        assertEquals(112.5f, OneRepMax.brzycki(100f, 5), 0.01f)
    }

    @Test fun `brzycki returns 0 when reps too high`() {
        assertEquals(0f, OneRepMax.brzycki(100f, 40), 0f)
    }

    @Test fun `average combines both estimators`() {
        val avg = OneRepMax.average(100f, 5)
        assertTrue(avg > 112f && avg < 117f)
    }

    @Test fun `weightForReps is inverse of epley`() {
        val oneRm = OneRepMax.epley(100f, 5)
        val computed = OneRepMax.weightForReps(oneRm, 5)
        assertEquals(100f, computed, 0.1f)
    }

    @Test fun `roundToPlate rounds to nearest 0-25`() {
        assertEquals(50.25f, OneRepMax.roundToPlate(50.3f), 0f)
        assertEquals(50f, OneRepMax.roundToPlate(50.12f), 0f)
        assertEquals(50.5f, OneRepMax.roundToPlate(50.4f), 0f)
    }
}
