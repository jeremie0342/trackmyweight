package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.data.db.entity.HeartRateSampleEntity
import com.kps.trackmyweight.data.db.entity.SleepEntryEntity
import com.kps.trackmyweight.data.db.entity.StepsEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface HabitDao {

    // ── Daily log (readiness + note) ──────────────────────
    @Upsert
    suspend fun upsertDailyLog(log: DailyLogEntity)

    @Query("SELECT * FROM daily_log WHERE date = :date LIMIT 1")
    fun observeDailyLog(date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_log WHERE date >= :from AND date <= :to ORDER BY date")
    fun observeDailyLogRange(from: LocalDate, to: LocalDate): Flow<List<DailyLogEntity>>

    // ── Habits ────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabitDefinitions(defs: List<HabitDefinitionEntity>)

    @Query("SELECT * FROM habit_definition WHERE isActive = 1 ORDER BY orderIndex")
    fun observeActiveHabits(): Flow<List<HabitDefinitionEntity>>

    @Upsert
    suspend fun upsertCompletion(c: HabitCompletionEntity)

    @Query("SELECT * FROM habit_completion WHERE date = :date")
    fun observeCompletionsForDate(date: LocalDate): Flow<List<HabitCompletionEntity>>

    /** Streak d'une habitude en jours consécutifs jusqu'à aujourd'hui. */
    @Query("""
        WITH RECURSIVE dates(d) AS (
            SELECT :today
            UNION ALL
            SELECT date(d, '-1 day') FROM dates
            WHERE (
                SELECT isDone FROM habit_completion
                WHERE habitId = :habitId AND date = d
            ) = 1
            LIMIT 400
        )
        SELECT COUNT(*) FROM dates
        WHERE (
            SELECT isDone FROM habit_completion
            WHERE habitId = :habitId AND date = d
        ) = 1
    """)
    suspend fun getStreakDays(habitId: Long, today: LocalDate): Int

    // ── Sleep ─────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleep(entry: SleepEntryEntity): Long

    @Query("SELECT * FROM sleep_entry WHERE date = :date LIMIT 1")
    fun observeSleepForDate(date: LocalDate): Flow<SleepEntryEntity?>

    @Query("SELECT * FROM sleep_entry WHERE date >= :from AND date <= :to ORDER BY date")
    suspend fun getSleepInRange(from: LocalDate, to: LocalDate): List<SleepEntryEntity>

    // ── Steps ─────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(entry: StepsEntryEntity): Long

    @Query("SELECT * FROM steps_entry WHERE date = :date LIMIT 1")
    fun observeStepsForDate(date: LocalDate): Flow<StepsEntryEntity?>

    @Query("SELECT * FROM steps_entry WHERE date >= :from AND date <= :to ORDER BY date")
    suspend fun getStepsInRange(from: LocalDate, to: LocalDate): List<StepsEntryEntity>

    // ── Heart rate ────────────────────────────────────────
    @Insert
    suspend fun insertHrSample(sample: HeartRateSampleEntity): Long

    @Query("SELECT * FROM heart_rate_sample WHERE context = 'REST_MORNING' ORDER BY timestamp DESC LIMIT :limit")
    fun observeRestingHrHistory(limit: Int = 60): Flow<List<HeartRateSampleEntity>>
}
