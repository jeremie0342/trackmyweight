package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: NutritionRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NutritionRepository(db, db.nutritionDao())
    }

    @After fun tearDown() = db.close()

    @Test fun seedIfEmpty_populates_foods_and_is_idempotent() = runTest {
        repo.seedIfEmpty()
        val n1 = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().size
        repo.seedIfEmpty()
        val n2 = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().size
        assertEquals(n1, n2)
        assertTrue("should have seeded foods", n1 > 0)
    }

    @Test fun addEntry_creates_meal_and_snapshots_macros() = runTest {
        repo.seedIfEmpty()
        val date = LocalDate(2026, 7, 16)
        val food = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().first { it.name.contains("Poulet") }
        repo.addEntry(date, MealType.LUNCH, food.id, PortionMode.PRECISE_G, 200f)
        val meals = repo.getMealsWithEntries(date)
        assertEquals(1, meals.size)
        val entry = meals.first().entries.first()
        // 200g × (kcalPer100g / 100)
        assertEquals(food.kcalPer100g * 2f, entry.entry.snapKcal, 0.5f)
        assertEquals(food.proteinPer100g * 2f, entry.entry.snapProteinG, 0.1f)
    }

    @Test fun addEntry_appends_to_existing_meal_of_same_type() = runTest {
        repo.seedIfEmpty()
        val date = LocalDate(2026, 7, 16)
        val food = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().first()
        repo.addEntry(date, MealType.DINNER, food.id, PortionMode.PRECISE_G, 100f)
        repo.addEntry(date, MealType.DINNER, food.id, PortionMode.PRECISE_G, 150f)
        val meals = repo.getMealsWithEntries(date)
        assertEquals("one meal", 1, meals.size)
        assertEquals("two entries in meal", 2, meals.first().entries.size)
    }

    @Test fun createFavoriteFromMeal_and_apply_reproduces_entries() = runTest {
        repo.seedIfEmpty()
        val date = LocalDate(2026, 7, 16)
        val food = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().first()
        repo.addEntry(date, MealType.LUNCH, food.id, PortionMode.PRECISE_G, 100f)
        repo.addEntry(date, MealType.LUNCH, food.id, PortionMode.PRECISE_G, 50f)
        val meal = repo.getMealsWithEntries(date).first()
        val favId = repo.createFavoriteFromMeal("Mon déj typique", MealType.LUNCH, meal)

        val date2 = LocalDate(2026, 7, 17)
        repo.applyFavorite(date2, MealType.LUNCH, favId)
        val meals2 = repo.getMealsWithEntries(date2)
        assertEquals(1, meals2.size)
        assertEquals(2, meals2.first().entries.size)
    }

    @Test fun observeDailyMacros_returns_totals() = runTest {
        repo.seedIfEmpty()
        val date = LocalDate(2026, 7, 16)
        val food = db.nutritionDao().observeFoodsByRegion(com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, 500).first().first()
        repo.addEntry(date, MealType.LUNCH, food.id, PortionMode.PRECISE_G, 200f)
        val macros = repo.observeDailyMacros(date).first()
        assertEquals(food.kcalPer100g * 2f, macros.kcal, 0.5f)
    }
}
