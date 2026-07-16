package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
import com.kps.trackmyweight.data.db.entity.ExerciseSubstitutionEntity
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // @Insert(REPLACE) car unique(slug).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Query("SELECT * FROM exercise WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE slug = :slug LIMIT 1")
    suspend fun getBySlug(slug: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE isDeleted = 0 AND primaryMuscle = :muscle ORDER BY name")
    fun observeByPrimaryMuscle(muscle: MuscleGroup): Flow<List<ExerciseEntity>>

    /**
     * Exercices faisables avec un ensemble d'équipements dispo dans une salle.
     * Un exercice apparaît si TOUS ses équipements requis sont dispo.
     */
    @Query("""
        SELECT e.* FROM exercise e
        WHERE e.isDeleted = 0
        AND NOT EXISTS (
            SELECT 1 FROM exercise_equipment_requirement req
            WHERE req.exerciseId = e.id
            AND req.isRequired = 1
            AND req.equipmentId NOT IN (
                SELECT equipmentId FROM gym_equipment WHERE gymId = :gymId
            )
        )
        ORDER BY e.name
    """)
    fun observeAvailableInGym(gymId: Long): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirements(reqs: List<ExerciseEquipmentRequirementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubstitutions(subs: List<ExerciseSubstitutionEntity>)

    @Query("""
        SELECT e.* FROM exercise e
        INNER JOIN exercise_substitution s ON s.substituteExerciseId = e.id
        WHERE s.exerciseId = :exerciseId AND e.isDeleted = 0
        ORDER BY s.priority ASC
        LIMIT :limit
    """)
    suspend fun getSubstitutes(exerciseId: Long, limit: Int = 5): List<ExerciseEntity>
}
