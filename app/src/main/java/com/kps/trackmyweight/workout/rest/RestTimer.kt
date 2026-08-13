package com.kps.trackmyweight.workout.rest

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.kps.trackmyweight.MainActivity
import com.kps.trackmyweight.R
import com.kps.trackmyweight.reminders.NotificationChannels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chrono de repos entre les séries.
 *
 * Deux principes :
 *
 *  1. **Le chrono est une échéance, pas un décompte.** On mémorise l'instant de
 *     fin ; l'affichage se déduit de `fin - maintenant`. La version précédente
 *     décrémentait un compteur dans une coroutine du ViewModel : elle dérivait,
 *     et surtout elle mourait avec le ViewModel — sortir de l'écran perdait le
 *     chrono.
 *
 *  2. **L'alerte ne dépend pas de l'app.** Une alarme système déclenche la
 *     notification à l'heure dite, que l'app soit au premier plan, en arrière-plan
 *     ou tuée. C'est indispensable en salle : on repose le téléphone entre deux
 *     séries, l'écran s'éteint.
 *
 * Pas de service de premier plan : pour une simple échéance ponctuelle, une alarme
 * exacte est plus fiable et bien moins coûteuse.
 */
@Singleton
class RestTimerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Programme l'alerte de fin de repos. Remplace toute alerte déjà programmée. */
    fun schedule(endsAt: Instant) {
        NotificationChannels.ensureCreated(context)
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val at = endsAt.toEpochMilliseconds()
        val pi = pendingIntent(context)

        // Sur Android 12+, les alarmes exactes peuvent être refusées à l'app.
        // On dégrade alors vers une fenêtre approximative plutôt que de ne rien
        // programmer : quelques secondes d'imprécision valent mieux qu'aucune alerte.
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        runCatching {
            if (canBeExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    at,
                    APPROXIMATE_WINDOW_MS,
                    pi,
                )
            }
        }
    }

    /** Annule l'alerte et retire la notification éventuellement déjà affichée. */
    fun cancel() {
        context.getSystemService<AlarmManager>()?.cancel(pendingIntent(context))
        NotificationManagerCompat.from(context).cancel(RestAlarmReceiver.NOTIF_ID)
    }

    private companion object {
        const val APPROXIMATE_WINDOW_MS = 10_000L

        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            RestAlarmReceiver.REQUEST_CODE,
            Intent(context, RestAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Reçoit l'alarme de fin de repos et affiche la notification. */
class RestAlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.ensureCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REST_TIMER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_rest_done_title))
            .setContentText(context.getString(R.string.notif_rest_done_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
    }

    companion object {
        const val NOTIF_ID = 2001
        const val REQUEST_CODE = 2001
    }
}
