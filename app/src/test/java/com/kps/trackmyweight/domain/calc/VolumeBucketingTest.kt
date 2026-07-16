package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeBucketingTest {
    @Test fun `under mev is flagged`() {
        val v = VolumeBucketing.verdictFor(MuscleGroup.CHEST, currentSets = 4)
        assertEquals(VolumeStatus.UNDER_MEV, v.status)
        assertTrue("suggested delta should propose adding sets", v.suggestedSetsDelta > 0)
    }

    @Test fun `within range keeps modest growth suggestion`() {
        val v = VolumeBucketing.verdictFor(MuscleGroup.CHEST, currentSets = 12)
        assertEquals(VolumeStatus.WITHIN_RANGE, v.status)
        assertEquals(2, v.suggestedSetsDelta)
    }

    @Test fun `over mrv suggests reducing`() {
        val v = VolumeBucketing.verdictFor(MuscleGroup.CHEST, currentSets = 24)
        assertEquals(VolumeStatus.OVER_MRV, v.status)
        assertTrue(v.suggestedSetsDelta < 0)
    }

    @Test fun `aggregate distributes sets to primary and secondary muscles`() {
        val summaries = listOf(
            ExerciseSetsSummary(1L, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_FRONT), totalSets = 12),
            ExerciseSetsSummary(2L, MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST), totalSets = 9),
        )
        val agg = VolumeBucketing.aggregate(summaries, secondaryWeight = 0.5f)
        assertEquals(16, agg[MuscleGroup.CHEST])          // 12 primary + 9*0.5 secondary
        assertEquals(15, agg[MuscleGroup.TRICEPS])         // 9 primary + 12*0.5 secondary
        assertEquals(6, agg[MuscleGroup.SHOULDERS_FRONT])  // 12*0.5 secondary
    }
}
