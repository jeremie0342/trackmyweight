package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.entity.ProgressPhotoEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.db.enums.PhotoAngle
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface BodyDao {

    // ── Weight ────────────────────────────────────────────
    @Upsert
    suspend fun upsertWeight(entry: WeightEntryEntity): Long

    @Query("SELECT * FROM weight_entry WHERE deletedAt IS NULL ORDER BY date DESC LIMIT :limit")
    fun observeRecentWeights(limit: Int = 90): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entry WHERE date = :date AND deletedAt IS NULL LIMIT 1")
    suspend fun getWeightOnDate(date: LocalDate): WeightEntryEntity?

    @Query("SELECT * FROM weight_entry WHERE deletedAt IS NULL AND date >= :from AND date <= :to ORDER BY date ASC")
    fun observeWeightsInRange(from: LocalDate, to: LocalDate): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entry WHERE deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    fun observeLastWeight(): Flow<WeightEntryEntity?>

    @Query("UPDATE weight_entry SET deletedAt = :now WHERE id = :id")
    suspend fun softDeleteWeight(id: Long, now: kotlinx.datetime.Instant)

    // ── Measurements ──────────────────────────────────────
    @Upsert
    suspend fun upsertMeasurement(session: BodyMeasurementSessionEntity): Long

    @Query("SELECT * FROM body_measurement_session ORDER BY date DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementSessionEntity>>

    @Query("SELECT * FROM body_measurement_session WHERE date = :date LIMIT 1")
    suspend fun getMeasurementOnDate(date: LocalDate): BodyMeasurementSessionEntity?

    @Query("SELECT * FROM body_measurement_session ORDER BY date DESC LIMIT 1")
    fun observeLastMeasurement(): Flow<BodyMeasurementSessionEntity?>

    // ── Photos ────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProgressPhotoEntity): Long

    @Query("SELECT * FROM progress_photo ORDER BY date DESC, angle ASC")
    fun observePhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photo WHERE date = :date ORDER BY angle ASC")
    suspend fun getPhotosOnDate(date: LocalDate): List<ProgressPhotoEntity>

    @Query("SELECT * FROM progress_photo WHERE angle = :angle ORDER BY date DESC LIMIT 1")
    suspend fun getLastPhotoForAngle(angle: PhotoAngle): ProgressPhotoEntity?

    @Query("DELETE FROM progress_photo WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    // ── Body composition ──────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyComposition(snapshot: BodyCompositionSnapshotEntity): Long

    @Query("SELECT * FROM body_composition_snapshot ORDER BY date DESC")
    fun observeBodyCompositionHistory(): Flow<List<BodyCompositionSnapshotEntity>>

    @Query("SELECT * FROM body_composition_snapshot ORDER BY date DESC LIMIT 1")
    fun observeLastBodyComposition(): Flow<BodyCompositionSnapshotEntity?>
}
