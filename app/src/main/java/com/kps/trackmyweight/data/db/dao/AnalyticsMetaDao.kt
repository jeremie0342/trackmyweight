package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert  // conservé pour syncState (PK naturelle)
import com.kps.trackmyweight.data.db.entity.AppEventEntity
import com.kps.trackmyweight.data.db.entity.BackupRecordEntity
import com.kps.trackmyweight.data.db.entity.CorrelationInsightEntity
import com.kps.trackmyweight.data.db.entity.HealthConnectSyncStateEntity
import com.kps.trackmyweight.data.db.entity.ProjectionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.WeeklyReviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface AnalyticsMetaDao {

    // ── Weekly reviews ────────────────────────────────────
    // @Insert(REPLACE) car unique(weekStart).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyReview(r: WeeklyReviewEntity): Long

    @Query("SELECT * FROM weekly_review WHERE weekStart = :weekStart LIMIT 1")
    suspend fun getReviewForWeek(weekStart: LocalDate): WeeklyReviewEntity?

    @Query("SELECT * FROM weekly_review ORDER BY weekStart DESC LIMIT :limit")
    fun observeRecentReviews(limit: Int = 12): Flow<List<WeeklyReviewEntity>>

    // ── Correlations ──────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrelations(items: List<CorrelationInsightEntity>)

    @Query("DELETE FROM correlation_insight")
    suspend fun clearCorrelations()

    @Query("SELECT * FROM correlation_insight ORDER BY ABS(pearsonR) DESC LIMIT :limit")
    fun observeTopCorrelations(limit: Int = 10): Flow<List<CorrelationInsightEntity>>

    // ── Projections ───────────────────────────────────────
    @Insert
    suspend fun insertProjection(p: ProjectionSnapshotEntity): Long

    @Query("SELECT * FROM projection_snapshot ORDER BY computedAt DESC LIMIT 1")
    fun observeLatestProjection(): Flow<ProjectionSnapshotEntity?>

    // ── Health Connect sync ───────────────────────────────
    @Upsert
    suspend fun upsertSyncState(state: HealthConnectSyncStateEntity)

    @Query("SELECT * FROM health_connect_sync_state WHERE dataType = :type LIMIT 1")
    suspend fun getSyncState(type: String): HealthConnectSyncStateEntity?

    // ── Backup ────────────────────────────────────────────
    @Insert
    suspend fun insertBackup(b: BackupRecordEntity): Long

    @Query("SELECT * FROM backup_record ORDER BY createdAt DESC LIMIT :limit")
    fun observeBackups(limit: Int = 20): Flow<List<BackupRecordEntity>>

    // ── Events (log léger) ────────────────────────────────
    @Insert
    suspend fun logEvent(event: AppEventEntity): Long

    @Query("DELETE FROM app_event WHERE timestamp < :cutoff")
    suspend fun purgeEventsBefore(cutoff: kotlinx.datetime.Instant)
}
