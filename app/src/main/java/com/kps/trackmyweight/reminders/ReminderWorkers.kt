package com.kps.trackmyweight.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kps.trackmyweight.MainActivity
import com.kps.trackmyweight.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Rappel matinal pour peser (planifié quotidiennement, fenêtre 6h-9h idéalement).
 */
@HiltWorker
class MorningWeighInReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showNotification(applicationContext, NOTIF_ID, "Pesée du jour", "Une petite pesée pour rester dans les rails.")
        return Result.success()
    }

    companion object { const val NOTIF_ID = 1001 }
}

/**
 * Rappel mensuel pour prendre les mensurations et les 3 photos.
 */
@HiltWorker
class MonthlyMeasurementReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        showNotification(
            applicationContext,
            NOTIF_ID,
            "Mensurations et photos",
            "C'est le début du mois — prends tes mensurations et tes 3 photos.",
        )
        return Result.success()
    }

    companion object { const val NOTIF_ID = 1002 }
}

@SuppressLint("MissingPermission")
private fun showNotification(context: Context, id: Int, title: String, body: String) {
    NotificationChannels.ensureCreated(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pi = PendingIntent.getActivity(
        context, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notif = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    NotificationManagerCompat.from(context).notify(id, notif)
}
