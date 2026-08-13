package com.kps.trackmyweight.data.repository

import com.kps.trackmyweight.data.db.dao.AnalyticsMetaDao
import com.kps.trackmyweight.data.db.dao.BodyDao
import com.kps.trackmyweight.data.db.dao.HabitDao
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.dao.WorkoutDao
import com.kps.trackmyweight.data.db.entity.CorrelationInsightEntity
import com.kps.trackmyweight.data.db.entity.WeeklyReviewEntity
import com.kps.trackmyweight.data.db.enums.CorrelationPeriod
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.domain.calc.AdherenceInputs
import com.kps.trackmyweight.domain.calc.AdherencePct
import com.kps.trackmyweight.domain.calc.CoachAdvice
import com.kps.trackmyweight.domain.calc.CoachAdvisor
import com.kps.trackmyweight.domain.calc.CorrelationInsight
import com.kps.trackmyweight.domain.calc.CorrelationInsights
import com.kps.trackmyweight.domain.calc.DailyMetric
import com.kps.trackmyweight.domain.calc.DatedValue
import com.kps.trackmyweight.domain.calc.NonLinearProjection
import com.kps.trackmyweight.domain.calc.ProjectionResult
import com.kps.trackmyweight.domain.calc.StagnationDetector
import com.kps.trackmyweight.domain.calc.WeeklyReviewGenerator
import com.kps.trackmyweight.domain.calc.WeeklySummary
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class WeeklyReport(
    val summary: WeeklySummary,
    val advices: List<CoachAdvice>,
    val projection: ProjectionResult?,
)

@Singleton
class AnalyticsRepository @Inject constructor(
    private val bodyDao: BodyDao,
    private val workoutDao: WorkoutDao,
    private val nutritionDao: NutritionDao,
    private val habitDao: HabitDao,
    private val analyticsDao: AnalyticsMetaDao,
) {

    /**
     * Génère le rapport pour la semaine passée (7 derniers jours par rapport à `today`).
     */
    suspend fun generateWeekly(
        today: LocalDate,
        goalPhase: GoalPhase,
        goalTargetKg: Float,
        goalTargetDate: LocalDate,
        proteinTargetG: Int,
        kcalTargetG: Int,
        weeksInPhase: Int,
    ): WeeklyReport {
        val weekStart = LocalDate.fromEpochDays(today.toEpochDays() - 6)
        val weightsRecent = bodyDao.observeWeightsInRange(weekStart, today).first().map {
            DatedValue(it.date, it.weightKg)
        }
        val weightDelta = if (weightsRecent.size >= 2) weightsRecent.last().value - weightsRecent.first().value else 0f

        val sessions = workoutDao.observeRecentSessions(50).first()
            .filter { it.date >= weekStart && it.date <= today }
        val cardio = workoutDao.getCardioInRange(weekStart, today)

        // Sommeil / readiness moyens
        val sleepEntries = habitDao.getSleepInRange(weekStart, today)
        val avgSleepMin = if (sleepEntries.isNotEmpty()) sleepEntries.sumOf { it.durationMin }.toFloat() / sleepEntries.size else 0f

        val dailyLogs = habitDao.observeDailyLogRange(weekStart, today).first()
        val readinessScores = dailyLogs.mapNotNull { it.readinessScore }
        val avgReadiness = if (readinessScores.isNotEmpty()) readinessScores.average().toFloat() else 0f

        // Steps
        val stepsEntries = habitDao.getStepsInRange(weekStart, today)
        val totalSteps = stepsEntries.sumOf { it.adjustedCount }

        // Nutrition (moyennes)
        val (avgProtein, avgKcal, daysProteinHit) = averagedNutrition(weekStart, today, proteinTargetG)

        val totalVolume = sessions.sumOf { it.totalVolumeKg.toDouble() }.toFloat()

        // Adhérence
        val daysInWindow = weekStart.daysUntil(today) + 1
        val habitsPossible = habitDao.observeActiveHabits().first().size * daysInWindow
        val habitsDone = dailyLogs.sumOf { logDate ->
            habitDao.observeCompletionsForDate(logDate.date).first().count { it.isDone }
        }

        val adherence = AdherencePct.compute(AdherenceInputs(
            workoutsDone = sessions.size, workoutsTarget = 5,   // TODO: derive from user's plan
            cardioDone = cardio.size, cardioTarget = 2,
            weighInsCount = weightsRecent.size, daysInWindow = daysInWindow,
            habitsDone = habitsDone, habitsPossible = habitsPossible.coerceAtLeast(1),
            daysWithGoodSleep = sleepEntries.count { it.durationMin >= 420 },
            daysWithProteinHit = daysProteinHit,
        ))

        val summary = WeeklyReviewGenerator.generate(
            weekStart = weekStart,
            adherencePct = adherence,
            weightDeltaKg = weightDelta,
            sessionsCount = sessions.size,
            cardioCount = cardio.size,
            avgProteinG = avgProtein,
            avgKcal = avgKcal,
            avgSleepMin = avgSleepMin,
            avgReadiness = avgReadiness,
            totalSteps = totalSteps,
            totalVolumeKg = totalVolume,
            proteinTarget = proteinTargetG,
            kcalTarget = kcalTargetG,
        )

        val stagnation = StagnationDetector.detect(weightsRecent, windowDays = 14, thresholdAbs = 0.3f, today = today)
        val projection = if (weightsRecent.size >= 2) {
            NonLinearProjection.project(weightsRecent, goalTargetKg, goalTargetDate, today)
        } else null

        val weeklyRate = if (weightsRecent.size >= 2) {
            val days = weightsRecent.first().date.daysUntil(weightsRecent.last().date).coerceAtLeast(1)
            weightDelta / days * 7f
        } else 0f

        val advices = CoachAdvisor.advise(
            phase = goalPhase,
            weeklyRateKg = weeklyRate,
            weeksInCurrentPhase = weeksInPhase,
            avgReadiness = avgReadiness,
            avgSleepMin = avgSleepMin,
            avgProteinG = avgProtein,
            proteinTargetG = proteinTargetG,
            volumeVerdictsOverMrv = emptyList(), // requiert agrégation par muscle, ajout futur
            stagnationDays = if (stagnation.isStagnating) 14 else 0,
            goalTargetDate = goalTargetDate,
            today = today,
        )

        // Persiste
        analyticsDao.upsertWeeklyReview(
            WeeklyReviewEntity(
                weekStart = weekStart,
                adherencePct = adherence,
                weightDeltaKg = weightDelta,
                sessionsCount = sessions.size,
                avgProteinG = avgProtein,
                avgKcal = avgKcal,
                avgSleepMin = avgSleepMin,
                avgReadiness = avgReadiness,
                totalStepsK = totalSteps / 1000f,
                totalVolumeKg = totalVolume,
                narrativeText = summary.narrative,
                generatedAt = Clock.System.now(),
            )
        )

        return WeeklyReport(summary = summary, advices = advices, projection = projection)
    }

    fun observeRecentReviews(limit: Int = 12) = analyticsDao.observeRecentReviews(limit)

    // ─────── Corrélations habitudes / résultats ───────

    fun observeTopCorrelations(limit: Int = 10) = analyticsDao.observeTopCorrelations(limit)

    /**
     * Recalcule les corrélations entre habitudes et résultats sur la période.
     *
     * Les insights sont entièrement réécrits plutôt que fusionnés : une
     * corrélation est un instantané d'une fenêtre glissante, garder les
     * anciennes ferait cohabiter des affirmations contradictoires.
     *
     * Ne stocke que ce qui est interprétable — voir
     * [CorrelationInsights.MIN_SAMPLE_SIZE].
     */
    suspend fun refreshCorrelations(
        period: CorrelationPeriod = CorrelationPeriod.LAST_90D,
    ): List<CorrelationInsight> {
        val today = todayLocal()
        val days = when (period) {
            CorrelationPeriod.LAST_30D -> 30
            CorrelationPeriod.LAST_90D -> 90
            CorrelationPeriod.ALL -> 3650
        }
        val from = LocalDate.fromEpochDays(today.toEpochDays() - days)

        val insights = CorrelationInsights.compute(collectDailySeries(from, today))

        val now = Clock.System.now()
        analyticsDao.clearCorrelations()
        if (insights.isNotEmpty()) {
            analyticsDao.insertCorrelations(
                insights.map { insight ->
                    CorrelationInsightEntity(
                        metricX = insight.pair.x.key,
                        metricY = insight.pair.y.key,
                        pearsonR = insight.result.r,
                        sampleSize = insight.result.sampleSize,
                        period = period,
                        narrativeText = insight.narrative,
                        computedAt = now,
                    )
                }
            )
        }
        return insights
    }

    /**
     * Rassemble les séries quotidiennes comparables.
     *
     * Chaque métrique est indexée par date : c'est l'intersection des dates qui
     * définit l'échantillon d'une paire, donc une journée sans pesée ne pénalise
     * pas les corrélations qui ne portent pas sur le poids.
     */
    private suspend fun collectDailySeries(
        from: LocalDate,
        to: LocalDate,
    ): Map<DailyMetric, Map<LocalDate, Float>> {
        val logs = habitDao.observeDailyLogRange(from, to).first()
        val sleep = habitDao.getSleepInRange(from, to)
        val steps = habitDao.getStepsInRange(from, to)
        val weights = bodyDao.observeWeightsInRange(from, to).first()
        val sessions = workoutDao.observeFinishedSessions(500).first()
            .filter { it.date >= from && it.date <= to }

        return buildMap {
            put(
                DailyMetric.SLEEP_HOURS,
                sleep.associate { it.date to it.durationMin / 60f },
            )
            put(
                DailyMetric.STEPS,
                steps.associate { it.date to it.adjustedCount.toFloat() },
            )
            put(
                DailyMetric.READINESS,
                logs.mapNotNull { log -> log.readinessScore?.let { log.date to it } }.toMap(),
            )
            put(
                DailyMetric.RESTING_HR,
                logs.mapNotNull { log -> log.restingHrBpm?.let { log.date to it.toFloat() } }.toMap(),
            )
            put(
                DailyMetric.BODY_WEIGHT,
                weights.associate { it.date to it.weightKg },
            )
            // Plusieurs séances le même jour se cumulent : c'est la charge du
            // jour qui est comparable, pas celle d'une séance isolée.
            put(
                DailyMetric.SESSION_VOLUME,
                sessions.groupBy { it.date }
                    .mapValues { (_, daily) -> daily.sumOf { it.totalVolumeKg.toDouble() }.toFloat() },
            )
        }
    }


    private suspend fun averagedNutrition(
        weekStart: LocalDate,
        today: LocalDate,
        proteinTargetG: Int,
    ): Triple<Float, Float, Int> {
        val dates = generateSequence(weekStart) { d ->
            val next = LocalDate.fromEpochDays(d.toEpochDays() + 1)
            if (next <= today) next else null
        }.toList()
        val perDay = dates.map { d -> nutritionDao.observeDailyMacros(d).first() }
        val avgProtein = if (perDay.isNotEmpty()) perDay.sumOf { it.protein.toDouble() }.toFloat() / perDay.size else 0f
        val avgKcal = if (perDay.isNotEmpty()) perDay.sumOf { it.kcal.toDouble() }.toFloat() / perDay.size else 0f
        val daysProteinHit = perDay.count { it.protein >= proteinTargetG * 0.9f }
        return Triple(avgProtein, avgKcal, daysProteinHit)
    }

    fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
