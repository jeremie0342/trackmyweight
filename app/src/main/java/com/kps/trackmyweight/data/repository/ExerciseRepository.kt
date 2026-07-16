package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.ExerciseDao
import com.kps.trackmyweight.data.db.dao.UserDao
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
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

    /**
     * Seed idempotent : insère les exercices manquants et lie leurs équipements requis.
     */
    suspend fun seedIfEmpty() {
        val now = Clock.System.now()
        val existingSlugs = exerciseDao.observeAll().first().map { it.slug }.toSet()

        val toInsert = ExerciseSeed.items(now).filter { (ex, _) -> ex.slug !in existingSlugs }
        if (toInsert.isEmpty()) return

        exerciseDao.insertAll(toInsert.map { it.first })

        val equipmentIdByKey = userDao.observeEquipment().first().associate { it.key to it.id }

        val requirements = mutableListOf<ExerciseEquipmentRequirementEntity>()
        toInsert.forEach { (ex, keys) ->
            val entity = exerciseDao.getBySlug(ex.slug) ?: return@forEach
            keys.forEach { key ->
                val eqId = equipmentIdByKey[key] ?: return@forEach
                requirements += ExerciseEquipmentRequirementEntity(entity.id, eqId, isRequired = true)
            }
        }
        if (requirements.isNotEmpty()) exerciseDao.insertRequirements(requirements)
    }
}
