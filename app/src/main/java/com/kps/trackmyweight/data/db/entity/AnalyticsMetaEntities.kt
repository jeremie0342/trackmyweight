package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.BackupDestination
import com.kps.trackmyweight.data.db.enums.CorrelationPeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "weekly_review",
    indices = [Index(value = ["weekStart"], unique = true)],
)
data class WeeklyReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: LocalDate,
    val adherencePct: Float,
    val weightDeltaKg: Float,
    val sessionsCount: Int,
    val avgProteinG: Float,
    val avgKcal: Float,
    val avgSleepMin: Float,
    val avgReadiness: Float,
    val totalStepsK: Float,
    val totalVolumeKg: Float,
    val narrativeText: String,
    val userAnswersJson: String? = null,
    val generatedAt: Instant,
)

@Entity(
    tableName = "correlation_insight",
    indices = [Index(value = ["metricX", "metricY", "period"])],
)
data class CorrelationInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricX: String,
    val metricY: String,
    val pearsonR: Float,
    val sampleSize: Int,
    val period: CorrelationPeriod,
    val narrativeText: String,
    val computedAt: Instant,
)

@Entity(tableName = "projection_snapshot")
data class ProjectionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val computedAt: Instant,
    val targetDate: LocalDate,
    val projectedWeightAtTarget: Float,
    val trendKgPerWeek: Float,
    val confidence: Float,
    val notes: String? = null,
)

@Entity(tableName = "health_connect_sync_state")
data class HealthConnectSyncStateEntity(
    @PrimaryKey val dataType: String,
    val lastSuccessfulSyncAt: Instant,
    val lastRecordExternalId: String? = null,
    val errorCount: Int = 0,
)

@Entity(tableName = "backup_record")
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Instant,
    val sizeBytes: Long,
    val destination: BackupDestination,
    val filePath: String,
    val isEncrypted: Boolean,
    val schemaVersion: Int,
)

@Entity(
    tableName = "app_event",
    indices = [Index("timestamp"), Index("type")],
)
data class AppEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val type: String,
    val payloadJson: String? = null,
)
