package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.FoodRegion
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.PortionMode
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortionResolverTest {
    @Test fun `precise grams returns quantity as-is`() {
        assertEquals(150f, PortionResolver.resolveGrams(PortionMode.PRECISE_G, 150f), 0f)
    }

    @Test fun `palm uses alias when given`() {
        assertEquals(120f, PortionResolver.resolveGrams(PortionMode.PALM, 1f, aliasGramsForMode = 120f), 0f)
    }

    @Test fun `palm falls back to category default for protein_animal`() {
        assertEquals(120f, PortionResolver.resolveGrams(PortionMode.PALM, 1f, category = FoodCategory.PROTEIN_ANIMAL), 0f)
    }

    @Test fun `fist for grain returns ~150g`() {
        assertEquals(150f, PortionResolver.resolveGrams(PortionMode.FIST, 1f, category = FoodCategory.GRAIN), 0f)
    }

    @Test fun `quantity multiplies portion`() {
        assertEquals(300f, PortionResolver.resolveGrams(PortionMode.FIST, 2f, category = FoodCategory.GRAIN), 0f)
    }

    @Test fun `unit falls back to defaultServingG`() {
        assertEquals(50f, PortionResolver.resolveGrams(PortionMode.UNIT, 1f, defaultServingG = 50f), 0f)
    }
}

class MacroCalculatorTest {
    private val food = FoodEntity(
        name = "Poulet", region = FoodRegion.BENIN, category = FoodCategory.PROTEIN_ANIMAL,
        kcalPer100g = 165f, proteinPer100g = 31f, carbsPer100g = 0f, fatsPer100g = 3.6f, fiberPer100g = 0f,
        createdAt = Instant.parse("2026-07-16T00:00:00Z"),
        updatedAt = Instant.parse("2026-07-16T00:00:00Z"),
    )

    @Test fun `snapshot scales by grams`() {
        val snap = MacroCalculator.snapshot(food, 120f)
        assertEquals(198f, snap.kcal, 0.5f)   // 165 * 1.2
        assertEquals(37.2f, snap.proteinG, 0.1f)
    }

    @Test fun `sum aggregates multiple entries`() {
        val a = MacroCalculator.snapshot(food, 100f)
        val b = MacroCalculator.snapshot(food, 50f)
        val total = MacroCalculator.sum(listOf(a, b))
        assertEquals(a.kcal + b.kcal, total.kcal, 0.01f)
        assertEquals(a.proteinG + b.proteinG, total.proteinG, 0.01f)
    }
}

class CostPerProteinTest {
    @Test fun `computes correctly`() {
        // 1000 FCFA for 200g of tuna (25g/100g protein) → 50g protein → 20 FCFA/g
        val cost = CostPerProtein.compute(
            pricePerPortion = 1000f,
            gramsPerPortion = 200f,
            proteinPer100g = 25f,
        )
        assertEquals(20f, cost!!, 0.01f)
    }

    @Test fun `returns null when data missing`() {
        assertNull(CostPerProtein.compute(null, 100f, 25f))
        assertNull(CostPerProtein.compute(500f, null, 25f))
        assertNull(CostPerProtein.compute(500f, 100f, 0f))
    }

    @Test fun `ranks foods ascending by cost`() {
        val items = listOf("cher" to 30f, "moyen" to 15f, "bon marche" to 5f, "inconnu" to null)
        val ranked = CostPerProtein.rankAscending(items) { it.second }
        assertEquals(listOf("bon marche", "moyen", "cher"), ranked.map { it.first })
    }
}

class ProteinDistributionTest {
    @Test fun `excellent when 3+ meals above threshold`() {
        val v = ProteinDistribution.analyze(
            meals = listOf(MealProtein("PD", 30f), MealProtein("D", 40f), MealProtein("Din", 35f)),
            dailyTargetG = 100,
        )
        assertEquals(DistributionQuality.EXCELLENT, v.quality)
    }

    @Test fun `insufficient when total below 75%% of target`() {
        val v = ProteinDistribution.analyze(
            meals = listOf(MealProtein("PD", 20f), MealProtein("D", 30f)),
            dailyTargetG = 200,
        )
        assertEquals(DistributionQuality.INSUFFICIENT, v.quality)
        assertTrue(v.advice.contains("Ajoute"))
    }

    @Test fun `unbalanced when only one meal has enough protein`() {
        val v = ProteinDistribution.analyze(
            meals = listOf(MealProtein("PD", 10f), MealProtein("D", 5f), MealProtein("Din", 120f)),
            dailyTargetG = 130,
        )
        assertEquals(DistributionQuality.UNBALANCED, v.quality)
    }
}

class CalorieAdapterTest {
    @Test fun `cut slow rate → decrease kcal or add steps`() {
        val advice = CalorieAdapter.advise(GoalPhase.CUT, 2500, -0.1f)
        assertNotNull(advice)
        assertEquals(-100, advice!!.deltaKcal)
        assertTrue(advice.stepsSuggestionDaily > 0)
    }

    @Test fun `cut fast rate → increase kcal to preserve muscle`() {
        val advice = CalorieAdapter.advise(GoalPhase.CUT, 2500, -1.0f)
        assertNotNull(advice)
        assertEquals(100, advice!!.deltaKcal)
    }

    @Test fun `cut in range → no advice`() {
        assertNull(CalorieAdapter.advise(GoalPhase.CUT, 2500, -0.4f))
    }

    @Test fun `bulk too slow → increase kcal`() {
        val advice = CalorieAdapter.advise(GoalPhase.BULK, 3000, 0.05f)
        assertNotNull(advice)
        assertEquals(100, advice!!.deltaKcal)
    }

    @Test fun `bulk too fast → decrease kcal`() {
        val advice = CalorieAdapter.advise(GoalPhase.BULK, 3000, 0.6f)
        assertNotNull(advice)
        assertEquals(-100, advice!!.deltaKcal)
    }

    @Test fun `weekly rate from linear fit is slope times 7`() {
        val start = LocalDate(2026, 7, 1)
        val points = (0..9).map { DatedValue(LocalDate.fromEpochDays(start.toEpochDays() + it), 84f - it * 0.1f) }
        val rate = CalorieAdapter.weeklyRateFrom(points)
        assertEquals(-0.7f, rate, 0.02f) // slope -0.1/day → -0.7/week
    }
}
