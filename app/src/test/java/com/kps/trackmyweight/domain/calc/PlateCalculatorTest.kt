package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {
    @Test fun `computes exact loading for 80kg with standard plates`() {
        val load = PlateCalculator.compute(80f)
        assertEquals(80f, load.achievedKg, 0.01f)
        assertTrue(load.isExact)
        // 80 - 20 = 60, /2 = 30 per side, best: 25 + 5 = 30
        assertEquals(listOf(25f, 5f), load.platesPerSide)
    }

    @Test fun `computes exact for 82-5kg`() {
        val load = PlateCalculator.compute(82.5f)
        assertEquals(82.5f, load.achievedKg, 0.01f)
        // 62.5 / 2 = 31.25 per side: 25 + 5 + 1.25
        assertEquals(listOf(25f, 5f, 1.25f), load.platesPerSide)
    }

    @Test fun `bar only when target equals bar`() {
        val load = PlateCalculator.compute(20f)
        assertTrue(load.platesPerSide.isEmpty())
        assertEquals(20f, load.achievedKg, 0.01f)
    }

    @Test fun `handles target lower than bar`() {
        val load = PlateCalculator.compute(15f)
        assertTrue(load.platesPerSide.isEmpty())
    }

    @Test fun `respects available plates`() {
        // Sans disques de 1.25, cible 82.5 non atteinte exactement
        val load = PlateCalculator.compute(
            targetKg = 82.5f,
            availablePlates = listOf(20f, 10f, 5f, 2.5f),
        )
        assertTrue(!load.isExact)
    }
}
