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
import com.kps.trackmyweight.data.db.enums.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours

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
        val sessionId = repo.startSession(templateId = null, gymId = null, plan = emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, order = 0)
        repo.logSet(sessionId, exerciseId, pe.id, setNumber = 1, weightKg = 80f, reps = 5, rpe = 8f, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, setNumber = 2, weightKg = 82.5f, reps = 5, rpe = 8.5f, restBeforeSec = 180)
        val sets = repo.setsForPerformedExercise(pe.id)
        assertEquals(2, sets.size)
        assertEquals(82.5f, sets.last().weightKg)
    }

    @Test fun startSession_snapshots_the_plan() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(
            templateId = null,
            gymId = null,
            plan = listOf(
                PlannedExercise(
                    exerciseId = exerciseId, name = "Test Ex",
                    targetSets = 4, targetRepsMin = 8, targetRepsMax = 10,
                    targetWeightKg = 60f, restSecOverride = 150,
                ),
            ),
        )
        val performed = repo.performedExercisesForSession(sessionId).single()
        assertEquals(4, performed.targetSets)
        assertEquals(8, performed.targetRepsMin)
        assertEquals(10, performed.targetRepsMax)
        assertEquals(60f, performed.targetWeightKg)
        assertEquals(150, performed.restSecOverride)
    }

    @Test fun startSession_snapshots_superset_groups() = runTest {
        val a = seedExercise("Superset A1", "ss_a1")
        val b = seedExercise("Superset A2", "ss_a2")
        val isolated = seedExercise("Isolé", "ss_solo")

        val sessionId = repo.startSession(
            templateId = null, gymId = null,
            plan = listOf(
                PlannedExercise(exerciseId = a, name = "Superset A1", supersetGroup = 1),
                PlannedExercise(exerciseId = b, name = "Superset A2", supersetGroup = 1),
                PlannedExercise(exerciseId = isolated, name = "Isolé"),
            ),
        )

        val performed = repo.performedExercisesForSession(sessionId)
        assertEquals(listOf(1, 1, null), performed.map { it.supersetGroup })
        assertEquals(
            "l'ordre du plan est conservé",
            listOf(0, 1, 2), performed.map { it.orderIndex },
        )
    }

    @Test fun endSession_computes_total_volume() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, 2, 100f, 5, null, restBeforeSec = null)
        repo.endSession(sessionId, sessionRpe = 8f, notes = null)
        val session = repo.observeFinishedSessions(1).first().first()
        assertEquals(1000f, session.totalVolumeKg, 0.1f)
        assertNotNull(session.endedAt)
    }

    // ─────── Cycle de vie ───────

    @Test fun active_session_is_visible_until_finished() = runTest {
        val sessionId = repo.startSession(null, null, emptyList())
        assertNotNull("une séance ouverte doit être active", repo.observeActiveSession().first())

        repo.endSession(sessionId, null, null)
        assertNull("une séance terminée n'est plus active", repo.observeActiveSession().first())
    }

    @Test fun abandoned_session_leaves_active_and_history() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)

        repo.abandonSession(sessionId)

        assertNull("plus de séance active", repo.observeActiveSession().first())
        assertTrue("absente de l'historique", repo.observeFinishedSessions(10).first().isEmpty())
    }

    @Test fun unfinished_session_stays_out_of_history() = runTest {
        repo.startSession(null, null, emptyList())
        assertTrue(
            "une séance en cours ne doit pas polluer l'historique",
            repo.observeFinishedSessions(10).first().isEmpty(),
        )
    }

    @Test fun closeStaleSessions_discards_empty_and_closes_worked() = runTest {
        val exerciseId = seedExercise()

        // Ancienne séance vide → écartée.
        val empty = repo.startSession(null, null, emptyList())
        ageSession(empty, hours = 30)

        // Ancienne séance avec du travail → clôturée avec son volume.
        val worked = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(worked, exerciseId, 0)
        repo.logSet(worked, exerciseId, pe.id, 1, 50f, 10, null, restBeforeSec = null)
        ageSession(worked, hours = 30)

        // Séance récente → laissée telle quelle.
        val recent = repo.startSession(null, null, emptyList())

        val result = repo.closeStaleSessions(staleAfterHours = 12)

        assertEquals(1, result.discarded)
        assertEquals(1, result.closed)
        assertNull("la séance vide est écartée", repo.getSession(empty))
        assertNotNull("la séance travaillée est clôturée", repo.getSession(worked)?.endedAt)
        assertEquals(500f, repo.getSession(worked)!!.totalVolumeKg, 0.1f)
        assertEquals("la séance récente reste active", recent, repo.observeActiveSession().first()!!.id)
    }

    // ─────── Édition des séries ───────

    @Test fun updateSet_recomputes_volume() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        val set = repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)

        repo.updateSet(set.id, weightKg = 50f, reps = 5, rpe = null, type = SetType.WORKING)

        repo.endSession(sessionId, null, null)
        assertEquals(250f, repo.getSession(sessionId)!!.totalVolumeKg, 0.1f)
    }

    @Test fun deleteSet_renumbers_and_recomputes_volume() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        val first = repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, 2, 100f, 5, null, restBeforeSec = null)

        repo.deleteSet(first.id)

        val remaining = repo.setsForPerformedExercise(pe.id)
        assertEquals(1, remaining.size)
        assertEquals("les séries sont renumérotées", 1, remaining.single().setNumber)
        repo.endSession(sessionId, null, null)
        assertEquals(500f, repo.getSession(sessionId)!!.totalVolumeKg, 0.1f)
    }

    // ─────── Records ───────

    @Test fun logSet_detects_max_weight_PR_on_first_lift() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        val prs = db.workoutDao().observeRecentPrs(10).first()
        assertTrue("should have MAX_WEIGHT PR", prs.any { it.kind == PrKind.MAX_WEIGHT_ANY_REPS && it.value == 100f })
        assertTrue("should have 1RM PR", prs.any { it.kind == PrKind.ONE_RM_EST })
    }

    @Test fun warmup_sets_never_produce_a_PR() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        val set = repo.logSet(
            sessionId, exerciseId, pe.id, 1, 200f, 5, null,
            type = SetType.WARMUP, restBeforeSec = null,
        )
        assertTrue(db.workoutDao().observeRecentPrs(10).first().isEmpty())
        assertFalse("pas de badge PR sur un échauffement", set.isPrCandidate)
    }

    @Test fun logSet_flags_the_set_as_PR_candidate() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        val set = repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        assertTrue("isPrCandidate doit refléter la détection", set.isPrCandidate)
    }

    @Test fun more_reps_at_same_weight_is_a_PR() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, null, restBeforeSec = null)
        repo.logSet(sessionId, exerciseId, pe.id, 2, 100f, 8, null, restBeforeSec = null)

        val prs = db.workoutDao().observeRecentPrs(10).first()
        assertTrue(
            "le PR reps-à-charge-égale doit se déclencher",
            prs.any { it.kind == PrKind.MAX_REPS_AT_WEIGHT && it.value == 8f },
        )
    }

    // ─────── Divers ───────

    @Test fun lastSetForExercise_finds_across_sessions() = runTest {
        val exerciseId = seedExercise()
        val s1 = repo.startSession(null, null, emptyList())
        val pe1 = repo.getOrCreatePerformedExercise(s1, exerciseId, 0)
        repo.logSet(s1, exerciseId, pe1.id, 1, 80f, 5, null, restBeforeSec = null)
        repo.endSession(s1, null, null)

        val s2 = repo.startSession(null, null, emptyList())
        val pe2 = repo.getOrCreatePerformedExercise(s2, exerciseId, 0)
        repo.logSet(s2, exerciseId, pe2.id, 1, 85f, 5, null, restBeforeSec = null)

        val last = repo.lastSetForExercise(exerciseId)
        assertNotNull(last)
        assertEquals(85f, last!!.weightKg)
    }

    @Test fun formatSessionForCoach_produces_readable_text() = runTest {
        val exerciseId = seedExercise()
        val sessionId = repo.startSession(null, null, emptyList())
        val pe = repo.getOrCreatePerformedExercise(sessionId, exerciseId, 0)
        repo.logSet(sessionId, exerciseId, pe.id, 1, 100f, 5, 8f, restBeforeSec = null)
        repo.endSession(sessionId, sessionRpe = 8f, notes = "Bonne séance")
        val text = repo.formatSessionForCoach(sessionId)
        assertTrue("output contains exercise name", text.contains("Test Ex"))
        assertTrue("output contains set details", text.contains("100.0 kg"))
        assertTrue("output contains notes", text.contains("Bonne séance"))
    }

    /** Recule artificiellement le début d'une séance pour tester le ménage. */
    private suspend fun ageSession(sessionId: Long, hours: Int) {
        val session = db.workoutDao().getSession(sessionId)!!
        db.workoutDao().updateSession(session.copy(startedAt = Clock.System.now().minus(hours.hours)))
    }

    private suspend fun seedExercise(name: String = "Test Ex", slug: String = "test_ex"): Long {
        val now = Clock.System.now()
        val exercise = ExerciseEntity(
            name = name,
            slug = slug,
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
