package com.kps.trackmyweight.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.UnitSystem
import com.kps.trackmyweight.data.db.enums.WeightSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {

    private lateinit var db: TrackMyWeightDatabase

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun opens_and_persists_profile() = runTest {
        val now = Clock.System.now()
        val profile = UserProfileEntity(
            sex = Sex.MALE,
            birthDate = LocalDate(1995, 6, 15),
            heightCm = 180f,
            preferredUnit = UnitSystem.METRIC,
            currency = "XOF",
            locale = "fr",
            activityLevel = ActivityLevel.VERY_ACTIVE,
            coachModeEnabled = true,
            createdAt = now,
            updatedAt = now,
        )
        db.userDao().upsertProfile(profile)

        val loaded = db.userDao().observeProfile().first()
        assertNotNull(loaded)
        assertEquals(180f, loaded!!.heightCm)
        assertEquals(Sex.MALE, loaded.sex)
        assertEquals(true, loaded.coachModeEnabled)
    }

    @Test
    fun weight_upsert_replaces_same_date() = runTest {
        val now = Clock.System.now()
        val date = LocalDate(2026, 7, 15)
        db.bodyDao().upsertWeight(
            WeightEntryEntity(
                date = date, weightKg = 84f, source = WeightSource.MANUAL,
                recordedAt = now, createdAt = now,
            )
        )
        db.bodyDao().upsertWeight(
            WeightEntryEntity(
                date = date, weightKg = 83.6f, source = WeightSource.MANUAL,
                recordedAt = now, createdAt = now,
            )
        )
        val row = db.bodyDao().getWeightOnDate(date)
        assertNotNull(row)
        assertEquals(83.6f, row!!.weightKg)
    }

    @Test
    fun goal_active_flag_survives_write() = runTest {
        val now = Clock.System.now()
        db.userDao().insertGoal(
            GoalEntity(
                targetWeightKg = 75f,
                targetDate = LocalDate(2027, 1, 15),
                phase = GoalPhase.RECOMP,
                isActive = true,
                startedAt = LocalDate(2026, 7, 15),
                createdAt = now,
                updatedAt = now,
            )
        )
        val goal = db.userDao().observeActiveGoal().first()
        assertNotNull(goal)
        assertEquals(75f, goal!!.targetWeightKg)
        assertEquals(GoalPhase.RECOMP, goal.phase)
    }
}
