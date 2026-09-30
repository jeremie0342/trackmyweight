package com.kps.trackmyweight.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.entity.FoodPriceEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.entity.GymEquipmentEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.MealEntity
import com.kps.trackmyweight.data.db.entity.MealEntryEntity
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.ProgramEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.db.enums.WaterSource
import com.kps.trackmyweight.data.db.enums.WeightSource
import com.kps.trackmyweight.data.photo.EncryptedPhotoStore
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.data.repository.NutritionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Aller-retour export → import entre deux bases distinctes.
 *
 * La base cible a volontairement un catalogue décalé (un exercice et un
 * aliment personnels créés avant le seed) : les identifiants n'y coïncident
 * pas avec ceux de la source. C'est le cas réel d'une restauration sur un
 * autre téléphone, et c'est précisément ce qui cassait l'ancien import.
 */
@RunWith(AndroidJUnit4::class)
class BackupServiceTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var ctx: Context
    private lateinit var source: TrackMyWeightDatabase
    private lateinit var target: TrackMyWeightDatabase

    @Before fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        source = newDb()
        target = newDb()
    }

    @After fun tearDown() {
        source.close()
        target.close()
    }

    @Test fun `la sauvegarde complete restaure seances, series et programmes`() = runTest {
        val ids = seedSource()
        shiftTargetCatalog()
        val payload = service(source).exportJson()

        val summary = service(target).importJson(payload)

        assertEquals(BackupRoot.SCHEMA_VERSION, summary.schemaVersion)
        assertTrue(summary.warnings.toString(), summary.warnings.isEmpty())
        val dao = target.backupDao()

        // Séance et séries : c'est ce que l'ancien import perdait.
        assertEquals(1, dao.sessions().size)
        val performed = dao.performedExercises()
        assertEquals(2, performed.size)
        assertEquals(3, dao.performedSets().size)
        val targetSlugs = dao.exercises().associate { it.id to it.slug }
        assertEquals(
            setOf(ids.catalogSlug, ids.customSlug),
            performed.map { targetSlugs[it.exerciseId] }.toSet(),
        )

        // Record, programme, template, salle.
        assertEquals(ids.catalogSlug, targetSlugs[dao.personalRecords().single().exerciseId])
        assertEquals(ids.catalogSlug, targetSlugs[dao.templateExercises().single().exerciseId])
        assertEquals(dao.templates().single().id, dao.programDays().single().templateId)
        val equipmentKeys = dao.equipment().associate { it.id to it.key }
        assertEquals(ids.equipmentKey, equipmentKeys[dao.gymEquipment().single().equipmentId])

        // Nutrition : prix et repas rattachés au bon aliment.
        val foodNames = dao.foods().associate { it.id to it.name }
        assertEquals(ids.foodName, foodNames[dao.foodPrices().single().foodId])
        assertEquals(ids.foodName, foodNames[dao.mealEntries().single().foodId])

        // Habitudes, douleurs, poids.
        val habitKeys = dao.habits().associate { it.id to it.key }
        assertEquals("creatine", habitKeys[dao.habitCompletions().single().habitId])
        assertEquals(ids.catalogSlug, targetSlugs[dao.painLogs().single().contextExerciseId])
        assertEquals(listOf(84.2f), dao.weights().map { it.weightKg })
    }

    @Test fun `restaurer remplace les donnees au lieu de les dupliquer`() = runTest {
        seedSource()
        val payload = service(source).exportJson()
        // Donnée présente avant la restauration, absente du fichier.
        target.backupDao().insertWeights(listOf(weight(LocalDate(2026, 1, 1), 99f)))

        service(target).importJson(payload)
        service(target).importJson(payload)

        val dao = target.backupDao()
        assertEquals(1, dao.sessions().size)
        assertEquals(3, dao.performedSets().size)
        assertEquals(1, dao.meals().size)
        assertEquals(1, dao.foodPrices().size)
        assertEquals(listOf(84.2f), dao.weights().map { it.weightKg })
    }

    @Test fun `un fichier incoherent n'efface rien`() = runTest {
        seedSource()
        val root = json.decodeFromString(BackupRoot.serializer(), service(source).exportJson())
        // Les séances référencent des exercices qui ne sont plus dans le fichier.
        val broken = json.encodeToString(BackupRoot.serializer(), root.copy(exercises = emptyList()))
        target.backupDao().insertWeights(listOf(weight(LocalDate(2026, 1, 1), 99f)))

        try {
            service(target).importJson(broken)
            fail("un fichier incohérent doit être refusé")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("incohérente"))
        }

        assertEquals(listOf(99f), target.backupDao().weights().map { it.weightKg })
        assertTrue(target.backupDao().sessions().isEmpty())
    }

    @Test fun `un format plus recent que l'app est refuse`() = runTest {
        try {
            service(target).importJson("""{ "schemaVersion": 99, "databaseVersion": 42, "exportedAt": "2030-01-01T00:00:00Z" }""")
            fail("un format inconnu doit être refusé")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("plus récente"))
        }
    }

    @Test fun `l'ancien format garde les series et ne cree pas de doublons`() = runTest {
        val probe = service(target)
        // Seed du catalogue via un premier import vide, pour connaître un nom d'exercice réel.
        probe.importJson(json.encodeToString(LegacyBackupRoot.serializer(), LegacyBackupRoot(exportedAt = "2026-07-16T12:00:00Z")))
        val exerciseName = target.backupDao().exercises().first { !it.isCustom }.name
        val foodName = target.backupDao().foods().first { !it.isCustom }.name

        val legacy = LegacyBackupRoot(
            exportedAt = "2026-07-16T12:00:00Z",
            workoutSessions = listOf(
                BWorkoutSession(
                    date = "2026-07-15", startedAt = "2026-07-15T18:00:00Z", endedAt = "2026-07-15T19:00:00Z",
                    totalVolumeKg = 1300f,
                    performedExercises = listOf(
                        BPerformedExercise(
                            exerciseName = exerciseName, orderIndex = 0,
                            sets = listOf(
                                BPerformedSet(1, 80f, 8, type = SetType.WORKING),
                                BPerformedSet(2, 82.5f, 8, type = SetType.WORKING),
                            ),
                        ),
                        BPerformedExercise(
                            exerciseName = "Exercice disparu du catalogue", orderIndex = 1,
                            sets = listOf(BPerformedSet(1, 20f, 12, type = SetType.WORKING)),
                        ),
                    ),
                ),
            ),
            meals = listOf(
                BMeal(
                    date = "2026-07-15", mealType = MealType.LUNCH, eatenAt = "2026-07-15T12:30:00Z",
                    entries = listOf(
                        BMealEntry(foodName, PortionMode.PRECISE_G, 100f, 100f, 200f, 20f, 10f, 5f, 1f),
                        BMealEntry("Aliment inconnu", PortionMode.PRECISE_G, 100f, 100f, 100f, 1f, 1f, 1f, 0f),
                    ),
                ),
            ),
            cardioSessions = listOf(
                BCardio("2026-07-15", "2026-07-15T07:00:00Z", null, CardioType.RUN, 1800, null, null, null, 300f, CardioSource.MANUAL),
            ),
            water = listOf(BWater("2026-07-15", "2026-07-15T08:00:00Z", 1500, WaterSource.MANUAL)),
        )
        val payload = json.encodeToString(LegacyBackupRoot.serializer(), legacy)

        val summary = probe.importJson(payload)
        probe.importJson(payload)

        val dao = target.backupDao()
        assertEquals(1, dao.sessions().size)
        assertEquals(2, dao.performedExercises().size)
        assertEquals(3, dao.performedSets().size)
        assertEquals(1, dao.meals().size)
        assertEquals(1, dao.cardioSessions().size)
        assertEquals(1, dao.water().size)
        // Seule l'entrée à l'aliment connu est restaurée, sur cet aliment précis.
        val entry = dao.mealEntries().single()
        assertEquals(foodName, dao.foods().first { it.id == entry.foodId }.name)
        assertTrue(summary.warnings.any { "Aliment inconnu" in it })
        assertTrue(summary.warnings.any { "Exercice disparu du catalogue" in it })
    }

    // ── Fixtures ────────────────────────────────────────────

    private data class SourceIds(
        val catalogSlug: String,
        val customSlug: String,
        val equipmentKey: String,
        val foodName: String,
    )

    private suspend fun seedSource(): SourceIds {
        seedCatalogs(source)
        val dao = source.backupDao()
        val t = Instant.parse("2026-07-15T18:00:00Z")
        val day = LocalDate(2026, 7, 15)

        val catalog = dao.exercises().first { !it.isCustom }
        val customId = exerciseRepo(source).createCustomExercise("Curl araignée maison", MuscleGroup.BICEPS)
        val custom = dao.exercises().first { it.id == customId }
        val equipment = dao.equipment().first()
        val food = dao.foods().first { !it.isCustom }
        val creatine = dao.habits().first { it.key == "creatine" }

        dao.insertGoals(listOf(GoalEntity(id = 1, targetWeightKg = 78f, targetDate = LocalDate(2026, 12, 31), phase = GoalPhase.CUT, isActive = true, startedAt = day, createdAt = t, updatedAt = t)))
        dao.insertGyms(listOf(GymEntity(id = 1, name = "Salle du quartier", isDefault = true, createdAt = t)))
        dao.insertGymEquipment(listOf(GymEquipmentEntity(gymId = 1, equipmentId = equipment.id)))
        dao.insertWeights(listOf(weight(day, 84.2f)))

        dao.insertTemplates(listOf(WorkoutTemplateEntity(id = 1, name = "Push", createdAt = t, updatedAt = t)))
        dao.insertTemplateExercises(listOf(TemplateExerciseEntity(id = 1, templateId = 1, exerciseId = catalog.id, orderIndex = 0, targetSets = 3)))
        dao.insertPrograms(listOf(ProgramEntity(id = 1, name = "Bloc 1", isCoachProgram = false, startDate = day, isActive = true, createdAt = t, updatedAt = t)))
        dao.insertProgramDays(listOf(ProgramDayEntity(id = 1, programId = 1, dayOfWeek = 3, templateId = 1)))

        dao.insertSessions(listOf(WorkoutSessionEntity(id = 1, date = day, startedAt = t, templateId = 1, programId = 1, gymId = 1, totalVolumeKg = 1540f)))
        dao.insertPerformedExercises(
            listOf(
                PerformedExerciseEntity(id = 1, sessionId = 1, exerciseId = catalog.id, exerciseNameSnapshot = catalog.name, orderIndex = 0),
                PerformedExerciseEntity(id = 2, sessionId = 1, exerciseId = custom.id, exerciseNameSnapshot = custom.name, orderIndex = 1),
            ),
        )
        dao.insertPerformedSets(
            listOf(
                PerformedSetEntity(id = 1, performedExerciseId = 1, setNumber = 1, weightKg = 80f, reps = 8, createdAt = t),
                PerformedSetEntity(id = 2, performedExerciseId = 1, setNumber = 2, weightKg = 85f, reps = 6, createdAt = t),
                PerformedSetEntity(id = 3, performedExerciseId = 2, setNumber = 1, weightKg = 12f, reps = 12, createdAt = t),
            ),
        )
        dao.insertPersonalRecords(listOf(PersonalRecordEntity(id = 1, exerciseId = catalog.id, kind = PrKind.ONE_RM_EST, value = 99f, achievedAt = t, sessionId = 1, setId = 2)))
        dao.insertPainLogs(listOf(PainLogEntity(id = 1, date = day, area = PainArea.SHOULDER_R, intensity = 3, contextExerciseId = catalog.id, createdAt = t)))

        dao.insertFoodPrices(listOf(FoodPriceEntity(id = 1, foodId = food.id, currency = "XOF", pricePer100g = 250f, costPerGramProtein = 12f, updatedAt = t)))
        dao.insertMeals(listOf(MealEntity(id = 1, date = day, mealType = MealType.LUNCH, eatenAt = t, createdAt = t)))
        dao.insertMealEntries(
            listOf(
                MealEntryEntity(
                    id = 1, mealId = 1, foodId = food.id, portionMode = PortionMode.PRECISE_G,
                    portionQuantity = 150f, resolvedGrams = 150f,
                    snapKcal = 300f, snapProteinG = 30f, snapCarbsG = 10f, snapFatsG = 12f, snapFiberG = 1f,
                ),
            ),
        )
        dao.insertHabitCompletions(listOf(HabitCompletionEntity(habitId = creatine.id, date = day, isDone = true)))

        return SourceIds(catalog.slug, custom.slug, equipment.key, food.name)
    }

    /** Décale les identifiants du catalogue de la base cible par rapport à la source. */
    private suspend fun shiftTargetCatalog() {
        exerciseRepo(target).createCustomExercise("Exercice local préexistant", MuscleGroup.CALVES)
        seedCatalogs(target)
    }

    private suspend fun seedCatalogs(db: TrackMyWeightDatabase) {
        GymRepository(db.userDao(), db).seedEquipmentIfEmpty()
        exerciseRepo(db).syncCatalog()
        NutritionRepository(db, db.nutritionDao()).seedIfEmpty()
        HabitRepository(db.habitDao(), db.nutritionDao()).seedIfEmpty()
    }

    private fun weight(date: LocalDate, kg: Float): WeightEntryEntity {
        val at = Instant.parse("${date}T07:00:00Z")
        return WeightEntryEntity(date = date, weightKg = kg, source = WeightSource.MANUAL, recordedAt = at, createdAt = at)
    }

    private fun exerciseRepo(db: TrackMyWeightDatabase) = ExerciseRepository(db.exerciseDao(), db.userDao())

    private fun service(db: TrackMyWeightDatabase) = BackupService(
        context = ctx,
        db = db,
        photoStore = EncryptedPhotoStore(ctx),
        gymRepo = GymRepository(db.userDao(), db),
        exerciseRepo = exerciseRepo(db),
        nutritionRepo = NutritionRepository(db, db.nutritionDao()),
        habitRepo = HabitRepository(db.habitDao(), db.nutritionDao()),
    )

    private fun newDb() = Room.inMemoryDatabaseBuilder(ctx, TrackMyWeightDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}
