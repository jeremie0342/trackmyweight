package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrelationInsightsTest {

    private fun days(n: Int): List<LocalDate> =
        (0 until n).map { LocalDate.fromEpochDays(20000 + it) }

    private fun series(metric: DailyMetric, values: List<Float>): Pair<DailyMetric, Map<LocalDate, Float>> =
        metric to days(values.size).zip(values).toMap()

    @Test fun `no insight below the minimum sample size`() {
        // Pearson sur peu de points donne facilement un r extreme par hasard.
        val insights = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.SLEEP_HOURS, listOf(6f, 7f, 8f, 9f)),
                series(DailyMetric.READINESS, listOf(2f, 3f, 4f, 5f)),
            ),
            pairs = listOf(MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS)),
        )
        assertTrue("4 jours ne suffisent pas", insights.isEmpty())
    }

    @Test fun `perfect positive relation is detected`() {
        val sleep = (0 until 12).map { 5f + it * 0.25f }
        val readiness = (0 until 12).map { 1f + it * 0.3f }
        val insights = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.SLEEP_HOURS, sleep),
                series(DailyMetric.READINESS, readiness),
            ),
            pairs = listOf(MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS)),
        )
        val insight = insights.single()
        assertEquals(1f, insight.result.r, 0.001f)
        assertEquals(CorrelationStrength.STRONG, insight.result.strength)
        assertTrue(insight.narrative.contains("augmente aussi"))
    }

    @Test fun `inverse relation is described as a decrease`() {
        val hr = (0 until 12).map { 50f + it }
        val readiness = (0 until 12).map { 5f - it * 0.3f }
        val insight = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.RESTING_HR, hr),
                series(DailyMetric.READINESS, readiness),
            ),
            pairs = listOf(MetricPair(DailyMetric.RESTING_HR, DailyMetric.READINESS)),
        ).single()
        assertTrue("r doit être négatif", insight.result.r < -0.9f)
        assertTrue(insight.narrative.contains("baisse"))
    }

    @Test fun `only days present in both series are paired`() {
        // Le sommeil couvre 12 jours, la forme seulement les 6 premiers :
        // l'échantillon commun tombe sous le seuil, donc aucun insight.
        val insights = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.SLEEP_HOURS, List(12) { 7f + it * 0.1f }),
                series(DailyMetric.READINESS, List(6) { 3f + it * 0.2f }),
            ),
            pairs = listOf(MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS)),
        )
        assertTrue(insights.isEmpty())
    }

    @Test fun `flat series yields no link rather than a spurious one`() {
        val insight = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.STEPS, List(14) { 8000f }),
                series(DailyMetric.BODY_WEIGHT, List(14) { 82f + it * 0.05f }),
            ),
            pairs = listOf(MetricPair(DailyMetric.STEPS, DailyMetric.BODY_WEIGHT)),
        ).single()
        assertEquals(0f, insight.result.r, 0.001f)
        assertEquals(CorrelationStrength.NEGLIGIBLE, insight.result.strength)
        assertTrue(insight.narrative.contains("Aucun lien visible"))
    }

    @Test fun `insights are ordered by strength`() {
        val strong = List(14) { it.toFloat() }
        val noisy = listOf(3f, 1f, 4f, 1f, 5f, 9f, 2f, 6f, 5f, 3f, 5f, 8f, 9f, 7f)
        val insights = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.SLEEP_HOURS, strong),
                series(DailyMetric.READINESS, strong),
                series(DailyMetric.SESSION_VOLUME, noisy),
            ),
            pairs = listOf(
                MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.SESSION_VOLUME),
                MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS),
            ),
        )
        assertEquals(2, insights.size)
        assertTrue(
            "la corrélation la plus forte doit venir en premier",
            kotlin.math.abs(insights[0].result.r) >= kotlin.math.abs(insights[1].result.r),
        )
    }

    @Test fun `narrative never claims causation`() {
        val insight = CorrelationInsights.compute(
            mapOf(
                series(DailyMetric.SLEEP_HOURS, List(14) { 6f + it * 0.2f }),
                series(DailyMetric.READINESS, List(14) { 2f + it * 0.2f }),
            ),
            pairs = listOf(MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS)),
        ).single()
        assertTrue(
            "une corrélation n'est pas une cause, le texte doit le dire",
            insight.narrative.contains("pas forcément une cause"),
        )
    }

    @Test fun `metric keys are stable`() {
        // Ces cles sont persistees dans correlation_insight : les renommer
        // rendrait illisibles les correlations deja calculees.
        assertEquals("sleep_h", DailyMetric.SLEEP_HOURS.key)
        assertEquals("steps", DailyMetric.STEPS.key)
        assertEquals("readiness", DailyMetric.READINESS.key)
        assertEquals("resting_hr", DailyMetric.RESTING_HR.key)
        assertEquals("session_volume", DailyMetric.SESSION_VOLUME.key)
        assertEquals("body_weight", DailyMetric.BODY_WEIGHT.key)
    }
}
