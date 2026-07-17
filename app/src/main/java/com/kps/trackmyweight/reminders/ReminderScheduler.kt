package com.kps.trackmyweight.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kps.trackmyweight.data.healthconnect.HealthConnectSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Planifie les rappels périodiques via WorkManager.
 * WorkManager garantit la fiabilité même en Doze mode.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Rappel quotidien de pesée à 07:30 locale. */
    fun scheduleMorningWeighIn() {
        val work = PeriodicWorkRequestBuilder<MorningWeighInReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayForHour(hour = 7, minute = 30), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_MORNING_WEIGH_IN,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }

    /** Rappel mensuel de mensurations + photos, le 1er de chaque mois à 09:00. */
    fun scheduleMonthlyMeasurement() {
        val work = PeriodicWorkRequestBuilder<MonthlyMeasurementReminderWorker>(30, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayForFirstOfMonth(hour = 9, minute = 0), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_MONTHLY_MEASUREMENT,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }

    /** Rappel du soir à 20:30 si aucune séance n'a été loguée aujourd'hui. */
    fun scheduleSessionNotLogged() {
        val work = PeriodicWorkRequestBuilder<SessionNotLoggedReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelayForHour(hour = 20, minute = 30), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_SESSION_NOT_LOGGED,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }

    /** Rappel hydratation à 14:00 et 18:00 (deux workers séparés pour deux fenêtres). */
    fun scheduleHydration() {
        listOf(14 to 0, 18 to 0).forEachIndexed { i, (h, m) ->
            val work = PeriodicWorkRequestBuilder<HydrationReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(computeInitialDelayForHour(h, m), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_HYDRATION}_$i",
                ExistingPeriodicWorkPolicy.KEEP,
                work,
            )
        }
    }

    /** Sync Health Connect toutes les 12h. */
    fun scheduleHealthConnectSync() {
        val work = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(60, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_HEALTH_CONNECT_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }

    /** Sync immédiate à la demande (bouton Settings). */
    fun runHealthConnectSyncNow() {
        val work = OneTimeWorkRequestBuilder<HealthConnectSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${WORK_HEALTH_CONNECT_SYNC}_oneshot",
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    /** Enchaîne tous les rappels standards. À appeler depuis RootViewModel après onboarding. */
    fun scheduleAll() {
        scheduleMorningWeighIn()
        scheduleMonthlyMeasurement()
        scheduleSessionNotLogged()
        scheduleHydration()
        scheduleHealthConnectSync()
    }

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_MORNING_WEIGH_IN)
        wm.cancelUniqueWork(WORK_MONTHLY_MEASUREMENT)
        wm.cancelUniqueWork(WORK_SESSION_NOT_LOGGED)
        wm.cancelUniqueWork("${WORK_HYDRATION}_0")
        wm.cancelUniqueWork("${WORK_HYDRATION}_1")
    }

    private fun computeInitialDelayForHour(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun computeInitialDelayForFirstOfMonth(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        const val WORK_MORNING_WEIGH_IN = "morning_weigh_in"
        const val WORK_MONTHLY_MEASUREMENT = "monthly_measurement"
        const val WORK_SESSION_NOT_LOGGED = "session_not_logged"
        const val WORK_HYDRATION = "hydration"
        const val WORK_HEALTH_CONNECT_SYNC = "health_connect_sync"
    }
}
