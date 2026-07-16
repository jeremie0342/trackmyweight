package com.kps.trackmyweight.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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

    fun cancelAll() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_MORNING_WEIGH_IN)
        wm.cancelUniqueWork(WORK_MONTHLY_MEASUREMENT)
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
    }
}
