package com.kps.trackmyweight.data.backup

import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.FoodRegion
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.SleepSource
import com.kps.trackmyweight.data.db.enums.StepsSource
import com.kps.trackmyweight.data.db.enums.UnitSystem
import com.kps.trackmyweight.data.db.enums.WaterSource
import com.kps.trackmyweight.data.db.enums.WeightSource
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    @Test fun `round-trip preserves all fields`() {
        val root = sampleRoot()
        val encoded = json.encodeToString(BackupRoot.serializer(), root)
        val decoded = json.decodeFromString(BackupRoot.serializer(), encoded)
        assertEquals(root, decoded)
    }

    @Test fun `schema version is 2`() {
        assertEquals(2, BackupRoot.SCHEMA_VERSION)
        assertEquals(2, sampleRoot().schemaVersion)
    }

    @Test fun `decoded json is stable across encoders`() {
        val root = sampleRoot()
        val once = json.encodeToString(BackupRoot.serializer(), root)
        val twice = json.encodeToString(BackupRoot.serializer(), json.decodeFromString(BackupRoot.serializer(), once))
        assertEquals(once, twice)
    }

    @Test fun `empty root can be decoded`() {
        val empty = json.encodeToString(BackupRoot.serializer(), BackupRoot(
            exportedAt = "2026-07-16T12:00:00Z", profile = null, activeGoal = null, activePhase = null,
        ))
        val decoded = json.decodeFromString(BackupRoot.serializer(), empty)
        assertTrue(decoded.weights.isEmpty())
        assertTrue(decoded.workoutSessions.isEmpty())
    }

    @Test fun `decoding tolerates missing optional fields`() {
        // JSON minimum n'ayant que exportedAt
        val minimal = """{ "exportedAt": "2026-07-16T12:00:00Z" }"""
        val decoded = json.decodeFromString(BackupRoot.serializer(), minimal)
        assertEquals("2026-07-16T12:00:00Z", decoded.exportedAt)
    }

    private fun sampleRoot() = BackupRoot(
        exportedAt = "2026-07-16T12:00:00Z",
        profile = BProfile(
            sex = Sex.MALE, birthDate = "1995-03-12", heightCm = 180f,
            preferredUnit = UnitSystem.METRIC, currency = "XOF", locale = "fr",
            activityLevel = ActivityLevel.VERY_ACTIVE, coachModeEnabled = false,
        ),
        activeGoal = BGoal(
            targetWeightKg = 75f, targetDate = "2027-01-15", phase = GoalPhase.CUT,
            startedAt = "2026-07-16",
        ),
        weights = listOf(
            BWeight("2026-07-15", 84.3f, WeightSource.MANUAL, "2026-07-15T07:00:00Z", null),
            BWeight("2026-07-16", 84.0f, WeightSource.MANUAL, "2026-07-16T07:05:00Z", "note test"),
        ),
        measurements = listOf(
            BMeasurement(date = "2026-07-01", neckCm = 40f, waistCm = 84f),
        ),
        meals = listOf(
            BMeal(
                date = "2026-07-16", mealType = MealType.LUNCH,
                eatenAt = "2026-07-16T13:00:00Z",
                entries = listOf(
                    BMealEntry("Poulet bicyclette grillé", PortionMode.PALM, 1f, 120f, 200f, 33f, 0f, 7f, 0f),
                ),
            ),
        ),
        favorites = listOf(
            BFavoriteMeal(
                name = "Mon déj typique", mealTypeHint = MealType.LUNCH,
                entries = listOf(BFavoriteMealEntry("Riz blanc cuit", PortionMode.FIST, 1f)),
            ),
        ),
        customFoods = listOf(
            BFood(name = "Poulet bicyclette maison", region = FoodRegion.CUSTOM, category = FoodCategory.PROTEIN_ANIMAL,
                kcalPer100g = 200f, proteinPer100g = 28f, carbsPer100g = 0f, fatsPer100g = 9f,
                defaultServingG = 150f),
        ),
        workoutSessions = listOf(
            BWorkoutSession(
                date = "2026-07-15", startedAt = "2026-07-15T18:00:00Z", endedAt = "2026-07-15T19:30:00Z",
                sessionRpe = 8f, totalVolumeKg = 4500f,
                performedExercises = listOf(
                    BPerformedExercise("Développé couché", 0, null, listOf(
                        BPerformedSet(1, 80f, 8, 7f, SetType.WORKING, 180),
                        BPerformedSet(2, 82.5f, 8, 8f, SetType.WORKING, 180),
                    )),
                ),
            ),
        ),
        cardioSessions = listOf(
            BCardio("2026-07-15", "2026-07-15T07:00:00Z", "2026-07-15T07:30:00Z",
                CardioType.RUN, 1800, 4500f, 9f, 6f, 300f,
                com.kps.trackmyweight.data.db.enums.CardioSource.MANUAL, null),
        ),
        sleep = listOf(
            BSleep("2026-07-16", "2026-07-15T23:00:00Z", "2026-07-16T06:30:00Z", 450, 4, SleepSource.MANUAL),
        ),
        steps = listOf(
            BSteps("2026-07-15", 8500, StepsSource.HEALTH_CONNECT, 1f, 8500),
        ),
        water = listOf(
            BWater("2026-07-16", "2026-07-16T08:00:00Z", 500, WaterSource.MANUAL),
        ),
        dailyLogs = listOf(
            BDailyLog("2026-07-16", 4, 4, 5, 4, 4.25f, 58, com.kps.trackmyweight.data.db.enums.HeartRateSource.MANUAL, null),
        ),
        habitCompletions = listOf(
            BHabitCompletion("morning_weigh_in", "2026-07-16", true, null),
        ),
        activePhase = BDietPhase("2026-07-16", null, DietPhaseKind.CUT_MODERATE, 2500, 176, 250, 80, null),
    )
}
