package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.seed.ExerciseSeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val userDao: UserDao,
) {
    fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    fun observeAvailableInGym(gymId: Long): Flow<List<ExerciseEntity>> =
        exerciseDao.observeAvailableInGym(gymId)

    fun observeByPrimaryMuscle(muscle: MuscleGroup): Flow<List<ExerciseEntity>> =
        exerciseDao.observeByPrimaryMuscle(muscle)

    suspend fun getById(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    suspend fun getSubstitutes(exerciseId: Long, limit: Int = 5): List<ExerciseEntity> =
        exerciseDao.getSubstitutes(exerciseId, limit)

    suspend fun getEquipmentFor(exerciseId: Long): List<EquipmentEntity> =
        exerciseDao.getEquipmentFor(exerciseId)

    /**
     * Crée un exercice absent du catalogue de référence.
     *
     * Marqué `isCustom` : [syncCatalog] ne le touchera jamais, et il n'a pas de
     * visuel — l'UI affiche une initiale à la place.
     *
     * Le slug est dérivé du nom et suffixé en cas de collision : l'index unique
     * sur `slug` ferait échouer l'insertion, et un exercice du catalogue ne doit
     * jamais être écrasé par une création utilisateur.
     */
    suspend fun createCustomExercise(
        name: String,
        primaryMuscle: MuscleGroup,
        secondaryMuscles: List<MuscleGroup> = emptyList(),
        mechanics: ExerciseMechanics = ExerciseMechanics.ISOLATION,
        force: ExerciseForce = ExerciseForce.PUSH,
        equipmentIds: List<Long> = emptyList(),
        cues: String? = null,
    ): Long {
        val now = Clock.System.now()
        val taken = exerciseDao.getAllIncludingDeleted().map { it.slug }.toSet()
        val id = exerciseDao.upsertExercise(
            ExerciseEntity(
                name = name.trim(),
                slug = uniqueSlug(name, taken),
                primaryMuscle = primaryMuscle,
                secondaryMuscles = secondaryMuscles,
                mechanics = mechanics,
                force = force,
                defaultRestSec = when (mechanics) {
                    ExerciseMechanics.COMPOUND -> COMPOUND_REST_SEC
                    ExerciseMechanics.ISOLATION -> ISOLATION_REST_SEC
                },
                cues = cues?.takeIf { it.isNotBlank() },
                mediaPath = null,
                isCustom = true,
                createdAt = now,
                updatedAt = now,
            )
        )
        if (equipmentIds.isNotEmpty()) {
            exerciseDao.insertRequirements(
                equipmentIds.map { ExerciseEquipmentRequirementEntity(id, it, isRequired = true) }
            )
        }
        return id
    }

    private fun uniqueSlug(name: String, taken: Set<String>): String {
        val base = name.trim().lowercase()
            .replace(ACCENTS_REGEX) { match -> ACCENT_FOLDING[match.value] ?: match.value }
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "exercice" }
        val prefixed = "custom_$base"
        if (prefixed !in taken) return prefixed
        var suffix = 2
        while ("${prefixed}_$suffix" in taken) suffix++
        return "${prefixed}_$suffix"
    }

    /**
     * Synchronise la base sur le catalogue de référence ([ExerciseSeed]). Idempotent.
     *
     * Trois passes :
     *  1. insertion des slugs absents (élargissement du catalogue) ;
     *  2. rafraîchissement des métadonnées éditoriales des exercices déjà présents
     *     — sans quoi une install existante resterait sans visuel ni consigne après
     *     un enrichissement du catalogue ;
     *  3. (ré)association des équipements requis.
     *
     * Les exercices créés par l'utilisateur ([ExerciseEntity.isCustom]) ne sont
     * jamais modifiés. La mise à jour passe par `updateExercise` et non par
     * `upsertExercise` : ce dernier supprimerait les records personnels liés.
     */
    suspend fun syncCatalog() {
        val now = Clock.System.now()
        val seed = ExerciseSeed.items(now)
        val bySlug = exerciseDao.getAllIncludingDeleted().associateBy { it.slug }

        // 1. Nouveaux exercices.
        val toInsert = seed.filter { (ex, _) -> ex.slug !in bySlug }
        if (toInsert.isNotEmpty()) exerciseDao.insertAll(toInsert.map { it.first })

        // 2. Métadonnées des exercices déjà en base.
        seed.forEach { (seeded, _) ->
            val current = bySlug[seeded.slug] ?: return@forEach
            if (current.isCustom) return@forEach
            val merged = current.copy(
                name = seeded.name,
                primaryMuscle = seeded.primaryMuscle,
                secondaryMuscles = seeded.secondaryMuscles,
                mechanics = seeded.mechanics,
                force = seeded.force,
                defaultRestSec = seeded.defaultRestSec,
                cues = seeded.cues,
                mediaPath = seeded.mediaPath,
            )
            if (merged != current) exerciseDao.updateExercise(merged.copy(updatedAt = now))
        }

        // 3. Équipements requis. insertRequirements est en REPLACE sur la clé
        //    composite (exerciseId, equipmentId) : réécrire est sans effet de bord.
        val equipmentIdByKey = userDao.observeEquipment().first().associate { it.key to it.id }
        if (equipmentIdByKey.isEmpty()) return

        val idBySlug = exerciseDao.getAllIncludingDeleted().associate { it.slug to it.id }
        val requirements = seed.flatMap { (ex, keys) ->
            val exerciseId = idBySlug[ex.slug] ?: return@flatMap emptyList<ExerciseEquipmentRequirementEntity>()
            keys.mapNotNull { key ->
                equipmentIdByKey[key]?.let { eqId ->
                    ExerciseEquipmentRequirementEntity(exerciseId, eqId, isRequired = true)
                }
            }
        }
        if (requirements.isNotEmpty()) exerciseDao.insertRequirements(requirements)
    }

    private companion object {
        const val COMPOUND_REST_SEC = 180
        const val ISOLATION_REST_SEC = 90

        /** Repli des accents pour produire un slug ASCII stable. */
        val ACCENT_FOLDING = mapOf(
            "à" to "a", "â" to "a", "ä" to "a", "á" to "a", "ã" to "a", "å" to "a",
            "ç" to "c",
            "è" to "e", "é" to "e", "ê" to "e", "ë" to "e",
            "ì" to "i", "í" to "i", "î" to "i", "ï" to "i",
            "ñ" to "n",
            "ò" to "o", "ó" to "o", "ô" to "o", "ö" to "o", "õ" to "o",
            "ù" to "u", "ú" to "u", "û" to "u", "ü" to "u",
            "ý" to "y", "ÿ" to "y",
            "œ" to "oe", "æ" to "ae",
        )
        val ACCENTS_REGEX = Regex("[${ACCENT_FOLDING.keys.joinToString("")}]")
    }
}
