package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.PrKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrDetectorTest {
    @Test fun `first ever set flags all PRs`() {
        val prs = PrDetector.detect(80f, 5, currentMaxWeight = null, currentOneRm = null, currentMaxRepsAtWeight = null)
        assertEquals(2, prs.size)  // max weight + 1RM (max reps at weight impossible without baseline)
        assertTrue(prs.any { it.kind == PrKind.MAX_WEIGHT_ANY_REPS })
        assertTrue(prs.any { it.kind == PrKind.ONE_RM_EST })
    }

    @Test fun `no PR when set is weaker across the board`() {
        val prs = PrDetector.detect(
            newWeightKg = 70f, newReps = 5,
            currentMaxWeight = 100f, currentOneRm = 120f, currentMaxRepsAtWeight = 10,
        )
        assertTrue(prs.isEmpty())
    }

    @Test fun `heavier weight flags MAX_WEIGHT and possibly 1RM`() {
        val prs = PrDetector.detect(
            newWeightKg = 110f, newReps = 3,
            currentMaxWeight = 100f, currentOneRm = 115f, currentMaxRepsAtWeight = null,
        )
        assertTrue(prs.any { it.kind == PrKind.MAX_WEIGHT_ANY_REPS })
    }

    @Test fun `more reps at same weight flags MAX_REPS_AT_WEIGHT`() {
        val prs = PrDetector.detect(
            newWeightKg = 80f, newReps = 12,
            currentMaxWeight = 100f, currentOneRm = 130f, currentMaxRepsAtWeight = 10,
        )
        assertTrue(prs.any { it.kind == PrKind.MAX_REPS_AT_WEIGHT && it.value == 12f && it.referenceValue == 80f })
    }

    @Test fun `invalid inputs return empty list`() {
        assertTrue(PrDetector.detect(0f, 5, null, null, null).isEmpty())
        assertTrue(PrDetector.detect(80f, 0, null, null, null).isEmpty())
    }
}
