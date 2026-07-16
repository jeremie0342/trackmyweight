package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class BmiCalculatorTest {
    @Test fun `bmi is weight over height squared`() {
        // 84 kg, 180 cm → 84 / 1.8² = 25.9
        assertEquals(25.93f, BmiCalculator.compute(84f, 180f), 0.01f)
    }

    @Test fun `bmi 70 kg 175 cm rounds to 22`() {
        assertEquals(22.86f, BmiCalculator.compute(70f, 175f), 0.01f)
    }

    @Test fun `categorize follows WHO cutoffs`() {
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCalculator.categorize(18.0f))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categorize(22.5f))
        assertEquals(BmiCategory.NORMAL, BmiCalculator.categorize(24.9f))
        assertEquals(BmiCategory.OVERWEIGHT, BmiCalculator.categorize(25f))
        assertEquals(BmiCategory.OVERWEIGHT, BmiCalculator.categorize(29.9f))
        assertEquals(BmiCategory.OBESITY_CLASS_I, BmiCalculator.categorize(30f))
        assertEquals(BmiCategory.OBESITY_CLASS_II, BmiCalculator.categorize(37f))
        assertEquals(BmiCategory.OBESITY_CLASS_III, BmiCalculator.categorize(42f))
    }
}
