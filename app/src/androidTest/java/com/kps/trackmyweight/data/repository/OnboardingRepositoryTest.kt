package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.UnitSystem
import com.kps.trackmyweight.data.seed.EquipmentSeed
import com.kps.trackmyweight.domain.calc.NutritionTargets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var userRepo: UserProfileRepository
    private lateinit var goalRepo: GoalRepository
    private lateinit var gymRepo: GymRepository
    private lateinit var onboardingRepo: OnboardingRepository

    @Before
    fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepo = UserProfileRepository(db.userDao())
        goalRepo = GoalRepository(db.userDao(), db)
        gymRepo = GymRepository(db.userDao(), db)
        onboardingRepo = OnboardingRepository(db, userRepo, goalRepo, gymRepo, db.nutritionDao())
    }

    @After fun tearDown() = db.close()

    @Test
    fun isOnboarded_false_initially_true_after_save() = runTest {
        assertEquals(false, userRepo.isOnboarded())
        val now = Clock.System.now()
        userRepo.save(profile(now))
        assertEquals(true, userRepo.isOnboarded())
    }

    @Test
    fun equipment_seed_is_idempotent() = runTest {
        gymRepo.seedEquipmentIfEmpty()
        val first = gymRepo.observeAllEquipment().first().size
        gymRepo.seedEquipmentIfEmpty()
        val second = gymRepo.observeAllEquipment().first().size
        assertEquals(first, second)
        assertEquals(EquipmentSeed.items.size, first)
    }

    @Test
    fun switchActive_deactivates_previous_goal() = runTest {
        val now = Clock.System.now()
        goalRepo.switchActive(goal(now, targetKg = 80f))
        goalRepo.switchActive(goal(now, targetKg = 75f))
        val active = goalRepo.observeActive().first()
        assertNotNull(active)
        assertEquals(75f, active!!.targetWeightKg)
        assertTrue(active.isActive)
    }

    @Test
    fun completeOnboarding_persists_profile_goal_gym_and_diet_phase() = runTest {
        gymRepo.seedEquipmentIfEmpty()
        val allEquipment = gymRepo.observeAllEquipment().first()
        val someIds = allEquipment.take(5).map { it.id }.toSet()

        val now = Clock.System.now()
        val targets = NutritionTargets(
            bmr = 1830, tdee = 3157,
            targetKcal = 2700, targetProteinG = 176,
            targetCarbsG = 250, targetFatsG = 80,
            weeklyRateKg = -0.4f, recommendedPhase = GoalPhase.CUT,
        )

        onboardingRepo.completeOnboarding(
            profile = profile(now),
            goal = goal(now, targetKg = 75f),
            targets = targets,
            gymName = "TestGym",
            equipmentIds = someIds,
        )

        assertEquals(true, userRepo.isOnboarded())
        assertNotNull(goalRepo.observeActive().first())
        val defaultGym = gymRepo.getDefaultGym()
        assertNotNull(defaultGym)
        assertEquals("TestGym", defaultGym!!.name)
        val gymEquipment = gymRepo.observeEquipmentForGym(defaultGym.id).first()
        assertEquals(5, gymEquipment.size)
        val activePhase = db.nutritionDao().observeActivePhase().first()
        assertNotNull(activePhase)
        assertEquals(2700, activePhase!!.targetKcal)
        assertEquals(176, activePhase.targetProteinG)
    }

    @Test
    fun completeOnboarding_without_gym_leaves_no_default() = runTest {
        val now = Clock.System.now()
        val targets = NutritionTargets(
            bmr = 1500, tdee = 2000, targetKcal = 2000,
            targetProteinG = 120, targetCarbsG = 200, targetFatsG = 60,
            weeklyRateKg = 0f, recommendedPhase = GoalPhase.MAINTENANCE,
        )
        onboardingRepo.completeOnboarding(
            profile = profile(now),
            goal = goal(now, targetKg = 84f),
            targets = targets,
            gymName = null,
            equipmentIds = emptySet(),
        )
        assertNull(gymRepo.getDefaultGym())
    }

    private fun profile(now: kotlinx.datetime.Instant) = UserProfileEntity(
        sex = Sex.MALE,
        birthDate = LocalDate(1995, 3, 12),
        heightCm = 180f,
        preferredUnit = UnitSystem.METRIC,
        currency = "XOF",
        locale = "fr",
        activityLevel = ActivityLevel.VERY_ACTIVE,
        coachModeEnabled = false,
        createdAt = now, updatedAt = now,
    )

    private fun goal(now: kotlinx.datetime.Instant, targetKg: Float) = GoalEntity(
        targetWeightKg = targetKg,
        targetDate = LocalDate(2027, 1, 15),
        phase = GoalPhase.CUT,
        isActive = true,
        startedAt = LocalDate(2026, 7, 16),
        createdAt = now, updatedAt = now,
    )
}
