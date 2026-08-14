package com.kps.trackmyweight.data.db

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Les migrations v5 → v9, exécutées contre une vraie base v5.
 *
 * Elles ont été écrites à la main et n'avaient jamais tourné : le schéma de la
 * v5 n'existait nulle part, faute d'avoir été commité. Il a fallu le
 * reconstruire en recompilant l'arbre à la révision c779a16
 * (`.github/workflows/extract-schemas.yml`). Sans ce point de départ, on ne
 * teste que sa propre relecture du passé.
 *
 * Ce que ça protège concrètement : quelqu'un qui a déjà installé un APK
 * antérieur et qui met à jour. `DatabaseModule` n'autorise plus le repli
 * destructif qu'en dessous de la v5 — au-delà, une migration fausse ne perd pas
 * les données en silence, elle empêche l'application de démarrer.
 *
 * `runMigrationsAndValidate` compare le schéma obtenu à `9.json`, colonne par
 * colonne, index par index. Il ne suffit donc pas que les migrations
 * s'exécutent : elles doivent produire exactement le schéma que Room attend.
 * Les assertions qui suivent vérifient l'autre moitié, que la validation ne
 * couvre pas — que les données sont toujours là.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrackMyWeightDatabase::class.java,
    )

    @Test
    fun migrate5To9_preservesUserData() {
        helper.createDatabase(TEST_DB, 5).use { db -> seedV5(db) }

        // validateDroppedTables = true : signale aussi une table oubliee.
        val db = helper.runMigrationsAndValidate(TEST_DB, 9, true, *ALL_MIGRATIONS)

        // — La seance et son historique ont survecu —
        assertEquals(1, db.count("SELECT COUNT(*) FROM workout_session"))
        assertEquals("2026-08-01", db.string("SELECT date FROM workout_session"))
        assertEquals(1250.0, db.double("SELECT totalVolumeKg FROM workout_session"), 0.01)
        assertEquals("Développé couché", db.string("SELECT exerciseNameSnapshot FROM performed_exercise"))

        assertEquals(3, db.count("SELECT COUNT(*) FROM performed_set"))
        assertEquals(85.0, db.double("SELECT weightKg FROM performed_set WHERE setNumber = 3"), 0.01)
        assertEquals(6, db.count("SELECT reps FROM performed_set WHERE setNumber = 3"))

        // — v5 → v6 : les colonnes d'objectif existent et sont nulles —
        // Une seance enregistree avant la refonte n'avait pas d'objectif ; lui
        // en inventer un serait pire que de ne rien afficher.
        db.query("SELECT targetSets, targetRepsMin, targetRepsMax, targetRpe, targetWeightKg, restSecOverride FROM performed_exercise")
            .use { c ->
                assertTrue(c.moveToFirst())
                repeat(c.columnCount) { i ->
                    assertTrue("${c.getColumnName(i)} devrait etre null", c.isNull(i))
                }
            }

        // — v6 → v7 : la table de charges max est creee et utilisable —
        // La creer ne suffit pas : si la cle etrangere ou une colonne NOT NULL
        // etait fausse, l'insertion echouerait ici et pas au test de schema.
        assertEquals(0, db.count("SELECT COUNT(*) FROM exercise_max_load"))
        db.execSQL(
            "INSERT INTO exercise_max_load (exerciseId, oneRmKg, source, sourceWeightKg, sourceReps, measuredAt) " +
                "VALUES (1, 102.5, 'ESTIMATED', 85.0, 6, 1754006400000)",
        )
        assertEquals(102.5, db.double("SELECT oneRmKg FROM exercise_max_load"), 0.01)

        // — v7 → v8 : superset des deux cotes, sans regrouper l'existant —
        assertNull(db.nullableLong("SELECT supersetGroup FROM performed_exercise"))
        assertNull(db.nullableLong("SELECT supersetGroup FROM template_exercise"))

        // — v8 → v9 : la creatine est activee sans toucher aux autres —
        assertEquals(1, db.count("SELECT isActive FROM habit_definition WHERE key = 'creatine'"))
        assertEquals(1, db.count("SELECT isActive FROM habit_definition WHERE key = 'water'"))
        assertEquals(0, db.count("SELECT isActive FROM habit_definition WHERE key = 'stretching'"))

        db.close()
    }

    /**
     * Une base v5 vide doit migrer aussi bien qu'une base remplie.
     *
     * `MIGRATION_8_9` fait un UPDATE sur une ligne qui peut ne pas exister —
     * install neuve jamais lancee, ou habitude supprimee par l'utilisateur.
     */
    @Test
    fun migrate5To9_onEmptyDatabase() {
        helper.createDatabase(TEST_DB, 5).close()
        helper.runMigrationsAndValidate(TEST_DB, 9, true, *ALL_MIGRATIONS).use { db ->
            assertEquals(0, db.count("SELECT COUNT(*) FROM habit_definition"))
            assertEquals(0, db.count("SELECT COUNT(*) FROM exercise_max_load"))
        }
    }

    /** Une seance realiste : un exercice, trois series montantes, deux habitudes. */
    private fun seedV5(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO exercise (id, name, slug, primaryMuscle, secondaryMuscles, mechanics, force,
                                  cues, mediaPath, defaultRestSec, isCustom, isDeleted, createdAt, updatedAt)
            VALUES (1, 'Développé couché', 'bench-press', 'CHEST', 'TRICEPS,SHOULDERS_FRONT',
                    'COMPOUND', 'PUSH', 'Omoplates serrées', NULL, 120, 0, 0, $EPOCH, $EPOCH)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO workout_session (id, date, startedAt, endedAt, totalVolumeKg, totalCalories, isCoachProgram)
            VALUES (1, '2026-08-01', $EPOCH, ${EPOCH + 3_600_000}, 1250.0, 320.0, 0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO performed_exercise (id, sessionId, exerciseId, exerciseNameSnapshot, orderIndex, notes)
            VALUES (1, 1, 1, 'Développé couché', 0, 'Bonne séance')
            """.trimIndent(),
        )
        listOf(1 to 75.0, 2 to 80.0, 3 to 85.0).forEach { (setNumber, weight) ->
            db.execSQL(
                """
                INSERT INTO performed_set (performedExerciseId, setNumber, weightKg, reps, rpe, type,
                                           isPrCandidate, createdAt)
                VALUES (1, $setNumber, $weight, ${9 - setNumber}, 8.0, 'WORKING', 0,
                        ${EPOCH + setNumber * 300_000})
                """.trimIndent(),
            )
        }
        // La creatine desactivee est le cas que MIGRATION_8_9 doit corriger ;
        // les deux autres servent de temoins, dans les deux sens.
        db.execSQL(
            """
            INSERT INTO habit_definition (key, displayName, isActive, orderIndex) VALUES
                ('creatine', 'Créatine', 0, 0),
                ('water', 'Eau', 1, 1),
                ('stretching', 'Étirements', 0, 2)
            """.trimIndent(),
        )
    }

    private companion object {
        const val TEST_DB = "migration-test"

        /** 2026-08-01T00:00:00Z, en millisecondes. */
        const val EPOCH = 1_754_006_400_000L
    }
}

// ─────────────────────────────────────────────────────────────
// Lectures brutes : apres migration, les entites Room ne sont pas
// utilisables directement, on interroge donc au curseur.
// ─────────────────────────────────────────────────────────────

private fun SupportSQLiteDatabase.count(sql: String): Int = single(sql) { it.getInt(0) } ?: 0

private fun SupportSQLiteDatabase.double(sql: String): Double = single(sql) { it.getDouble(0) } ?: 0.0

private fun SupportSQLiteDatabase.string(sql: String): String? = single(sql) { it.getString(0) }

private fun SupportSQLiteDatabase.nullableLong(sql: String): Long? =
    single(sql) { if (it.isNull(0)) null else it.getLong(0) }

private fun <T> SupportSQLiteDatabase.single(sql: String, read: (Cursor) -> T?): T? =
    query(sql).use { c -> if (c.moveToFirst()) read(c) else null }
