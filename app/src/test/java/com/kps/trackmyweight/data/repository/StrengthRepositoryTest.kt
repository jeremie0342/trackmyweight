package com.kps.trackmyweight.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MaxLoadSource
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.domain.calc.IsoWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.days

/**
 * Le moteur du suivi de charge : rien ici n'était testé, alors que c'est ce qui
 * répond à « où j'en suis au fil du temps ».
 */
@RunWith(AndroidJUnit4::class)
class StrengthRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: StrengthRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = StrengthRepository(db.workoutDao(), db.exerciseDao())
    }

    @After fun tearDown() = db.close()

    // ─────── Charge maximale ───────

    @Test fun `un max teste est enregistre tel quel`() = runTest {
        val id = seedExercise("Squat")
        repo.recordTestedMax(id, 140f)
        val max = repo.getLatestMaxLoad(id)!!
        assertEquals(140f, max.oneRmKg, 0.01f)
        assertEquals(MaxLoadSource.TESTED, max.source)
        // Un test a 1 rep : la source est la charge elle-meme.
        assertEquals(140f, max.sourceWeightKg!!, 0.01f)
        assertEquals(1, max.sourceReps)
    }

    @Test fun `un max estime passe par la formule, pas par la charge brute`() = runTest {
        val id = seedExercise("Développé couché")
        repo.recordEstimatedMax(id, weightKg = 100f, reps = 5)
        val max = repo.getLatestMaxLoad(id)!!
        assertEquals(MaxLoadSource.ESTIMATED, max.source)
        // 5 reps a 100 kg valent nettement plus de 100 kg en 1RM.
        assertTrue("1RM estime = ${max.oneRmKg}", max.oneRmKg > 108f && max.oneRmKg < 120f)
        assertEquals(100f, max.sourceWeightKg!!, 0.01f)
        assertEquals(5, max.sourceReps)
    }

    @Test fun `une serie impossible n'enregistre rien`() = runTest {
        val id = seedExercise("Squat")
        // 0 rep n'est pas une performance : mieux vaut ne rien ecrire qu'un
        // 1RM nul qui deviendrait la reference courante.
        assertNull(repo.recordEstimatedMax(id, weightKg = 100f, reps = 0))
        assertNull(repo.getLatestMaxLoad(id))
    }

    @Test fun `un max declare est pris tel quel, sans source`() = runTest {
        val id = seedExercise("Squat")
        repo.recordDeclaredMax(id, 150f)
        val max = repo.getLatestMaxLoad(id)!!
        assertEquals(150f, max.oneRmKg, 0.01f)
        assertEquals(MaxLoadSource.DECLARED, max.source)
        // Valeur connue de l'exterieur : aucune serie ne la justifie.
        assertNull(max.sourceWeightKg)
        assertNull(max.sourceReps)
    }

    @Test fun `le max courant est le plus recent, pas le plus lourd`() = runTest {
        val id = seedExercise("Squat")
        val now = Clock.System.now()
        insertMaxAt(id, 150f, now - 30.days)
        insertMaxAt(id, 130f, now)
        // Une baisse assumee (reprise, blessure) reste la reference courante :
        // afficher 150 laisserait croire a une charge qu'on ne tient plus.
        assertEquals(130f, repo.getLatestMaxLoad(id)!!.oneRmKg, 0.01f)
    }

    @Test fun `la progression se mesure contre la valeur d'avant la fenetre`() = runTest {
        val id = seedExercise("Squat")
        val now = Clock.System.now()
        insertMaxAt(id, 100f, now - 200.days)   // hors fenetre : c'est la reference
        insertMaxAt(id, 120f, now - 10.days)    // dans la fenetre : c'est l'actuel

        val summary = repo.observeMaxLoadSummaries(windowDays = 90).first().single()
        assertEquals("Squat", summary.exerciseName)
        assertEquals(120f, summary.current.oneRmKg, 0.01f)
        assertEquals(100f, summary.referenceKg!!, 0.01f)
        assertEquals(20f, summary.deltaKg!!, 0.01f)
        assertEquals(20f, summary.progressionPercent!!, 0.1f)
    }

    @Test fun `sans historique anterieur, pas de progression inventee`() = runTest {
        val id = seedExercise("Squat")
        insertMaxAt(id, 120f, Clock.System.now() - 10.days)
        val summary = repo.observeMaxLoadSummaries(windowDays = 90).first().single()
        // Une premiere mesure n'est une progression de rien du tout.
        assertNull(summary.referenceKg)
        assertNull(summary.deltaKg)
        assertNull(summary.progressionPercent)
    }

    @Test fun `un exercice supprime ne fait pas planter le resume`() = runTest {
        val id = seedExercise("Squat")
        insertMaxAt(id, 120f, Clock.System.now())
        // Pas de setter dedie : la suppression douce se fait par update.
        val supprime = db.exerciseDao().getById(id)!!.copy(isDeleted = true)
        db.exerciseDao().updateExercise(supprime)
        // observeAll ne renvoie plus l'exercice : la ligne est ignoree, pas une exception.
        assertTrue(repo.observeMaxLoadSummaries().first().isEmpty())
    }

    // ─────── Volume hebdomadaire ───────

    @Test fun `le volume hebdo agrege la semaine et la persiste`() = runTest {
        // Jeudi 13 aout 2026, semaine du lundi 10 au dimanche 16.
        val jeudi = LocalDate(2026, 8, 13)
        val squat = seedExercise("Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES))
        seedSession(jeudi, squat, sets = 4, weightKg = 100f, reps = 5)

        val verdicts = repo.refreshWeeklyVolume(jeudi)
        assertTrue(verdicts.isNotEmpty())

        val stored = repo.weeklyVolumeFor(jeudi)
        val quads = stored.single { it.muscleGroup == MuscleGroup.QUADS }
        assertEquals(IsoWeek.of(jeudi), quads.isoWeek)
        assertEquals(4, quads.totalSets)
        assertEquals(20, quads.totalReps)
        assertEquals(2000f, quads.totalVolumeKg, 0.01f)
        // Les reperes sont figes au moment du calcul.
        assertTrue(quads.mev > 0 && quads.mev <= quads.mav && quads.mav <= quads.mrv)

        // Muscle secondaire : compte pour moitie, donc 2 series sur 4.
        val glutes = stored.single { it.muscleGroup == MuscleGroup.GLUTES }
        assertEquals(2, glutes.totalSets)
    }

    @Test fun `l'echauffement ne compte pas dans le volume`() = runTest {
        val jeudi = LocalDate(2026, 8, 13)
        val squat = seedExercise("Squat", MuscleGroup.QUADS)
        val pe = seedSession(jeudi, squat, sets = 3, weightKg = 100f, reps = 5)
        db.workoutDao().insertPerformedSet(
            PerformedSetEntity(
                performedExerciseId = pe, setNumber = 4, weightKg = 40f, reps = 15,
                type = SetType.WARMUP, createdAt = Clock.System.now(),
            ),
        )
        repo.refreshWeeklyVolume(jeudi)
        val quads = repo.weeklyVolumeFor(jeudi).single { it.muscleGroup == MuscleGroup.QUADS }
        assertEquals(3, quads.totalSets)
        assertEquals(1500f, quads.totalVolumeKg, 0.01f)
    }

    @Test fun `une seance hors semaine n'est pas comptee`() = runTest {
        val jeudi = LocalDate(2026, 8, 13)
        val squat = seedExercise("Squat", MuscleGroup.QUADS)
        // Dimanche 9 : la veille du lundi qui ouvre la semaine.
        seedSession(LocalDate(2026, 8, 9), squat, sets = 5, weightKg = 100f, reps = 5)
        assertTrue(repo.refreshWeeklyVolume(jeudi).isEmpty())
        assertTrue(repo.weeklyVolumeFor(jeudi).isEmpty())
    }

    @Test fun `toute la semaine compte, du lundi au dimanche`() = runTest {
        val squat = seedExercise("Squat", MuscleGroup.QUADS)
        seedSession(LocalDate(2026, 8, 10), squat, sets = 2, weightKg = 100f, reps = 5) // lundi
        seedSession(LocalDate(2026, 8, 16), squat, sets = 3, weightKg = 100f, reps = 5) // dimanche

        // Recalcule depuis le mercredi : les bornes viennent de la semaine, pas du jour.
        repo.refreshWeeklyVolume(LocalDate(2026, 8, 12))
        val quads = repo.weeklyVolumeFor(LocalDate(2026, 8, 12)).single { it.muscleGroup == MuscleGroup.QUADS }
        assertEquals(5, quads.totalSets)
    }

    @Test fun `recalculer la meme semaine remplace au lieu d'empiler`() = runTest {
        val jeudi = LocalDate(2026, 8, 13)
        val squat = seedExercise("Squat", MuscleGroup.QUADS)
        seedSession(jeudi, squat, sets = 3, weightKg = 100f, reps = 5)
        repo.refreshWeeklyVolume(jeudi)
        seedSession(jeudi, squat, sets = 2, weightKg = 100f, reps = 5)
        repo.refreshWeeklyVolume(jeudi)

        // Une seule ligne par (semaine, muscle) — l'index unique le garantit,
        // encore faut-il que l'upsert s'appuie dessus plutot que d'inserer.
        val quads = repo.weeklyVolumeFor(jeudi).filter { it.muscleGroup == MuscleGroup.QUADS }
        assertEquals(1, quads.size)
        assertEquals(5, quads.single().totalSets)
    }

    @Test fun `une semaine sans seance ne rend aucun verdict`() = runTest {
        assertTrue(repo.refreshWeeklyVolume(LocalDate(2026, 8, 13)).isEmpty())
    }

    // ─────── Tonnage ───────

    @Test fun `le tonnage mensuel est rendu du plus ancien au plus recent`() = runTest {
        val squat = seedExercise("Squat", MuscleGroup.QUADS)
        seedSession(LocalDate(2026, 6, 10), squat, sets = 2, weightKg = 100f, reps = 5)
        seedSession(LocalDate(2026, 8, 10), squat, sets = 3, weightKg = 100f, reps = 5)

        val tonnage = repo.observeMonthlyTonnage(months = 12).first()
        assertEquals(2, tonnage.size)
        // Un graphique se lit de gauche a droite : la requete trie DESC, le
        // repository doit remettre dans l'ordre chronologique.
        assertEquals("2026-06", tonnage.first().month)
        assertEquals("2026-08", tonnage.last().month)
        assertEquals(1000f, tonnage.first().volumeKg, 0.01f)
        assertEquals(1500f, tonnage.last().volumeKg, 0.01f)
    }

    // ─────── Fixtures ───────

    private suspend fun seedExercise(
        name: String,
        primary: MuscleGroup = MuscleGroup.CHEST,
        secondary: List<MuscleGroup> = emptyList(),
    ): Long {
        val now = Clock.System.now()
        return db.exerciseDao().upsertExercise(
            ExerciseEntity(
                name = name,
                slug = name.lowercase().replace(' ', '-'),
                primaryMuscle = primary,
                secondaryMuscles = secondary,
                mechanics = ExerciseMechanics.COMPOUND,
                force = ExerciseForce.PUSH,
                defaultRestSec = 120,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun insertMaxAt(exerciseId: Long, oneRmKg: Float, at: Instant) {
        repo.recordEstimatedMax(exerciseId, weightKg = oneRmKg, reps = 1, at = at)
    }

    /** Une séance d'un exercice, [sets] séries identiques. Renvoie l'id du performed_exercise. */
    private suspend fun seedSession(
        date: LocalDate,
        exerciseId: Long,
        sets: Int,
        weightKg: Float,
        reps: Int,
    ): Long {
        val now = Clock.System.now()
        val sessionId = db.workoutDao().insertSession(
            WorkoutSessionEntity(date = date, startedAt = now, endedAt = now),
        )
        val peId = db.workoutDao().insertPerformedExercise(
            PerformedExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseNameSnapshot = "Exercice",
                orderIndex = 0,
            ),
        )
        repeat(sets) { i ->
            db.workoutDao().insertPerformedSet(
                PerformedSetEntity(
                    performedExerciseId = peId,
                    setNumber = i + 1,
                    weightKg = weightKg,
                    reps = reps,
                    type = SetType.WORKING,
                    createdAt = now,
                ),
            )
        }
        assertNotNull(db.workoutDao().getSession(sessionId))
        return peId
    }
}
