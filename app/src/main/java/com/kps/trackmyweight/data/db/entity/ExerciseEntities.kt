package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.datetime.Instant

@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["slug"], unique = true),
        Index(value = ["primaryMuscle"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val slug: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val mechanics: ExerciseMechanics,
    val force: ExerciseForce,
    val cues: String? = null,
    val mediaPath: String? = null,
    val defaultRestSec: Int = 120,
    val isCustom: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "exercise_equipment_requirement",
    primaryKeys = ["exerciseId", "equipmentId"],
    foreignKeys = [
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EquipmentEntity::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("equipmentId")],
)
data class ExerciseEquipmentRequirementEntity(
    val exerciseId: Long,
    val equipmentId: Long,
    val isRequired: Boolean = true,
)

@Entity(
    tableName = "exercise_substitution",
    primaryKeys = ["exerciseId", "substituteExerciseId"],
    foreignKeys = [
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["substituteExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("substituteExerciseId")],
)
data class ExerciseSubstitutionEntity(
    val exerciseId: Long,
    val substituteExerciseId: Long,
    val priority: Int = 100,
)
