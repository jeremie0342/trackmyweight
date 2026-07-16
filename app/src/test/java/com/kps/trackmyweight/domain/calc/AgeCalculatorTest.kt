package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AgeCalculatorTest {

    @Test fun `age computed correctly when birthday already passed`() {
        val age = AgeCalculator.yearsBetween(
            birthDate = LocalDate(1995, 3, 12),
            today = LocalDate(2026, 7, 15),
        )
        assertEquals(31, age)
    }

    @Test fun `age computed correctly when birthday not yet reached this year`() {
        val age = AgeCalculator.yearsBetween(
            birthDate = LocalDate(1995, 12, 20),
            today = LocalDate(2026, 7, 15),
        )
        assertEquals(30, age)
    }

    @Test fun `age zero when today is birthday`() {
        val age = AgeCalculator.yearsBetween(
            birthDate = LocalDate(2026, 7, 15),
            today = LocalDate(2026, 7, 15),
        )
        assertEquals(0, age)
    }
}
