package com.kps.trackmyweight.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val REMINDERS_ID = "reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService<NotificationManager>() ?: return
        if (mgr.getNotificationChannel(REMINDERS_ID) != null) return
        val name = context.getString(com.kps.trackmyweight.R.string.notif_channel_reminders)
        val desc = context.getString(com.kps.trackmyweight.R.string.notif_channel_reminders_desc)
        mgr.createNotificationChannel(
            NotificationChannel(REMINDERS_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = desc
                enableVibration(false)
            }
        )
    }
}
