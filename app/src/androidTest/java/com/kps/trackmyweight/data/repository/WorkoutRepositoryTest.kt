package com.kps.trackmyweight.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PrKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: WorkoutRepository

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WorkoutRepository(db, db.workoutDao(), db.exerciseDao())
    }

    @After fun tearDown() = db.close()

    @Test fun startSession_and_logSet_persist() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(templateId = null, gymId = null)
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, order = 0)
        repo.logSet(sessionId, exerciseId, pe.id, setNumber = 1, weightKg = 80f, reps = 5, rpe = 8f, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, setNumber = 2, weightKg = 82.5f, reps = 5, rpe = 8.5f, restBeforeSec = 180)
        val sets = repo.setsForPerformedExercise(pe.id)
        assertEquals(2, sets.size)
        assertEquals(82.5f, sets.last().weightKg)
    }

    @Test fun endSession_computes_total_volume() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null)
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, 2, 100f, 5, null, restBeforeSec = null)
        repo.endSession(sessionId, sessionRpe = 8f, notes = null)
        val session = repo.observeRecentSessions(1).first().first()
        assertEquals(1000f, session.totalVolumeKg, 0.1f)
        assertNotNull(session.endedAt)
    }

    @Test fun logSet_detects_max_weight_PR_on_first_lift() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null)
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        val prs = db.workoutDao().observeRecentPrs(10).first()
        assertTrue("should have MAX_WEIGHT PR", prs.any { it.kind == PrKind.MAX_WEIGHT_ANY_REPS && it.value == 100f })
        assertTrue("should have 1RM PR", prs.any { it.kind == PrKind.ONE_RM_EST })
    }

    @Test fun lastSetForExercise_finds_across_sessions() = runTest {
        val exerciseId = seedExercise()
        // Session 1
        val s1 = repo.startSession(null, null)
        val pe1 = repo.getOrCreatePerformedExercise(s1, exerciseId, 0)
        repo.logSet(s1, exerciseId, pe1.id, 1, 80f, 5, null, restBeforeSec = null)
        repo.endSession(s1, null, null)
        // Session 2 : dernière série s'ajoute
        val s2 = repo.startSession(null, null)
        val pe2 = repo.getOrCreatePerformedExercise(s2, exerciseId, 0)
        repo.logSet(s2, exerciseId, pe2.id, 1, 85f, 5, null, restBeforeSec = null)
        val last = repo.lastSetForExercise(exerciseId)
        assertNotNull(last)
        assertEquals(85f, last!!.weightKg)
    }

    @Test fun formatSessionForCoach_produces_readable_text() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null)
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, 8f, restBeforeSec = null)
        repo.endSession(sessionId, sessionRpe = 8f, notes = "Bonne séance")
        val text = repo.formatSessionForCoach(sessionId)
        assertTrue("output contains exercise name", text.contains("Test Ex"))
        assertTrue("output contains set details", text.contains("100.0 kg"))
        assertTrue("output contains notes", text.contains("Bonne séance"))
    }

    private suspend fun seedExercise(): Long {
        val now = Clock.System.now()
        val exercise = ExerciseEntity(
            name = "Test Ex",
            slug = "test_ex",
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = emptyList(),
            mechanics = ExerciseMechanics.COMPOUND,
            force = ExerciseForce.PUSH,
            defaultRestSec = 180,
            createdAt = now,
            updatedAt = now,
        )
        return db.exerciseDao().upsertExercise(exercise)
    }
}
