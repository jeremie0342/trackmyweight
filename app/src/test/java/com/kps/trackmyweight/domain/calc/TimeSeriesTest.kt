package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeSeriesTest {

    @Test fun `trailing moving average uses window of days not samples`() {
        val start = LocalDate(2026, 7, 1)
        val points = (0..9).map { DatedValue(start.plusDaysInt(it), 84f - it * 0.1f) }
        val smoothed = TimeSeries.trailingMovingAverage(points, windowDays = 7)
        assertEquals(10, smoothed.size)
        // Le premier point n'a pas d'historique → smoothed = raw
        assertEquals(84f, smoothed.first().smoothed, 0.001f)
        // Le 7e point moyenne les 7 premières valeurs → 84 + 83.9 + ... + 83.4 → 83.7
        assertEquals(83.7f, smoothed[6].smoothed, 0.02f)
    }

    @Test fun `linear fit recovers exact slope on synthetic data`() {
        val start = LocalDate(2026, 7, 1)
        val points = (0..20).map { DatedValue(start.plusDaysInt(it), 84f - it * 0.05f) }
        val (slope, intercept) = TimeSeries.linearFit(points)!!
        assertEquals(-0.05f, slope, 0.001f)
        assertEquals(84f, intercept, 0.02f)
    }

    @Test fun `linear fit returns null on insufficient data`() {
        assertNull(TimeSeries.linearFit(emptyList()))
        assertNull(TimeSeries.linearFit(listOf(DatedValue(LocalDate(2026, 7, 1), 84f))))
    }

    @Test fun `projection extrapolates linearly`() {
        val start = LocalDate(2026, 7, 1)
        val points = (0..9).map { DatedValue(start.plusDaysInt(it), 84f - it * 0.1f) }
        val projected = TimeSeries.projectAt(points, start.plusDaysInt(30))!!
        // slope = -0.1 kg/day → à J+30 : 84 - 3 = 81
        assertEquals(81f, projected, 0.05f)
    }

    @Test fun `etaFor returns date when target reachable`() {
        val start = LocalDate(2026, 7, 1)
        val points = (0..9).map { DatedValue(start.plusDaysInt(it), 84f - it * 0.1f) }
        // slope = -0.1 → pour atteindre 80 depuis 84 : 40 jours
        val eta = TimeSeries.etaFor(points, 80f)
        assertNotNull(eta)
        assertEquals(start.plusDaysInt(40), eta)
    }

    @Test fun `etaFor returns null if slope goes wrong direction`() {
        val start = LocalDate(2026, 7, 1)
        // Poids qui monte, cible plus basse → jamais atteint
        val points = (0..9).map { DatedValue(start.plusDaysInt(it), 84f + it * 0.1f) }
        assertNull(TimeSeries.etaFor(points, 80f))
    }

    @Test fun `stagnation detected when net change is under threshold over enough days`() {
        val today = LocalDate(2026, 7, 16)
        val points = (0..13).map {
            DatedValue(today.plusDaysInt(-13 + it), 84f + (if (it % 2 == 0) 0.1f else -0.1f))
        }
        val verdict = StagnationDetector.detect(points, windowDays = 14, thresholdAbs = 0.3f, today = today)
        assertTrue("should be stagnating: $verdict", verdict.isStagnating)
    }

    @Test fun `stagnation not detected when losing meaningfully`() {
        val today = LocalDate(2026, 7, 16)
        val points = (0..13).map { DatedValue(today.plusDaysInt(-13 + it), 84f - it * 0.15f) }
        val verdict = StagnationDetector.detect(points, windowDays = 14, thresholdAbs = 0.3f, today = today)
        assertTrue("should NOT be stagnating: $verdict", !verdict.isStagnating)
    }
}

private fun LocalDate.plusDaysInt(days: Int): LocalDate =
    LocalDate.fromEpochDays(this.toEpochDays() + days)
