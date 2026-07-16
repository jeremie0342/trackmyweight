package com.kps.trackmyweight.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.EquipmentCategory
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.UnitSystem
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Profil utilisateur unique (singleton, id = 1).
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Float,
    val preferredUnit: UnitSystem = UnitSystem.METRIC,
    val currency: String = "XOF",
    val locale: String = "fr",
    val activityLevel: ActivityLevel = ActivityLevel.VERY_ACTIVE,
    val coachModeEnabled: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "goal",
    indices = [Index(value = ["isActive"])],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetWeightKg: Float,
    val targetDate: LocalDate,
    val phase: GoalPhase,
    val isActive: Boolean,
    val startedAt: LocalDate,
    val endedAt: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "gym",
    indices = [Index(value = ["isDefault"])],
)
data class GymEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "equipment",
    indices = [Index(value = ["key"], unique = true)],
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val displayName: String,
    val category: EquipmentCategory,
)

@Entity(
    tableName = "gym_equipment",
    primaryKeys = ["gymId", "equipmentId"],
    foreignKeys = [
        ForeignKey(entity = GymEntity::class, parentColumns = ["id"], childColumns = ["gymId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EquipmentEntity::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("equipmentId")],
)
data class GymEquipmentEntity(
    val gymId: Long,
    val equipmentId: Long,
    @ColumnInfo(defaultValue = "NULL") val minWeightKg: Float? = null,
    @ColumnInfo(defaultValue = "NULL") val maxWeightKg: Float? = null,
    @ColumnInfo(defaultValue = "1") val quantity: Int = 1,
)
