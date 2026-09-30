package com.kps.trackmyweight.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.BackupDao
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.photo.EncryptedPhotoStore
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.data.repository.NutritionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Bilan d'une restauration.
 *
 * [warnings] liste ce qui n'a pas pu être restauré. L'ancienne implémentation
 * écartait en silence les séries dont l'exercice n'était pas retrouvé : le
 * compteur affichait un succès alors que l'historique d'entraînement était
 * perdu. Tout abandon doit désormais apparaître ici.
 */
data class ImportSummary(
    val entitiesRestored: Int,
    val schemaVersion: Int,
    val warnings: List<String> = emptyList(),
)

@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TrackMyWeightDatabase,
    private val photoStore: EncryptedPhotoStore,
    private val gymRepo: GymRepository,
    private val exerciseRepo: ExerciseRepository,
    private val nutritionRepo: NutritionRepository,
    private val habitRepo: HabitRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val dao: BackupDao get() = db.backupDao()

    // ─────── EXPORT ───────

    /** Export sans les photos : toutes les tables, métadonnées de photos exclues. */
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        json.encodeToString(BackupRoot.serializer(), snapshot().copy(photos = emptyList()))
    }

    /**
     * Export complet en zip : `photos/{id}.jpg` (déchiffrées) puis `backup.json`.
     *
     * Les photos sont écrites une par une, avant le JSON : les charger toutes en
     * mémoire pour les lister d'abord ferait exploser le tas au-delà de quelques
     * dizaines de clichés. Le JSON, écrit en dernier, ne référence que celles qui
     * ont réellement pu être déchiffrées — l'import n'en attendra pas d'autres.
     */
    suspend fun exportZip(output: OutputStream) = withContext(Dispatchers.IO) {
        val root = snapshot()
        val exported = HashSet<Long>()
        ZipOutputStream(output).use { zip ->
            root.photos.forEach { p ->
                val bytes = runCatching { photoStore.readDecrypted(p.encryptedFilePath) }.getOrNull()
                    ?: return@forEach
                zip.putNextEntry(ZipEntry("photos/${p.id}.jpg"))
                zip.write(bytes)
                zip.closeEntry()
                exported += p.id
            }
            val finalRoot = root.copy(photos = root.photos.filter { it.id in exported })
            zip.putNextEntry(ZipEntry(BACKUP_JSON))
            zip.write(json.encodeToString(BackupRoot.serializer(), finalRoot).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /** Lecture de toutes les tables dans une même transaction, pour un instantané cohérent. */
    private suspend fun snapshot(): BackupRoot = db.withTransaction {
        BackupRoot(
            databaseVersion = db.openHelper.readableDatabase.version,
            exportedAt = Clock.System.now().toString(),
            equipment = dao.equipment(),
            exercises = dao.exercises(),
            exerciseRequirements = dao.exerciseRequirements(),
            exerciseSubstitutions = dao.exerciseSubstitutions(),
            foods = dao.foods(),
            foodPortionAliases = dao.foodPortionAliases(),
            habits = dao.habits(),
            profiles = dao.profiles(),
            goals = dao.goals(),
            gyms = dao.gyms(),
            gymEquipment = dao.gymEquipment(),
            weights = dao.weights(),
            measurements = dao.measurements(),
            photos = dao.photos(),
            bodyCompositions = dao.bodyCompositions(),
            exerciseMaxLoads = dao.exerciseMaxLoads(),
            templates = dao.templates(),
            templateExercises = dao.templateExercises(),
            rotationGroups = dao.rotationGroups(),
            rotationMembers = dao.rotationMembers(),
            programs = dao.programs(),
            programDays = dao.programDays(),
            sessions = dao.sessions(),
            performedExercises = dao.performedExercises(),
            performedSets = dao.performedSets(),
            personalRecords = dao.personalRecords(),
            muscleVolumes = dao.muscleVolumes(),
            cardioSessions = dao.cardioSessions(),
            cardioBlocks = dao.cardioBlocks(),
            painLogs = dao.painLogs(),
            foodPrices = dao.foodPrices(),
            meals = dao.meals(),
            mealEntries = dao.mealEntries(),
            favoriteMeals = dao.favoriteMeals(),
            favoriteMealEntries = dao.favoriteMealEntries(),
            recipes = dao.recipes(),
            recipeIngredients = dao.recipeIngredients(),
            water = dao.water(),
            alcohol = dao.alcohol(),
            dietPhases = dao.dietPhases(),
            dailyLogs = dao.dailyLogs(),
            habitCompletions = dao.habitCompletions(),
            sleep = dao.sleep(),
            steps = dao.steps(),
            heartRates = dao.heartRates(),
            weeklyReviews = dao.weeklyReviews(),
            correlations = dao.correlations(),
            projections = dao.projections(),
            appEvents = dao.appEvents(),
        )
    }

    // ─────── IMPORT ───────

    /**
     * Restaure depuis un ZIP (`backup.json` + `photos/`). Les photos sont
     * déposées dans un dossier temporaire au fil de la lecture, puis
     * re-chiffrées avec la clé de ce téléphone.
     */
    suspend fun importZip(input: InputStream): ImportSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var payload: String? = null
            val photoFiles = HashMap<String, File>()
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name == BACKUP_JSON) {
                    payload = zip.readBytes().toString(Charsets.UTF_8)
                } else {
                    // Le nom d'entrée ne sert que de clé, jamais de chemin : un zip
                    // forgé avec des « ../ » ne peut rien écrire hors du dossier.
                    val key = PHOTO_ENTRY.matchEntire(name)?.groupValues?.get(1)
                    if (key != null) {
                        val target = File(staging, "${photoFiles.size}.jpg")
                        target.outputStream().use { zip.copyTo(it) }
                        photoFiles[key] = target
                    }
                }
                zip.closeEntry()
            }
            importPayload(payload ?: error("$BACKUP_JSON manquant dans le zip"), photoFiles)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Restaure depuis un JSON seul : aucune photo n'est restaurée. */
    suspend fun importJson(payload: String): ImportSummary = withContext(Dispatchers.IO) {
        importPayload(payload, emptyMap())
    }

    private suspend fun importPayload(payload: String, photoFiles: Map<String, File>): ImportSummary {
        val version = runCatching {
            json.parseToJsonElement(payload).jsonObject["schemaVersion"]?.jsonPrimitive?.intOrNull
        }.getOrElse { error("Fichier de sauvegarde illisible : ${it.message}") } ?: LEGACY_VERSION

        if (version > BackupRoot.SCHEMA_VERSION) {
            error(
                "Sauvegarde produite par une version plus récente de l'app (format v$version). " +
                    "Mets l'app à jour avant de restaurer.",
            )
        }
        // Le catalogue doit être en place avant de recaler exercices, aliments et
        // habitudes : sur une installation neuve, rien n'a encore été seedé.
        ensureCatalogs()
        return if (version >= BackupRoot.SCHEMA_VERSION) {
            restoreFull(json.decodeFromString(BackupRoot.serializer(), payload), photoFiles)
        } else {
            restoreLegacy(json.decodeFromString(LegacyBackupRoot.serializer(), payload), photoFiles)
        }
    }

    private suspend fun ensureCatalogs() {
        gymRepo.seedEquipmentIfEmpty()
        exerciseRepo.syncCatalog()
        nutritionRepo.seedIfEmpty()
        habitRepo.seedIfEmpty()
    }

    // ─────── Format v3 ───────

    /**
     * Remplace toutes les données utilisateur par celles de la sauvegarde.
     *
     * Remplacer plutôt que fusionner : c'est ce qu'on attend d'une restauration,
     * et c'est la seule façon d'éviter les doublons quand on importe deux fois
     * le même fichier. Tout se joue dans une seule transaction — si une ligne est
     * refusée, rien n'est effacé.
     *
     * Les référentiels ne sont jamais effacés : ils portent le catalogue de
     * l'app, éventuellement plus récent que la sauvegarde. Chaque élément de la
     * sauvegarde est rattaché à son équivalent courant par sa clé naturelle
     * (slug, clé d'équipement, nom d'aliment, clé d'habitude) ; seul un élément
     * introuvable est ajouté. Les références des données utilisateur sont
     * ensuite réécrites vers ces identifiants.
     */
    private suspend fun restoreFull(root: BackupRoot, photoFiles: Map<String, File>): ImportSummary {
        val warnings = mutableListOf<String>()

        // Chiffrement des photos avant la transaction : c'est de l'I/O disque,
        // qui n'a rien à faire pendant qu'on tient le verrou d'écriture.
        val stamp = System.currentTimeMillis()
        val restoredPhotos = mutableListOf<ProgressPhotoEntity>()
        val newPhotoFiles = mutableListOf<String>()
        var missingPhotos = 0
        try {
            root.photos.forEach { p ->
                val file = photoFiles[p.id.toString()]
                if (file == null) { missingPhotos++; return@forEach }
                val (enc, thumb) = photoStore.save(file.readBytes(), "r${stamp}_${p.id}")
                newPhotoFiles += enc
                newPhotoFiles += thumb
                restoredPhotos += p.copy(encryptedFilePath = enc, thumbnailPath = thumb, overlayReferencePhotoId = null)
            }
        } catch (e: Exception) {
            deleteFiles(newPhotoFiles)
            throw e
        }
        if (missingPhotos > 0) {
            warnings += "$missingPhotos photo(s) absente(s) du fichier, non restaurée(s)."
        }

        val previousPhotos = dao.photos()
        val restored = try {
            db.withTransaction { writeFull(root, restoredPhotos) }
        } catch (e: Exception) {
            deleteFiles(newPhotoFiles)
            throw e
        }

        // Les anciennes photos ne sont effacées du disque qu'une fois la
        // restauration validée : avant, un échec les aurait rendues orphelines.
        val kept = newPhotoFiles.toSet()
        deleteFiles(
            previousPhotos.flatMap { listOf(it.encryptedFilePath, it.thumbnailPath) }.filter { it !in kept },
        )

        return ImportSummary(restored, root.schemaVersion, warnings)
    }

    private suspend fun writeFull(root: BackupRoot, photos: List<ProgressPhotoEntity>): Int {
        // ── Référentiels : rattachement par clé naturelle ──
        val equipmentIds = HashMap<Long, Long>()
        val currentEquipment = dao.equipment().associateBy { it.key }
        root.equipment.forEach { e ->
            equipmentIds[e.id] = currentEquipment[e.key]?.id ?: dao.insertEquipment(e.copy(id = 0))
        }

        val exerciseIds = HashMap<Long, Long>()
        val addedExercises = HashSet<Long>()
        val currentExercises = dao.exercises().associateBy { it.slug }
        root.exercises.forEach { ex ->
            val current = currentExercises[ex.slug]
            exerciseIds[ex.id] = if (current != null) {
                current.id
            } else {
                addedExercises += ex.id
                dao.insertExercise(ex.copy(id = 0))
            }
        }
        // Liens de catalogue : uniquement pour les exercices qu'on vient d'ajouter.
        // Ceux du catalogue courant ont déjà les leurs, à jour.
        dao.insertExerciseRequirements(
            root.exerciseRequirements
                .filter { it.exerciseId in addedExercises }
                .mapNotNull { r ->
                    val eq = equipmentIds[r.equipmentId] ?: return@mapNotNull null
                    r.copy(exerciseId = exerciseIds.ref(r.exerciseId, "exercice"), equipmentId = eq)
                },
        )
        dao.insertExerciseSubstitutions(
            root.exerciseSubstitutions
                .filter { it.exerciseId in addedExercises || it.substituteExerciseId in addedExercises }
                .mapNotNull { s ->
                    val a = exerciseIds[s.exerciseId] ?: return@mapNotNull null
                    val b = exerciseIds[s.substituteExerciseId] ?: return@mapNotNull null
                    s.copy(exerciseId = a, substituteExerciseId = b)
                },
        )

        val foodIds = HashMap<Long, Long>()
        val addedFoods = HashSet<Long>()
        val currentFoods = dao.foods().associateBy { it.name to it.isCustom }
        root.foods.forEach { f ->
            val current = currentFoods[f.name to f.isCustom]
            foodIds[f.id] = if (current != null) {
                current.id
            } else {
                addedFoods += f.id
                dao.insertFood(f.copy(id = 0))
            }
        }
        dao.insertFoodPortionAliases(
            root.foodPortionAliases
                .filter { it.foodId in addedFoods }
                .map { it.copy(id = 0, foodId = foodIds.ref(it.foodId, "aliment")) },
        )

        // Habitudes : les réglages de la sauvegarde (active, cible, ordre) priment,
        // mais l'identifiant reste celui de la ligne existante.
        val habitIds = HashMap<Long, Long>()
        val currentHabits = dao.habits().associateBy { it.key }
        root.habits.forEach { h ->
            val current = currentHabits[h.key]
            habitIds[h.id] = if (current != null) {
                dao.updateHabit(h.copy(id = current.id))
                current.id
            } else {
                dao.insertHabit(h.copy(id = 0))
            }
        }

        // ── Données utilisateur : on vide, puis on réinsère à l'identique ──
        val sqlite = db.openHelper.writableDatabase
        USER_TABLES_CHILD_FIRST.forEach { table -> sqlite.execSQL("DELETE FROM `$table`") }

        dao.insertProfiles(root.profiles)
        dao.insertGoals(root.goals)
        dao.insertGyms(root.gyms)
        dao.insertGymEquipment(root.gymEquipment.map { it.copy(equipmentId = equipmentIds.ref(it.equipmentId, "équipement")) })

        dao.insertWeights(root.weights)
        dao.insertMeasurements(root.measurements)
        dao.insertPhotos(photos)
        val photoIds = photos.map { it.id }.toSet()
        root.photos.forEach { p ->
            val ref = p.overlayReferencePhotoId ?: return@forEach
            if (p.id in photoIds && ref in photoIds) dao.setPhotoOverlay(p.id, ref)
        }
        dao.insertBodyCompositions(root.bodyCompositions)

        dao.insertExerciseMaxLoads(root.exerciseMaxLoads.map { it.copy(exerciseId = exerciseIds.ref(it.exerciseId, "exercice")) })
        dao.insertTemplates(root.templates)
        dao.insertTemplateExercises(root.templateExercises.map { it.copy(exerciseId = exerciseIds.ref(it.exerciseId, "exercice")) })
        dao.insertRotationGroups(root.rotationGroups)
        dao.insertRotationMembers(root.rotationMembers)
        dao.insertPrograms(root.programs)
        dao.insertProgramDays(root.programDays)
        dao.insertCardioSessions(root.cardioSessions)
        dao.insertCardioBlocks(root.cardioBlocks)
        dao.insertSessions(root.sessions)
        dao.insertPerformedExercises(root.performedExercises.map { it.copy(exerciseId = exerciseIds.ref(it.exerciseId, "exercice")) })
        dao.insertPerformedSets(root.performedSets)
        dao.insertPersonalRecords(root.personalRecords.map { it.copy(exerciseId = exerciseIds.ref(it.exerciseId, "exercice")) })
        dao.insertMuscleVolumes(root.muscleVolumes)
        dao.insertPainLogs(root.painLogs.map { p -> p.copy(contextExerciseId = p.contextExerciseId?.let { exerciseIds[it] }) })

        dao.insertFoodPrices(root.foodPrices.map { it.copy(foodId = foodIds.ref(it.foodId, "aliment")) })
        dao.insertMeals(root.meals)
        dao.insertMealEntries(root.mealEntries.map { it.copy(foodId = foodIds.ref(it.foodId, "aliment")) })
        dao.insertFavoriteMeals(root.favoriteMeals)
        dao.insertFavoriteMealEntries(root.favoriteMealEntries.map { it.copy(foodId = foodIds.ref(it.foodId, "aliment")) })
        dao.insertRecipes(root.recipes)
        dao.insertRecipeIngredients(root.recipeIngredients.map { it.copy(foodId = foodIds.ref(it.foodId, "aliment")) })
        dao.insertWater(root.water)
        dao.insertAlcohol(root.alcohol)
        dao.insertDietPhases(root.dietPhases)

        dao.insertDailyLogs(root.dailyLogs)
        dao.insertHabitCompletions(root.habitCompletions.map { it.copy(habitId = habitIds.ref(it.habitId, "habitude")) })
        dao.insertSleep(root.sleep)
        dao.insertSteps(root.steps)
        dao.insertHeartRates(root.heartRates)

        dao.insertWeeklyReviews(root.weeklyReviews)
        dao.insertCorrelations(root.correlations)
        dao.insertProjections(root.projections)
        dao.insertAppEvents(root.appEvents)

        return listOf(
            root.profiles, root.goals, root.gyms, root.gymEquipment, root.weights, root.measurements,
            photos, root.bodyCompositions, root.exerciseMaxLoads, root.templates, root.templateExercises,
            root.rotationGroups, root.rotationMembers, root.programs, root.programDays, root.cardioSessions,
            root.cardioBlocks, root.sessions, root.performedExercises, root.performedSets,
            root.personalRecords, root.muscleVolumes, root.painLogs, root.foodPrices, root.meals,
            root.mealEntries, root.favoriteMeals, root.favoriteMealEntries, root.recipes,
            root.recipeIngredients, root.water, root.alcohol, root.dietPhases, root.dailyLogs,
            root.habitCompletions, root.sleep, root.steps, root.heartRates, root.weeklyReviews,
            root.correlations, root.projections, root.appEvents,
        ).sumOf { it.size }
    }

    /**
     * Toute référence exportée vise une ligne elle aussi exportée. Une clé
     * introuvable signale donc un fichier corrompu ou modifié à la main : on
     * interrompt la restauration plutôt que d'écrire une donnée orpheline.
     */
    private fun Map<Long, Long>.ref(oldId: Long, what: String): Long =
        this[oldId] ?: error("Sauvegarde incohérente : $what #$oldId référencé mais absent du fichier.")

    // ─────── Ancien format (v1-v2) ───────

    /**
     * Import de l'ancien format, qui ne contient qu'une partie des données.
     *
     * Contrairement au format v3, on fusionne au lieu de remplacer : effacer les
     * programmes, salles ou records pour restaurer un fichier qui ne les contient
     * pas détruirait des données. Pour que réimporter le même fichier ne crée pas
     * de doublons, chaque élément déjà présent (même date, même horodatage, même
     * nom) est ignoré.
     */
    private suspend fun restoreLegacy(root: LegacyBackupRoot, photoFiles: Map<String, File>): ImportSummary {
        val userDao = db.userDao()
        val bodyDao = db.bodyDao()
        val workoutDao = db.workoutDao()
        val nutritionDao = db.nutritionDao()
        val habitDao = db.habitDao()
        val warnings = mutableListOf<String>()
        val createdExercises = LinkedHashSet<String>()
        val unknownFoods = LinkedHashSet<String>()
        var restored = 0

        db.withTransaction {
            root.profile?.let {
                userDao.upsertProfile(it.toEntity())
                restored++
            }
            root.activeGoal?.let { g ->
                if (dao.countSameActiveGoal(g.targetWeightKg, g.targetDate, g.startedAt) == 0) {
                    userDao.deactivateAllGoals(Clock.System.now())
                    userDao.insertGoal(g.toEntity())
                    restored++
                }
            }
            root.weights.forEach { w -> bodyDao.upsertWeight(w.toEntity()); restored++ }
            root.measurements.forEach { m -> bodyDao.upsertMeasurement(m.toEntity()); restored++ }

            val importedFoodIds = HashMap<String, Long>()
            root.customFoods.forEach { f ->
                importedFoodIds[f.name] = dao.customFoodIdByName(f.name)
                    ?: nutritionDao.upsertFood(f.toEntity()).also { restored++ }
            }
            // Correspondance exacte uniquement. L'ancienne version retombait sur le
            // premier résultat de recherche plein texte : un repas pouvait être
            // rattaché en silence à un autre aliment, avec des macros fausses.
            suspend fun foodId(name: String): Long? =
                (importedFoodIds[name] ?: dao.foodIdByName(name)).also { if (it == null) unknownFoods += name }

            root.meals.forEach { m ->
                val entity = m.toEntity()
                if (dao.countMeals(entity.date.toString(), entity.mealType.name, entity.eatenAt) > 0) return@forEach
                val mealId = nutritionDao.insertMeal(entity)
                restored++
                m.entries.forEach { e ->
                    val id = foodId(e.foodName) ?: return@forEach
                    nutritionDao.insertMealEntry(e.toEntity(mealId, id))
                    restored++
                }
            }
            root.favorites.forEach { f ->
                if (dao.countFavorites(f.name) > 0) return@forEach
                val id = nutritionDao.insertFavorite(f.toEntity())
                val entries = f.entries.mapNotNull { e -> foodId(e.foodName)?.let { e.toEntity(id, it) } }
                if (entries.isNotEmpty()) nutritionDao.setFavoriteEntries(entries)
                restored++
            }

            root.workoutSessions.forEach { ws ->
                val entity = ws.toEntity()
                if (dao.countSessionsStartedAt(entity.startedAt) > 0) return@forEach
                val sessId = workoutDao.insertSession(entity)
                restored++
                ws.performedExercises.forEach { pe ->
                    val exerciseId = dao.exerciseIdByName(pe.exerciseName)
                        ?: createLegacyExercise(pe.exerciseName).also { createdExercises += pe.exerciseName }
                    val peId = workoutDao.insertPerformedExercise(pe.toEntity(sessId, exerciseId))
                    restored++
                    pe.sets.forEach { s ->
                        workoutDao.insertPerformedSet(s.toEntity(peId).copy(createdAt = entity.startedAt))
                        restored++
                    }
                }
            }
            root.cardioSessions.forEach { c ->
                val entity = c.toEntity()
                if (dao.countCardioStartedAt(entity.startedAt) > 0) return@forEach
                workoutDao.insertCardio(entity)
                restored++
            }
            root.sleep.forEach { s -> habitDao.upsertSleep(s.toEntity()); restored++ }
            root.steps.forEach { s -> habitDao.upsertSteps(s.toEntity()); restored++ }
            // L'ancien export n'avait qu'un total par jour : on ne l'ajoute que sur
            // un jour vide, sans quoi il s'additionnerait aux prises déjà saisies.
            root.water.forEach { w ->
                if (dao.countWaterOn(w.date) > 0) return@forEach
                nutritionDao.insertWater(w.toEntity())
                restored++
            }
            root.dailyLogs.forEach { d -> habitDao.upsertDailyLog(d.toEntity()); restored++ }
            // Toutes les habitudes, actives ou non : une habitude désactivée depuis
            // l'export garde son historique.
            val habitByKey = dao.habits().associate { it.key to it.id }
            root.habitCompletions.forEach { h ->
                val id = habitByKey[h.habitKey] ?: return@forEach
                habitDao.upsertCompletion(h.toEntity(id))
                restored++
            }
            root.activePhase?.let { p ->
                nutritionDao.switchActivePhase(p.toEntity())
                restored++
            }
        }

        // Photos hors transaction (I/O disque). La paire (date, angle) est unique
        // en base : une photo déjà présente pour ce créneau est conservée.
        val taken = dao.photos().map { it.date to it.angle }.toHashSet()
        val stamp = System.currentTimeMillis()
        var missingPhotos = 0
        root.photos.forEach { bp ->
            val date = LocalDate.parse(bp.date)
            val angle = PhotoAngle.valueOf(bp.angle)
            if ((date to angle) in taken) return@forEach
            val file = photoFiles[bp.fileKey]
            if (file == null) { missingPhotos++; return@forEach }
            val (encPath, thumbPath) = photoStore.save(file.readBytes(), "r${stamp}_${bp.fileKey}")
            bodyDao.insertPhoto(
                ProgressPhotoEntity(
                    date = date,
                    angle = angle,
                    encryptedFilePath = encPath,
                    thumbnailPath = thumbPath,
                    overlayReferencePhotoId = null,
                    widthPx = bp.widthPx,
                    heightPx = bp.heightPx,
                    createdAt = Instant.parse(bp.createdAt),
                ),
            )
            taken += date to angle
            restored++
        }

        if (createdExercises.isNotEmpty()) {
            warnings += "Exercice(s) absent(s) du catalogue, recréé(s) comme exercices personnels " +
                "(groupe musculaire à vérifier) : ${createdExercises.joinToString()}."
        }
        if (unknownFoods.isNotEmpty()) {
            warnings += "Aliment(s) introuvable(s), entrées de repas ignorées : ${unknownFoods.joinToString()}."
        }
        if (missingPhotos > 0) {
            warnings += "$missingPhotos photo(s) absente(s) du fichier, non restaurée(s)."
        }
        return ImportSummary(restored, root.schemaVersion, warnings)
    }

    /**
     * L'ancien format ne désignait l'exercice que par son nom. S'il n'existe plus
     * dans le catalogue (renommé depuis), on le recrée plutôt que de jeter les
     * séries — c'est ce que faisait l'ancienne implémentation, sans le dire.
     * Le groupe musculaire est inconnu : l'avertissement invite à le corriger.
     */
    private suspend fun createLegacyExercise(name: String): Long =
        dao.exerciseIdByName(name) ?: exerciseRepo.createCustomExercise(name = name, primaryMuscle = MuscleGroup.CHEST)

    private fun deleteFiles(paths: Collection<String>) {
        paths.forEach { runCatching { File(it).delete() } }
    }

    private companion object {
        const val BACKUP_JSON = "backup.json"
        const val LEGACY_VERSION = 2
        val PHOTO_ENTRY = Regex("photos/([A-Za-z0-9_-]+)\\.jpg")

        /**
         * Tables de données utilisateur, enfants avant parents.
         *
         * L'ordre compte : plusieurs clés étrangères sont en RESTRICT, que SQLite
         * applique immédiatement, même avec des contraintes différées.
         */
        val USER_TABLES_CHILD_FIRST = listOf(
            "personal_record", "exercise_max_load", "performed_set", "performed_exercise",
            "workout_session", "program_day", "program", "template_rotation_member",
            "template_rotation_group", "template_exercise", "workout_template",
            "gym_equipment", "gym", "cardio_block", "cardio_session", "pain_log",
            "muscle_group_volume_weekly",
            "meal_entry", "meal", "favorite_meal_entry", "favorite_meal",
            "recipe_ingredient", "recipe", "food_price", "water_entry", "alcohol_entry", "diet_phase",
            "body_composition_snapshot", "body_measurement_session", "progress_photo", "weight_entry",
            "habit_completion", "daily_log", "sleep_entry", "steps_entry", "heart_rate_sample",
            "weekly_review", "correlation_insight", "projection_snapshot", "app_event",
            "goal", "user_profile",
        )
    }
}
