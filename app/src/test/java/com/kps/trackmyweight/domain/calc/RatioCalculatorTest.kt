package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatioCalculatorTest {

    @Test fun `whtr computed correctly and categorized`() {
        assertEquals(0.5f, RatioCalculator.whtr(90f, 180f)!!, 0.001f)
        assertEquals(WhtrCategory.OVERWEIGHT, RatioCalculator.categorizeWhtr(0.51f))
        assertEquals(WhtrCategory.HEALTHY, RatioCalculator.categorizeWhtr(0.45f))
        assertEquals(WhtrCategory.UNDERWEIGHT, RatioCalculator.categorizeWhtr(0.35f))
        assertEquals(WhtrCategory.ABDOMINAL_OBESITY, RatioCalculator.categorizeWhtr(0.60f))
    }

    @Test fun `whr computed correctly`() {
        assertEquals(0.9f, RatioCalculator.whr(90f, 100f)!!, 0.001f)
    }

    @Test fun `null inputs produce null ratios`() {
        assertNull(RatioCalculator.whtr(null, 180f))
        assertNull(RatioCalculator.whr(90f, null))
        assertNull(RatioCalculator.whr(null, 100f))
    }
}
