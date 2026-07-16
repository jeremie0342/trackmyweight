package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyFatCalculatorTest {

    @Test fun `navy male typical values are in reasonable range`() {
        // Homme 180cm, cou 40cm, taille 84cm → ~15% body fat (référence)
        val pct = BodyFatCalculator.navyBodyFatPct(
            sex = Sex.MALE,
            heightCm = 180f,
            neckCm = 40f,
            waistCm = 84f,
            hipCm = null,
        )
        assertNotNull(pct)
        assertTrue("pct should be in typical range (10-20%): $pct", pct!! in 10f..20f)
    }

    @Test fun `navy female typical values`() {
        // Femme 165cm, cou 32cm, taille 70cm, hanches 95cm → ~24% (référence)
        val pct = BodyFatCalculator.navyBodyFatPct(
            sex = Sex.FEMALE,
            heightCm = 165f,
            neckCm = 32f,
            waistCm = 70f,
            hipCm = 95f,
        )
        assertNotNull(pct)
        assertTrue("pct should be in typical range (18-30%): $pct", pct!! in 18f..30f)
    }

    @Test fun `navy returns null for missing measurements`() {
        assertNull(BodyFatCalculator.navyBodyFatPct(Sex.MALE, 180f, null, 84f, null))
        assertNull(BodyFatCalculator.navyBodyFatPct(Sex.MALE, 180f, 40f, null, null))
        assertNull(BodyFatCalculator.navyBodyFatPct(Sex.FEMALE, 165f, 32f, 70f, null))
    }

    @Test fun `navy returns null for impossible values (waist smaller than neck for male)`() {
        assertNull(BodyFatCalculator.navyBodyFatPct(Sex.MALE, 180f, 45f, 40f, null))
    }

    @Test fun `compose returns fat and lean mass consistent with percentage`() {
        val comp = BodyFatCalculator.compose(
            sex = Sex.MALE, heightCm = 180f, weightKg = 84f,
            neckCm = 40f, waistCm = 84f, hipCm = null,
        )
        assertNotNull(comp)
        assertEquals(84f, comp!!.fatMassKg + comp.leanMassKg, 0.01f)
        assertTrue(comp.fatMassKg > 0f && comp.leanMassKg > 0f)
    }

    @Test fun `body fat decreases when waist shrinks (holding other constant)`() {
        val fatter = BodyFatCalculator.navyBodyFatPct(Sex.MALE, 180f, 40f, 95f, null)!!
        val leaner = BodyFatCalculator.navyBodyFatPct(Sex.MALE, 180f, 40f, 80f, null)!!
        assertTrue("smaller waist should give lower BF%: $leaner < $fatter", leaner < fatter)
    }
}
