package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmupCalculatorTest {
    @Test fun `compound warmup includes bar + progressive percentages`() {
        val sets = WarmupCalculator.generate(topSetKg = 100f, mechanics = ExerciseMechanics.COMPOUND)
        assertEquals(5, sets.size)
        assertEquals(20f, sets[0].weightKg, 0.01f) // bar
        assertTrue(sets[1].weightKg in 39f..41f)   // 40%
        assertTrue(sets[2].weightKg in 59f..61f)   // 60%
        assertTrue(sets[3].weightKg in 79f..81f)   // 80%
        assertTrue(sets[4].weightKg in 89f..91f)   // 90%
    }

    @Test fun `isolation warmup has 2 sets`() {
        val sets = WarmupCalculator.generate(topSetKg = 20f, mechanics = ExerciseMechanics.ISOLATION)
        assertEquals(2, sets.size)
    }

    @Test fun `no warmup when top set is at or below bar`() {
        val sets = WarmupCalculator.generate(topSetKg = 18f, mechanics = ExerciseMechanics.COMPOUND)
        assertTrue(sets.isEmpty())
    }

    @Test fun `rest time defaults are correct`() {
        assertEquals(180, RestTime.defaultSecFor(ExerciseMechanics.COMPOUND))
        assertEquals(90, RestTime.defaultSecFor(ExerciseMechanics.ISOLATION))
    }
}
