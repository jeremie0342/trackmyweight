package com.kps.trackmyweight.data.backup

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Petit store SharedPreferences pour :
 *  - l'URI persistante du dossier de sauvegarde (choisie via SAF)
 *  - le toggle activant le backup automatique
 *  - le timestamp du dernier backup réussi (ms epoch)
 */
@Singleton
class BackupPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    var folderUri: String?
        get() = prefs.getString(KEY_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_URI, value).apply()

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ENABLED, value).apply()

    var lastBackupAtMs: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_MS, value).apply()

    var lastBackupSizeBytes: Long
        get() = prefs.getLong(KEY_LAST_SIZE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SIZE, value).apply()

    var lastBackupError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR, value).apply()

    private companion object {
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_AUTO_ENABLED = "auto_enabled"
        const val KEY_LAST_BACKUP_MS = "last_backup_ms"
        const val KEY_LAST_SIZE = "last_size"
        const val KEY_LAST_ERROR = "last_error"
    }
}
