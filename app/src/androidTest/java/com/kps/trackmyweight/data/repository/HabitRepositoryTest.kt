package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.seed.HabitSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: HabitRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = HabitRepository(db.habitDao(), db.nutritionDao())
    }

    @After fun tearDown() = db.close()

    @Test fun seedIfEmpty_populates_habits() = runTest {
        repo.seedIfEmpty()
        val habits = repo.observeHabits().first()
        assertEquals(HabitSeed.items.size, habits.size)
    }

    @Test fun toggle_completion_persists() = runTest {
        repo.seedIfEmpty()
        val habit = repo.observeHabits().first().first()
        val date = LocalDate(2026, 7, 16)
        repo.toggleCompletion(habit.id, date, done = true)
        val done = repo.observeCompletionsForDate(date).first()
        assertTrue(done.any { it.habitId == habit.id && it.isDone })
    }

    @Test fun streak_days_computed_recursively() = runTest {
        repo.seedIfEmpty()
        val habit = repo.observeHabits().first().first()
        val today = LocalDate(2026, 7, 16)
        // Log 5 jours consécutifs
        (0..4).forEach { offset ->
            val d = LocalDate.fromEpochDays(today.toEpochDays() - offset)
            repo.toggleCompletion(habit.id, d, done = true)
        }
        val streak = repo.streakDays(habit.id, today)
        assertEquals(5, streak)
    }

    @Test fun readiness_score_is_computed_and_stored() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.saveReadiness(date, sleepQuality = 4, energy = 4, soreness = 5, mood = 4)
        val log = repo.observeDailyLog(date).first()
        assertNotNull(log)
        assertEquals(4.25f, log!!.readinessScore!!, 0.01f)
    }

    @Test fun sleep_upsert_by_date() = runTest {
        val date = LocalDate(2026, 7, 16)
        val bed = Instant.parse("2026-07-15T23:00:00Z")
        val wake1 = Instant.parse("2026-07-16T06:00:00Z")
        val wake2 = Instant.parse("2026-07-16T07:30:00Z")
        repo.logSleep(date, bed, wake1, qualityRating = 3)
        repo.logSleep(date, bed, wake2, qualityRating = 4)
        val entry = repo.observeSleepForDate(date).first()
        assertNotNull(entry)
        assertEquals(510, entry!!.durationMin)   // 23h → 7h30 = 8h30 = 510
        assertEquals(4, entry.qualityRating)
    }

    @Test fun water_logs_accumulate() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.logWater(date, 500)
        repo.logWater(date, 750)
        val total = repo.observeWaterMlForDate(date).first()
        assertEquals(1250, total)
    }

    @Test fun morning_pulse_saved_in_log_and_hr_samples() = runTest {
        val date = LocalDate(2026, 7, 16)
        repo.saveMorningPulse(date, bpm = 58)
        val log = repo.observeDailyLog(date).first()
        assertEquals(58, log!!.restingHrBpm)
    }
}
