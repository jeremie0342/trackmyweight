package com.kps.trackmyweight.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val REMINDERS_ID = "reminders"

    /**
     * Canal dédié à la fin du temps de repos.
     *
     * Séparé des rappels : en salle, la notification doit passer devant tout le
     * reste, sonner et vibrer même si l'utilisateur a mis les rappels en sourdine.
     * D'où IMPORTANCE_HIGH, la vibration et le son explicites.
     */
    const val REST_TIMER_ID = "rest_timer"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService<NotificationManager>() ?: return

        if (mgr.getNotificationChannel(REMINDERS_ID) == null) {
            val name = context.getString(com.kps.trackmyweight.R.string.notif_channel_reminders)
            val desc = context.getString(com.kps.trackmyweight.R.string.notif_channel_reminders_desc)
            mgr.createNotificationChannel(
                NotificationChannel(REMINDERS_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = desc
                    enableVibration(false)
                }
            )
        }

        if (mgr.getNotificationChannel(REST_TIMER_ID) == null) {
            val name = context.getString(com.kps.trackmyweight.R.string.notif_channel_rest)
            val desc = context.getString(com.kps.trackmyweight.R.string.notif_channel_rest_desc)
            mgr.createNotificationChannel(
                NotificationChannel(REST_TIMER_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = desc
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .build(),
                    )
                }
            )
        }
    }
}
