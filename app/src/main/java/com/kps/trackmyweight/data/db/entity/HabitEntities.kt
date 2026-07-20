package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.HeartRateContext
import com.kps.trackmyweight.data.db.enums.HeartRateSource
import com.kps.trackmyweight.data.db.enums.SleepSource
import com.kps.trackmyweight.data.db.enums.StepsSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "daily_log")
data class DailyLogEntity(
    @PrimaryKey val date: LocalDate,
    val readinessSleep: Int? = null,
    val readinessEnergy: Int? = null,
    val readinessSoreness: Int? = null,
    val readinessMood: Int? = null,
    val readinessScore: Float? = null,
    val restingHrBpm: Int? = null,
    val restingHrSource: HeartRateSource? = null,
    val freeNote: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "habit_definition",
    indices = [Index(value = ["key"], unique = true), Index("isActive")],
)
data class HabitDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val displayName: String,
    val iconKey: String? = null,
    val targetPerWeek: Int? = null,
    /** Cible quotidienne mesurable (ex : 2.0 pour "2L d'eau", 10000 pour "10k pas"). */
    val dailyTarget: Float? = null,
    /** Unité de la cible quotidienne (ex : "L", "pas", "h"). */
    val unit: String? = null,
    val isActive: Boolean = true,
    val orderIndex: Int = 0,
)

@Entity(
    tableName = "habit_completion",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(entity = HabitDefinitionEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("date")],
)
data class HabitCompletionEntity(
    val habitId: Long,
    val date: LocalDate,
    val isDone: Boolean,
    val valueNumeric: Float? = null,
)

@Entity(
    tableName = "sleep_entry",
    indices = [Index(value = ["date"], unique = true)],
)
data class SleepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Jour du réveil. */
    val date: LocalDate,
    val bedtime: Instant,
    val wakeTime: Instant,
    val durationMin: Int,
    val qualityRating: Int? = null,
    val deepSleepMin: Int? = null,
    val remSleepMin: Int? = null,
    val lightSleepMin: Int? = null,
    val awakeningsCount: Int? = null,
    val source: SleepSource = SleepSource.MANUAL,
    val createdAt: Instant,
)

@Entity(
    tableName = "steps_entry",
    indices = [Index(value = ["date"], unique = true)],
)
data class StepsEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val count: Int,
    val source: StepsSource = StepsSource.HEALTH_CONNECT,
    val correctionFactor: Float = 1f,
    val adjustedCount: Int,
    val updatedAt: Instant,
)

@Entity(
    tableName = "heart_rate_sample",
    indices = [Index("timestamp"), Index("context")],
)
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val bpm: Int,
    val context: HeartRateContext,
    val source: HeartRateSource,
)
