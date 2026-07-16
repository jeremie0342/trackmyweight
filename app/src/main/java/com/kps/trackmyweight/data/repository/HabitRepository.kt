package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.HabitDao
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.data.db.entity.HeartRateSampleEntity
import com.kps.trackmyweight.data.db.entity.SleepEntryEntity
import com.kps.trackmyweight.data.db.entity.StepsEntryEntity
import com.kps.trackmyweight.data.db.entity.WaterEntryEntity
import com.kps.trackmyweight.data.db.enums.HeartRateContext
import com.kps.trackmyweight.data.db.enums.HeartRateSource
import com.kps.trackmyweight.data.db.enums.SleepSource
import com.kps.trackmyweight.data.db.enums.StepsSource
import com.kps.trackmyweight.data.db.enums.WaterSource
import com.kps.trackmyweight.data.seed.HabitSeed
import com.kps.trackmyweight.domain.calc.ReadinessInputs
import com.kps.trackmyweight.domain.calc.ReadinessScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val nutritionDao: NutritionDao,
) {

    // ── Seed ────────────────────────────────────
    suspend fun seedIfEmpty() {
        habitDao.insertHabitDefinitions(HabitSeed.items)
    }

    fun observeHabits(): Flow<List<HabitDefinitionEntity>> = habitDao.observeActiveHabits()
    fun observeCompletionsForDate(date: LocalDate): Flow<List<HabitCompletionEntity>> = habitDao.observeCompletionsForDate(date)

    suspend fun toggleCompletion(habitId: Long, date: LocalDate, done: Boolean) {
        habitDao.upsertCompletion(HabitCompletionEntity(habitId, date, isDone = done))
    }

    suspend fun streakDays(habitId: Long, today: LocalDate): Int = habitDao.getStreakDays(habitId, today)

    // ── Daily log (readiness + note) ─────────────
    fun observeDailyLog(date: LocalDate): Flow<DailyLogEntity?> = habitDao.observeDailyLog(date)

    suspend fun saveReadiness(
        date: LocalDate,
        sleepQuality: Int?,
        energy: Int?,
        soreness: Int?,
        mood: Int?,
        note: String? = null,
    ) {
        val verdict = ReadinessScore.compute(ReadinessInputs(sleepQuality, energy, soreness, mood))
        val now = Clock.System.now()
        val existing = habitDao.observeDailyLog(date).first()
        habitDao.upsertDailyLog(
            DailyLogEntity(
                date = date,
                readinessSleep = sleepQuality,
                readinessEnergy = energy,
                readinessSoreness = soreness,
                readinessMood = mood,
                readinessScore = if (verdict.filledDimensions > 0) verdict.score else null,
                restingHrBpm = existing?.restingHrBpm,
                restingHrSource = existing?.restingHrSource,
                freeNote = note ?: existing?.freeNote,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

    suspend fun saveMorningPulse(date: LocalDate, bpm: Int, source: HeartRateSource = HeartRateSource.MANUAL) {
        val now = Clock.System.now()
        val existing = habitDao.observeDailyLog(date).first()
        habitDao.upsertDailyLog(
            (existing ?: DailyLogEntity(date = date, createdAt = now, updatedAt = now)).copy(
                restingHrBpm = bpm,
                restingHrSource = source,
                updatedAt = now,
            )
        )
        habitDao.insertHrSample(
            HeartRateSampleEntity(
                timestamp = now,
                bpm = bpm,
                context = HeartRateContext.REST_MORNING,
                source = source,
            )
        )
    }

    // ── Sleep ─────────────────────────────────────
    fun observeSleepForDate(date: LocalDate): Flow<SleepEntryEntity?> = habitDao.observeSleepForDate(date)

    suspend fun logSleep(
        date: LocalDate,
        bedtime: Instant,
        wakeTime: Instant,
        qualityRating: Int?,
        source: SleepSource = SleepSource.MANUAL,
    ): Long {
        val minutes = ((wakeTime.toEpochMilliseconds() - bedtime.toEpochMilliseconds()) / 60_000L).toInt().coerceAtLeast(0)
        return habitDao.upsertSleep(
            SleepEntryEntity(
                date = date,
                bedtime = bedtime,
                wakeTime = wakeTime,
                durationMin = minutes,
                qualityRating = qualityRating,
                source = source,
                createdAt = Clock.System.now(),
            )
        )
    }

    // ── Steps ─────────────────────────────────────
    fun observeStepsForDate(date: LocalDate): Flow<StepsEntryEntity?> = habitDao.observeStepsForDate(date)

    suspend fun logSteps(date: LocalDate, count: Int, correctionFactor: Float = 1f, source: StepsSource = StepsSource.MANUAL): Long {
        val now = Clock.System.now()
        val adjusted = (count * correctionFactor).toInt()
        return habitDao.upsertSteps(
            StepsEntryEntity(
                date = date,
                count = count,
                source = source,
                correctionFactor = correctionFactor,
                adjustedCount = adjusted,
                updatedAt = now,
            )
        )
    }

    // ── Water ─────────────────────────────────────
    fun observeWaterMlForDate(date: LocalDate): Flow<Int> = nutritionDao.observeWaterMlForDate(date)

    suspend fun logWater(date: LocalDate, ml: Int) {
        nutritionDao.insertWater(
            WaterEntryEntity(date = date, timestamp = Clock.System.now(), volumeMl = ml, source = WaterSource.MANUAL)
        )
    }
}
