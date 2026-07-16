package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test fun `bmr Mifflin-St Jeor male reference case`() {
        // Homme 30 ans, 84 kg, 180 cm → 10*84 + 6.25*180 - 5*30 + 5 = 1820
        val bmr = NutritionCalculator.bmr(Sex.MALE, 84f, 180f, 30)
        assertEquals(1820, bmr)
    }

    @Test fun `bmr Mifflin-St Jeor female reference case`() {
        // Femme 30 ans, 60 kg, 165 cm → BMR théorique ~ 1320
        val bmr = NutritionCalculator.bmr(Sex.FEMALE, 60f, 165f, 30)
        assertEquals(1320, bmr)
    }

    @Test fun `tdee scales with activity`() {
        val bmr = 1820
        val sedentary = NutritionCalculator.tdee(bmr, ActivityLevel.SEDENTARY)
        val veryActive = NutritionCalculator.tdee(bmr, ActivityLevel.VERY_ACTIVE)
        assertEquals(2184, sedentary)   // 1820 * 1.2
        assertEquals(3140, veryActive)  // 1820 * 1.725 = 3139.5 → 3140
        assertTrue("veryActive should be > sedentary", veryActive > sedentary)
    }

    @Test fun `protein target adapts to phase`() {
        val w = 84f
        assertEquals(176, NutritionCalculator.proteinTargetG(w, GoalPhase.CUT))
        assertEquals(160, NutritionCalculator.proteinTargetG(w, GoalPhase.RECOMP))
        assertEquals(143, NutritionCalculator.proteinTargetG(w, GoalPhase.BULK))
        assertEquals(134, NutritionCalculator.proteinTargetG(w, GoalPhase.MAINTENANCE))
    }

    @Test fun `weekly rate is capped for cut safety`() {
        // Vise 60kg depuis 84kg en 4 semaines → 6kg par semaine "brut", doit être plafonné.
        val rate = NutritionCalculator.computeWeeklyRateKg(
            currentWeightKg = 84f,
            targetWeightKg = 60f,
            today = LocalDate(2026, 7, 15),
            targetDate = LocalDate(2026, 8, 12),
        )
        // Cap : max 1% du poids ou 0.8kg, donc -0.8kg/semaine.
        assertEquals(-0.8f, rate, 0.01f)
    }

    @Test fun `weekly rate handles reasonable cut correctly`() {
        // Vise 75kg depuis 84kg d'ici janvier 2027 (~26 semaines) → -0.35kg/sem
        val rate = NutritionCalculator.computeWeeklyRateKg(
            currentWeightKg = 84f,
            targetWeightKg = 75f,
            today = LocalDate(2026, 7, 15),
            targetDate = LocalDate(2027, 1, 15),
        )
        assertTrue("rate should be a moderate cut", rate < 0f && rate > -0.5f)
    }

    @Test fun `weekly rate is capped for bulk safety`() {
        // Bulk agressif : vise 100kg depuis 60kg en 4 semaines
        val rate = NutritionCalculator.computeWeeklyRateKg(
            currentWeightKg = 60f,
            targetWeightKg = 100f,
            today = LocalDate(2026, 7, 15),
            targetDate = LocalDate(2026, 8, 12),
        )
        // Cap : 0.5% du poids ou 0.4kg → 0.3kg/sem pour 60kg
        assertEquals(0.3f, rate, 0.01f)
    }

    @Test fun `suggestPhase returns CUT when target is significantly lower`() {
        assertEquals(GoalPhase.CUT, NutritionCalculator.suggestPhase(84f, 75f))
    }

    @Test fun `suggestPhase returns BULK when target is significantly higher`() {
        assertEquals(GoalPhase.BULK, NutritionCalculator.suggestPhase(60f, 70f))
    }

    @Test fun `suggestPhase returns MAINTENANCE when target equals current`() {
        assertEquals(GoalPhase.MAINTENANCE, NutritionCalculator.suggestPhase(80f, 80f))
    }

    @Test fun `suggestPhase returns RECOMP for small delta`() {
        // 84 → 82 = -2.4% (dans la fenêtre recomp -3% à +3%)
        assertEquals(GoalPhase.RECOMP, NutritionCalculator.suggestPhase(84f, 82f))
    }

    @Test fun `compute end-to-end for cut scenario matches manual calc`() {
        val targets = NutritionCalculator.compute(
            sex = Sex.MALE,
            weightKg = 84f,
            heightCm = 180f,
            ageYears = 30,
            activityLevel = ActivityLevel.VERY_ACTIVE,
            targetWeightKg = 75f,
            today = LocalDate(2026, 7, 15),
            targetDate = LocalDate(2027, 1, 15),
        )
        assertEquals(1820, targets.bmr)
        assertEquals(3140, targets.tdee)
        assertEquals(GoalPhase.CUT, targets.recommendedPhase)
        assertTrue("kcal target should be in cut range", targets.targetKcal in 2500..3100)
        assertTrue("deficit should be positive", targets.deficitKcal > 0)
        assertTrue("proteins should be substantial in cut", targets.targetProteinG >= 170)
        assertTrue("carbs should be positive", targets.targetCarbsG > 0)
    }

    @Test fun `compute never returns negative macros`() {
        // Cas extrême : très petite personne avec cible très basse
        val t = NutritionCalculator.compute(
            sex = Sex.FEMALE,
            weightKg = 45f,
            heightCm = 150f,
            ageYears = 60,
            activityLevel = ActivityLevel.SEDENTARY,
            targetWeightKg = 40f,
            today = LocalDate(2026, 7, 15),
            targetDate = LocalDate(2026, 10, 15),
        )
        assertTrue(t.targetKcal > 0)
        assertTrue(t.targetProteinG > 0)
        assertTrue(t.targetCarbsG >= 0)
        assertTrue(t.targetFatsG > 0)
    }
}
