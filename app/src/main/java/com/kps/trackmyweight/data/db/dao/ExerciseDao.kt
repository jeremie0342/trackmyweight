package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEquipmentRequirementEntity
import com.kps.trackmyweight.data.db.entity.ExerciseSubstitutionEntity
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    // @Insert(REPLACE) car unique(slug).
    // ATTENTION : sur conflit, Room fait DELETE + INSERT. Les FK ON DELETE CASCADE
    // qui pointent vers `exercise` (personal_record notamment) partiraient avec, et
    // le RESTRICT de performed_exercise ferait échouer l'opération. Ne l'utiliser que
    // pour une véritable insertion — pour modifier une ligne existante, [updateExercise].
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    /** Modifie une ligne existante par sa clé primaire, sans DELETE préalable. */
    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercise WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    /**
     * Inclut les exercices soft-deleted, contrairement à [observeAll].
     * Nécessaire à la synchronisation du catalogue : un slug soft-deleted occupe
     * toujours l'index unique, il ne faut pas tenter de le réinsérer.
     */
    @Query("SELECT * FROM exercise")
    suspend fun getAllIncludingDeleted(): List<ExerciseEntity>

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

    /** Équipements requis par un exercice, pour la fiche exercice. */
    @Query("""
        SELECT eq.* FROM equipment eq
        INNER JOIN exercise_equipment_requirement req ON req.equipmentId = eq.id
        WHERE req.exerciseId = :exerciseId
        ORDER BY eq.displayName
    """)
    suspend fun getEquipmentFor(exerciseId: Long): List<EquipmentEntity>

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
