package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingLoadTest {

    @Test fun `table is empty without a reference max`() {
        assertTrue(WorkingLoad.table(0f).isEmpty())
        assertTrue(WorkingLoad.table(-10f).isEmpty())
    }

    @Test fun `table derives weights from the percentage of max`() {
        val rows = WorkingLoad.table(100f, percentages = listOf(90, 80, 70))
        assertEquals(listOf(90, 80, 70), rows.map { it.percentOfMax })
        assertEquals(90f, rows[0].weightKg, 0.01f)
        assertEquals(80f, rows[1].weightKg, 0.01f)
        assertEquals(70f, rows[2].weightKg, 0.01f)
    }

    @Test fun `weights are rounded to a loadable increment`() {
        // 120 × 85 % = 102 exactement ; 120 × 65 % = 78 ; on vérifie surtout
        // qu'aucune valeur ne sort avec des décimales non chargeables.
        WorkingLoad.table(123f).forEach { row ->
            val quarters = row.weightKg * 4f
            assertEquals(
                "poids non arrondi au 0,25 kg : ${row.weightKg}",
                quarters.toDouble(), Math.round(quarters).toDouble(), 0.001,
            )
        }
    }

    @Test fun `percentages are sorted descending and filtered`() {
        val rows = WorkingLoad.table(100f, percentages = listOf(70, 120, 90, 0, -5, 80))
        assertEquals(listOf(90, 80, 70), rows.map { it.percentOfMax })
    }

    @Test fun `expected reps decrease as intensity rises`() {
        val rows = WorkingLoad.table(100f)
        val reps = rows.map { it.estimatedReps }
        assertEquals(
            "les reps doivent croître quand le pourcentage baisse",
            reps.sorted(), reps,
        )
    }

    @Test fun `one hundred percent means a single rep`() {
        assertEquals(1, WorkingLoad.repsAtPercent(100))
        assertEquals(1, WorkingLoad.repsAtPercent(105))
    }

    @Test fun `reps at eighty percent land in the expected range`() {
        // Inversion d'Epley : 30 × (1/0,8 − 1) = 7,5 → 8
        assertEquals(8, WorkingLoad.repsAtPercent(80))
    }

    @Test fun `plates are computed only when the exercise is loaded on a bar`() {
        val onBar = WorkingLoad.table(100f, percentages = listOf(80), onBar = true)
        assertTrue("les disques doivent être calculés", onBar.single().plates != null)
        assertEquals(80f, onBar.single().plates!!.achievedKg, 0.01f)

        val offBar = WorkingLoad.table(100f, percentages = listOf(80), onBar = false)
        assertNull("pas de disques hors barre", offBar.single().plates)
    }

    @Test fun `no plates when the target is at or below the empty bar`() {
        val rows = WorkingLoad.table(20f, percentages = listOf(80), onBar = true, barKg = 20f)
        assertNull(rows.single().plates)
    }

    @Test fun `progression is expressed relative to the starting point`() {
        assertEquals(25f, WorkingLoad.progressionPercent(100f, 125f)!!, 0.01f)
        assertEquals(-10f, WorkingLoad.progressionPercent(100f, 90f)!!, 0.01f)
        assertEquals(0f, WorkingLoad.progressionPercent(100f, 100f)!!, 0.01f)
    }

    @Test fun `progression is undefined without a starting reference`() {
        assertNull(WorkingLoad.progressionPercent(null, 120f))
        assertNull(WorkingLoad.progressionPercent(120f, null))
        assertNull("division par zéro impossible", WorkingLoad.progressionPercent(0f, 120f))
    }
}
