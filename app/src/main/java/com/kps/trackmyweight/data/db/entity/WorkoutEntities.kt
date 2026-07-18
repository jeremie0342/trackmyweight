package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.CardioSource
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.db.enums.SetType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

// ─────────────────────────────────────────────────────────────
// Templates & programmes
// ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "workout_template",
    indices = [Index(value = ["isArchived"])],
)
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val colorHex: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "template_exercise",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["templateId", "orderIndex"]),
        Index("exerciseId"),
    ],
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetRpe: Float? = null,
    val targetWeightKg: Float? = null,
    val restSecOverride: Int? = null,
    val notes: String? = null,
)

@Entity(
    tableName = "template_rotation_group",
    indices = [Index(value = ["dayOfWeek"])],
)
data class TemplateRotationGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 1 = Lundi ... 7 = Dimanche (ISO). */
    val dayOfWeek: Int,
)

@Entity(
    tableName = "template_rotation_member",
    primaryKeys = ["rotationGroupId", "orderInRotation"],
    foreignKeys = [
        ForeignKey(entity = TemplateRotationGroupEntity::class, parentColumns = ["id"], childColumns = ["rotationGroupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WorkoutTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId")],
)
data class TemplateRotationMemberEntity(
    val rotationGroupId: Long,
    val templateId: Long,
    val orderInRotation: Int,
)

@Entity(tableName = "program")
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCoachProgram: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val mesocycleWeeks: Int = 5,
    val notes: String? = null,
    val isActive: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "program_day",
    foreignKeys = [
        ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WorkoutTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TemplateRotationGroupEntity::class, parentColumns = ["id"], childColumns = ["rotationGroupId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["programId", "dayOfWeek"]),
        Index("templateId"),
        Index("rotationGroupId"),
    ],
)
data class ProgramDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val dayOfWeek: Int,
    val templateId: Long? = null,
    val rotationGroupId: Long? = null,
    val isRest: Boolean = false,
    val notes: String? = null,
)

// ─────────────────────────────────────────────────────────────
// Séances exécutées
// ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplateEntity::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = GymEntity::class, parentColumns = ["id"], childColumns = ["gymId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["date"]),
        Index("templateId"),
        Index("programId"),
        Index("gymId"),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val templateId: Long? = null,
    val programId: Long? = null,
    val gymId: Long? = null,
    val sessionRpe: Float? = null,
    val mood: Int? = null,
    val notes: String? = null,
    val totalVolumeKg: Float = 0f,
    val totalCalories: Float = 0f,
    val isCoachProgram: Boolean = false,
    val deletedAt: Instant? = null,
)

@Entity(
    tableName = "performed_exercise",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["sessionId", "orderIndex"]),
        Index("exerciseId"),
    ],
)
data class PerformedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    /** Snapshot du nom au moment de la séance (pour conservation historique). */
    val exerciseNameSnapshot: String,
    val orderIndex: Int,
    val notes: String? = null,
)

@Entity(
    tableName = "performed_set",
    foreignKeys = [
        ForeignKey(entity = PerformedExerciseEntity::class, parentColumns = ["id"], childColumns = ["performedExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["performedExerciseId", "setNumber"])],
)
data class PerformedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val performedExerciseId: Long,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val rpe: Float? = null,
    val type: SetType = SetType.WORKING,
    val restBeforeSec: Int? = null,
    val isPrCandidate: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "personal_record",
    foreignKeys = [
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PerformedSetEntity::class, parentColumns = ["id"], childColumns = ["setId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["exerciseId", "kind", "achievedAt"]),
        Index("sessionId"),
        Index("setId"),
    ],
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val kind: PrKind,
    val value: Float,
    val referenceValue: Float? = null,
    val achievedAt: Instant,
    val sessionId: Long,
    val setId: Long? = null,
)

@Entity(
    tableName = "muscle_group_volume_weekly",
    indices = [Index(value = ["isoWeek", "muscleGroup"], unique = true)],
)
data class MuscleGroupVolumeWeeklyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Format ISO "2026-W29". */
    val isoWeek: String,
    val muscleGroup: MuscleGroup,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Float,
    val mev: Int,
    val mav: Int,
    val mrv: Int,
    val computedAt: Instant,
)

// ─────────────────────────────────────────────────────────────
// Cardio & douleur
// ─────────────────────────────────────────────────────────────

@Entity(
    tableName = "cardio_session",
    indices = [Index(value = ["date"])],
)
data class CardioSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val type: CardioType,
    val durationSec: Int,
    val distanceM: Float? = null,
    val avgSpeedKmh: Float? = null,
    val avgRpe: Float? = null,
    /** JSON `{"z1": secondes, "z2": secondes, ...}`. */
    val zonesBreakdown: Map<String, Int>? = null,
    val caloriesEstimated: Float,
    val source: CardioSource = CardioSource.MANUAL,
    val notes: String? = null,
    val createdAt: Instant,
)

/**
 * Bloc individuel dans une CardioSession multi-blocs.
 * Ex : 20min elliptique + 30min tapis + 5min corde = 3 blocs.
 */
@Entity(
    tableName = "cardio_block",
    foreignKeys = [
        ForeignKey(
            entity = CardioSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class CardioBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val type: CardioType,
    val durationSec: Int,
    val distanceM: Float? = null,
    val avgRpe: Float? = null,
    val caloriesEstimated: Float,
    val notes: String? = null,
)

@Entity(
    tableName = "pain_log",
    foreignKeys = [
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["contextExerciseId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["date", "area"]),
        Index("contextExerciseId"),
    ],
)
data class PainLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val area: PainArea,
    val intensity: Int,
    val contextExerciseId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant,
)
