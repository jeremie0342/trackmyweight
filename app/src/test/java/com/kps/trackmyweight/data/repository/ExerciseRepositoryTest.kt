package com.kps.trackmyweight.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.seed.ExerciseSeed
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

/**
 * `syncCatalog` tourne à chaque lancement et réécrit 200 exercices.
 *
 * C'est le code le plus proche du piège documenté sur `upsertExercise` :
 * `@Insert(REPLACE)` ferait un DELETE avant l'INSERT, ce qui emporterait les
 * records en cascade et buterait sur le RESTRICT de `performed_exercise`.
 * D'où le `@Update` dédié — et d'où ces tests, qui vérifient que la
 * synchronisation met bien à jour sans jamais recréer une ligne.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest {

    private lateinit var db: TrackMyWeightDatabase
    private lateinit var repo: ExerciseRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ExerciseRepository(db.exerciseDao(), db.userDao())
    }

    @After fun tearDown() = db.close()

    private val seedSize get() = ExerciseSeed.items(Clock.System.now()).size

    @Test fun `la premiere synchro insere tout le catalogue`() = runTest {
        repo.syncCatalog()
        assertEquals(seedSize, repo.observeAll().first().size)
        assertTrue("le catalogue devrait etre consequent", seedSize >= 200)
    }

    @Test fun `synchroniser deux fois ne duplique rien`() = runTest {
        repo.syncCatalog()
        repo.syncCatalog()
        val all = repo.observeAll().first()
        assertEquals(seedSize, all.size)
        // L'index unique sur slug le garantirait en levant, mais une insertion
        // en IGNORE echoue en silence : mieux vaut le constater.
        assertEquals(all.size, all.map { it.slug }.distinct().size)
    }

    @Test fun `les identifiants survivent a une resynchro`() = runTest {
        repo.syncCatalog()
        val avant = repo.observeAll().first().associate { it.slug to it.id }
        repo.syncCatalog()
        val apres = repo.observeAll().first().associate { it.slug to it.id }
        // Si un seul id bougeait, tout l'historique pointerait a cote.
        assertEquals(avant, apres)
    }

    @Test fun `une correction du seed est repercutee sur la base`() = runTest {
        repo.syncCatalog()
        val original = db.exerciseDao().getBySlug(SLUG)!!
        // On simule une base figee sur une ancienne version du catalogue.
        db.exerciseDao().updateExercise(
            original.copy(name = "Vieux nom", defaultRestSec = 42, primaryMuscle = MuscleGroup.CALVES),
        )

        repo.syncCatalog()

        val remis = db.exerciseDao().getBySlug(SLUG)!!
        assertEquals(original.name, remis.name)
        assertEquals(original.defaultRestSec, remis.defaultRestSec)
        assertEquals(original.primaryMuscle, remis.primaryMuscle)
        assertEquals("la ligne doit etre modifiee, pas recreee", original.id, remis.id)
    }

    @Test fun `une resynchro ne detruit ni historique ni records`() = runTest {
        repo.syncCatalog()
        val exercice = db.exerciseDao().getBySlug(SLUG)!!
        val now = Clock.System.now()

        val sessionId = db.workoutDao().insertSession(
            WorkoutSessionEntity(date = LocalDate(2026, 8, 13), startedAt = now, endedAt = now),
        )
        db.workoutDao().insertPerformedExercise(
            PerformedExerciseEntity(
                sessionId = sessionId,
                exerciseId = exercice.id,
                exerciseNameSnapshot = exercice.name,
                orderIndex = 0,
            ),
        )
        db.workoutDao().insertPr(
            PersonalRecordEntity(
                exerciseId = exercice.id,
                kind = PrKind.ONE_RM_EST,
                value = 130f,
                achievedAt = now,
                sessionId = sessionId,
            ),
        )

        // Le seed reecrit forcement cette ligne : on la desaligne d'abord pour
        // que la passe de mise a jour ait quelque chose a faire.
        db.exerciseDao().updateExercise(exercice.copy(name = "Desynchronise"))
        repo.syncCatalog()

        // Un DELETE cascaderait sur personal_record et echouerait sur le
        // RESTRICT de performed_exercise. Les deux doivent etre intacts.
        assertEquals(1, db.workoutDao().getPerformedExercises(sessionId).size)
        val pr = db.workoutDao().getCurrentPr(exercice.id, PrKind.ONE_RM_EST)
        assertEquals(130f, pr!!.value, 0.01f)
        assertEquals(exercice.name, db.exerciseDao().getBySlug(SLUG)!!.name)
    }

    @Test fun `un exercice personnalise n'est jamais ecrase`() = runTest {
        val id = repo.createCustomExercise(
            name = "Ma variante maison",
            primaryMuscle = MuscleGroup.CHEST,
        )
        val custom = db.exerciseDao().getById(id)!!
        repo.syncCatalog()
        val apres = db.exerciseDao().getById(id)!!
        assertEquals(custom, apres)
        assertTrue(apres.isCustom)
    }

    @Test fun `un exercice supprime n'est pas ressuscite`() = runTest {
        repo.syncCatalog()
        val exercice = db.exerciseDao().getBySlug(SLUG)!!
        db.exerciseDao().updateExercise(exercice.copy(isDeleted = true))

        repo.syncCatalog()

        // Le slug occupe toujours l'index unique : le reinserer echouerait, et
        // le remettre visible annulerait un choix de l'utilisateur.
        val apres = db.exerciseDao().getBySlug(SLUG)!!
        assertTrue(apres.isDeleted)
        assertEquals(exercice.id, apres.id)
        assertNull(repo.observeAll().first().firstOrNull { it.slug == SLUG })
    }

    @Test fun `un exercice personnalise recoit un slug unique`() = runTest {
        val premier = repo.createCustomExercise("Écarté à la poulie", MuscleGroup.CHEST)
        val second = repo.createCustomExercise("Écarté à la poulie", MuscleGroup.CHEST)
        val slugs = listOf(premier, second).map { db.exerciseDao().getById(it)!!.slug }

        assertEquals(2, slugs.distinct().size)
        // Accents replies : le slug reste ASCII, donc stable et comparable.
        assertEquals("custom_ecarte_a_la_poulie", slugs.first())
        assertEquals("custom_ecarte_a_la_poulie_2", slugs.last())
        assertNotNull(db.exerciseDao().getBySlug(slugs.last()))
    }

    private companion object {
        /** Un slug du catalogue, choisi parce qu'il ne changera pas. */
        const val SLUG = "bench_press"
    }
}
