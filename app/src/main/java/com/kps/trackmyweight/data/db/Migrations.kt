package com.kps.trackmyweight.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations Room explicites.
 *
 * Jusqu'à la v5 le projet s'appuyait uniquement sur `fallbackToDestructiveMigration()`,
 * c'est-à-dire que **tout changement de schéma effaçait l'intégralité des données
 * de l'utilisateur** — pesées, photos, historique de séances, records. Acceptable
 * en tout début de développement, plus du tout dès lors que l'app est utilisée au
 * quotidien.
 *
 * À partir de la v6, chaque évolution de schéma doit être accompagnée de sa
 * migration ici. Le repli destructif reste autorisé uniquement pour les versions
 * antérieures à 5 (voir `DatabaseModule`), pour lesquelles aucune donnée n'était
 * censée être conservée de toute façon.
 */

/**
 * v5 → v6 : snapshot du plan sur `performed_exercise`.
 *
 * Permet d'afficher les objectifs (séries, fourchette de reps, RPE, charge, repos)
 * pendant la séance, et de les ajuster à la préparation sans modifier le template.
 * Colonnes nullables sans valeur par défaut : les séances déjà enregistrées
 * restent valides, simplement sans objectif.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN targetSets INTEGER")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN targetRepsMin INTEGER")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN targetRepsMax INTEGER")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN targetRpe REAL")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN targetWeightKg REAL")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN restSecOverride INTEGER")
    }
}

/**
 * v6 → v7 : historique des charges maximales par exercice.
 *
 * Table purement additive : aucune donnée existante n'est touchée. Le schéma
 * doit correspondre exactement à `ExerciseMaxLoadEntity`, sans quoi Room refuse
 * d'ouvrir la base au démarrage.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_max_load` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `exerciseId` INTEGER NOT NULL,
                `oneRmKg` REAL NOT NULL,
                `source` TEXT NOT NULL,
                `sourceWeightKg` REAL,
                `sourceReps` INTEGER,
                `measuredAt` INTEGER NOT NULL,
                `notes` TEXT,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_exercise_max_load_exerciseId_measuredAt` " +
                "ON `exercise_max_load` (`exerciseId`, `measuredAt`)"
        )
    }
}

/**
 * v7 → v8 : groupement des exercices en superset.
 *
 * Une seule colonne nullable de chaque côté : le plan (template) et son
 * instantané (séance exécutée). Les lignes existantes restent des exercices
 * isolés, ce qui est le comportement actuel.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE template_exercise ADD COLUMN supersetGroup INTEGER")
        db.execSQL("ALTER TABLE performed_exercise ADD COLUMN supersetGroup INTEGER")
    }
}

/** Toutes les migrations connues, dans l'ordre. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
