package com.kps.trackmyweight.data.healthconnect

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kps.trackmyweight.data.db.enums.SleepSource
import com.kps.trackmyweight.data.db.enums.StepsSource
import com.kps.trackmyweight.data.db.enums.WeightSource
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Sync périodique (12h) Health Connect → Room. Idempotent : upsert par date pour poids/pas/sommeil.
 * Skip silencieusement si les permissions ne sont pas accordées.
 */
@HiltWorker
class HealthConnectSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val hcManager: HealthConnectManager,
    private val weightRepo: WeightRepository,
    private val habitRepo: HabitRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!hcManager.isAvailable || !hcManager.hasAllPermissions()) return Result.success()

        val now = Clock.System.now()
        val from = Instant.fromEpochMilliseconds(
            now.toJavaInstantSafe().minus(HealthConnectManager.LOOKBACK_DAYS, ChronoUnit.DAYS).toEpochMilli()
        )
        val tz = TimeZone.currentSystemDefault()

        // Poids
        hcManager.readWeights(from, now).forEach { s ->
            val date = s.timestamp.toLocalDateTime(tz).date
            weightRepo.log(date = date, weightKg = s.kg, source = WeightSource.HEALTH_CONNECT)
        }

        // Pas — Health Connect renvoie plusieurs enregistrements par jour. On agrège par date.
        val stepsByDate = hcManager.readSteps(from, now).groupBy {
            it.fromInstant.toLocalDateTime(tz).date
        }
        stepsByDate.forEach { (date, records) ->
            val total = records.sumOf { it.count }
            habitRepo.logSteps(date, total, correctionFactor = 1f, source = StepsSource.HEALTH_CONNECT)
        }

        // Sommeil — 1 entrée par nuit (date = jour du réveil).
        hcManager.readSleep(from, now).forEach { s ->
            val wakeDate = s.end.toLocalDateTime(tz).date
            habitRepo.logSleep(
                date = wakeDate,
                bedtime = s.start,
                wakeTime = s.end,
                qualityRating = null,
                source = SleepSource.HEALTH_CONNECT,
            )
        }

        return Result.success()
    }
}

private fun Instant.toJavaInstantSafe(): java.time.Instant =
    java.time.Instant.ofEpochMilli(toEpochMilliseconds())
