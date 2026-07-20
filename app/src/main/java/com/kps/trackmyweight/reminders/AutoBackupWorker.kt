package com.kps.trackmyweight.reminders

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kps.trackmyweight.data.backup.BackupPreferences
import com.kps.trackmyweight.data.backup.BackupService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup automatique quotidien : écrit un zip dans le dossier choisi par l'utilisateur
 * via SAF (Drive, OneDrive, Downloads, etc.). Ne fait rien si aucun dossier n'a été choisi
 * ou si le toggle est désactivé.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val backupService: BackupService,
    private val prefs: BackupPreferences,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!prefs.autoBackupEnabled) return Result.success()
        val folderUriStr = prefs.folderUri ?: return Result.success()
        val folderUri = runCatching { Uri.parse(folderUriStr) }.getOrNull() ?: return Result.failure()

        return try {
            val folder = DocumentFile.fromTreeUri(ctx, folderUri) ?: error("Dossier introuvable")
            if (!folder.canWrite()) error("Pas de droit d'écriture sur le dossier")
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val fileName = "trackmyweight-$stamp.zip"
            val doc = folder.createFile("application/zip", fileName) ?: error("Création du fichier échouée")
            ctx.contentResolver.openOutputStream(doc.uri)?.use { os ->
                backupService.exportZip(os)
            } ?: error("Ouverture du flux échouée")
            prefs.lastBackupAtMs = System.currentTimeMillis()
            prefs.lastBackupSizeBytes = doc.length()
            prefs.lastBackupError = null
            Log.i("AutoBackup", "Backup OK : ${doc.uri} taille=${doc.length() / 1024} KB")

            // Rotation : garde uniquement les 7 derniers backups .zip du dossier
            folder.listFiles()
                .filter { it.name?.startsWith("trackmyweight-") == true && it.name?.endsWith(".zip") == true }
                .sortedByDescending { it.lastModified() }
                .drop(7)
                .forEach { runCatching { it.delete() } }

            Result.success()
        } catch (e: Exception) {
            prefs.lastBackupError = e.message
            Log.e("AutoBackup", "Backup échoué", e)
            Result.retry()
        }
    }
}
