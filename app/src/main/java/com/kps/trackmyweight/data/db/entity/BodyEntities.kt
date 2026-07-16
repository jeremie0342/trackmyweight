package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.BodyFatMethod
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import com.kps.trackmyweight.data.db.enums.WeightSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "weight_entry",
    indices = [Index(value = ["date"], unique = true)],
)
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Float,
    val source: WeightSource = WeightSource.MANUAL,
    val recordedAt: Instant,
    val note: String? = null,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)

/**
 * Session complète de mensurations à une date donnée. Champs nullables → saisie partielle.
 */
@Entity(
    tableName = "body_measurement_session",
    indices = [Index(value = ["date"], unique = true)],
)
data class BodyMeasurementSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val neckCm: Float? = null,
    val shoulderCm: Float? = null,
    val chestCm: Float? = null,
    val waistCm: Float? = null,
    val hipCm: Float? = null,
    val armLeftCm: Float? = null,
    val armRightCm: Float? = null,
    val forearmLeftCm: Float? = null,
    val forearmRightCm: Float? = null,
    val thighLeftCm: Float? = null,
    val thighRightCm: Float? = null,
    val calfLeftCm: Float? = null,
    val calfRightCm: Float? = null,
    val wristCm: Float? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "progress_photo",
    indices = [
        Index(value = ["date", "angle"], unique = true),
        Index(value = ["date"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProgressPhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["overlayReferencePhotoId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val angle: PhotoAngle,
    val encryptedFilePath: String,
    val thumbnailPath: String,
    val overlayReferencePhotoId: Long? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "body_composition_snapshot",
    indices = [Index(value = ["date"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = BodyMeasurementSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceMeasurementSessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class BodyCompositionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val bodyFatPct: Float,
    val method: BodyFatMethod,
    val leanMassKg: Float,
    val fatMassKg: Float,
    val sourceMeasurementSessionId: Long? = null,
    val computedAt: Instant,
)
