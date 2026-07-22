package com.kps.trackmyweight.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.backup.BackupPreferences
import com.kps.trackmyweight.data.backup.BackupService
import com.kps.trackmyweight.data.healthconnect.HealthConnectManager
import com.kps.trackmyweight.reminders.ReminderScheduler
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SecondaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isBusy: Boolean = false,
    val lastMessage: String? = null,
    val healthConnectGranted: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val autoBackupEnabled: Boolean = false,
    val lastBackupAtMs: Long = 0L,
    val lastBackupSizeBytes: Long = 0L,
    val lastBackupError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupService: BackupService,
    private val backupPrefs: BackupPreferences,
    private val hcManager: HealthConnectManager,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    fun refreshBackupState() {
        _state.value = _state.value.copy(
            autoBackupFolderUri = backupPrefs.folderUri,
            autoBackupEnabled = backupPrefs.autoBackupEnabled,
            lastBackupAtMs = backupPrefs.lastBackupAtMs,
            lastBackupSizeBytes = backupPrefs.lastBackupSizeBytes,
            lastBackupError = backupPrefs.lastBackupError,
        )
    }

    fun setBackupFolder(uri: String?) {
        backupPrefs.folderUri = uri
        refreshBackupState()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        backupPrefs.autoBackupEnabled = enabled
        if (enabled) reminderScheduler.scheduleAutoBackup()
        else reminderScheduler.cancelAutoBackup()
        refreshBackupState()
    }

    fun runBackupNow() {
        reminderScheduler.runAutoBackupNow()
        _state.value = _state.value.copy(lastMessage = "Backup lancé, il apparaîtra dans le dossier choisi dans quelques secondes.")
    }

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { refreshBackupState() }

    val healthConnectPermissions: Set<String> = hcManager.readPermissions
    val healthConnectAvailable: Boolean get() = hcManager.isAvailable

    suspend fun refreshHealthConnectStatus() {
        val granted = hcManager.hasAllPermissions()
        _state.value = _state.value.copy(healthConnectGranted = granted)
    }

    fun runHealthConnectSyncNow() {
        reminderScheduler.runHealthConnectSyncNow()
        _state.value = _state.value.copy(lastMessage = "Sync Health Connect lancée en tâche de fond.")
    }

    suspend fun buildExportJson(): String = backupService.exportJson()

    suspend fun writeExportZip(output: java.io.OutputStream) = backupService.exportZip(output)

    fun importJson(payload: String) {
        _state.value = _state.value.copy(isBusy = true, lastMessage = null)
        viewModelScope.launch {
            runCatching { backupService.importJson(payload) }
                .onSuccess { summary ->
                    _state.value = SettingsUiState(
                        isBusy = false,
                        lastMessage = "Restauration réussie : ${summary.entitiesRestored} entités.",
                    )
                }
                .onFailure {
                    _state.value = SettingsUiState(isBusy = false, lastMessage = "Erreur : ${it.message}")
                }
        }
    }

    fun importZip(input: java.io.InputStream) {
        _state.value = _state.value.copy(isBusy = true, lastMessage = null)
        viewModelScope.launch {
            runCatching { backupService.importZip(input) }
                .onSuccess { summary ->
                    _state.value = SettingsUiState(
                        isBusy = false,
                        lastMessage = "Restauration réussie : ${summary.entitiesRestored} entités (photos incluses).",
                    )
                }
                .onFailure {
                    _state.value = SettingsUiState(isBusy = false, lastMessage = "Erreur import : ${it.message}")
                }
        }
    }

    fun setMessage(msg: String?) { _state.value = _state.value.copy(lastMessage = msg) }
}

@Composable
fun SettingsScreen(
    onOpenGyms: () -> Unit = {},
    onOpenGoal: () -> Unit = {},
    onOpenHabits: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hcPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        scope.launch { vm.refreshHealthConnectStatus() }
    }
    // Fallback runtime-permissions launcher (Android 14+ traite les android.permission.health.*
    // comme des permissions runtime standard, mais le contract PermissionController est parfois cassé
    // sur les alphas). On lance directement RequestMultiplePermissions.
    val hcRuntimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        scope.launch { vm.refreshHealthConnectStatus() }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refreshHealthConnectStatus() }

    val openTree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            vm.setBackupFolder(it.toString())
            vm.setMessage("Dossier de backup configuré.")
        }
    }

    val createZipDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(it)?.use { os -> vm.writeExportZip(os) }
                    vm.setMessage("Backup complet enregistré (photos incluses).")
                }.onFailure { e -> vm.setMessage("Échec export : ${e.message}") }
            }
        }
    }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val payload = vm.buildExportJson()
                    context.contentResolver.openOutputStream(it)?.use { os -> os.write(payload.toByteArray()) }
                    vm.setMessage("Export JSON enregistré (sans photos).")
                }.onFailure { e -> vm.setMessage("Échec export : ${e.message}") }
            }
        }
    }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val mime = context.contentResolver.getType(it)
                    val name = context.contentResolver.query(it, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                    } ?: ""
                    if (name.endsWith(".zip", ignoreCase = true) || mime == "application/zip") {
                        context.contentResolver.openInputStream(it)?.use { input -> vm.importZip(input) }
                    } else {
                        val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                        if (text != null) vm.importJson(text)
                    }
                }.onFailure { e -> vm.setMessage("Échec import : ${e.message}") }
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Paramètres", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGoal() },
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mon objectif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Vois ton profil, ton objectif, ta phase (CUT / RECOMP / MAINTENANCE / BULK). Change de phase pour recalculer instantanément tes cibles caloriques.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenHabits() },
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mes habitudes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Active / désactive / ajoute des habitudes. Règle les cibles quotidiennes d'eau (L), de pas et d'autres.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGyms() },
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mes salles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "Gère tes salles de sport et leur équipement. La salle active filtre les exercices en séance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!vm.healthConnectAvailable) {
                        Text(
                            "Health Connect n'est pas disponible sur ce téléphone. Installe l'app Health Connect depuis le Play Store si tu veux importer poids/pas/sommeil automatiquement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (!state.healthConnectGranted) {
                        Text(
                            "Autorise l'app à lire ton poids, tes pas et ton sommeil depuis Health Connect pour importer automatiquement les données de ta balance/montre connectée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryButton(
                            text = "Autoriser Health Connect",
                            onClick = {
                                // Sur Android 14+, ces permissions sont runtime → on tente d'abord
                                // le contract Health Connect (bon pour Android <14 + les futures alphas).
                                // Si l'utilisateur ne voit rien, il utilisera le fallback.
                                hcPermissionsLauncher.launch(vm.healthConnectPermissions)
                            },
                        )
                        SecondaryButton(
                            text = "Demander via permissions runtime",
                            onClick = { hcRuntimeLauncher.launch(vm.healthConnectPermissions.toTypedArray()) },
                        )
                        SecondaryButton(
                            text = "Ouvrir Health Connect",
                            onClick = {
                                // Actions successives : réglages HC, puis fiche appli en dernier recours.
                                val actions = listOf(
                                    "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS",
                                    "androidx.health.ACTION_HEALTH_HOME_SETTINGS",
                                )
                                var opened = false
                                for (action in actions) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                        opened = true
                                    }
                                    if (opened) break
                                }
                                if (!opened) {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_MAIN)
                                                .setPackage("com.google.android.healthconnect.controller")
                                                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    }
                                }
                            },
                        )
                    } else {
                        Text(
                            "Permissions accordées. Sync automatique toutes les 12h. Tu peux forcer une sync immédiate ci-dessous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SecondaryButton(text = "Sync maintenant", onClick = vm::runHealthConnectSyncNow)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sauvegarde & restauration", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Exporte toutes tes données (profil, objectif, poids, mensurations, repas, séances, sommeil, habitudes) en un fichier JSON, ou restaure depuis un fichier précédemment exporté.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(
                        text = "Exporter (ZIP + photos)",
                        onClick = {
                            val name = "trackmyweight-backup-${System.currentTimeMillis()}.zip"
                            createZipDoc.launch(name)
                        },
                        enabled = !state.isBusy,
                    )
                    SecondaryButton(
                        text = "Exporter JSON seulement",
                        onClick = {
                            val name = "trackmyweight-backup-${System.currentTimeMillis()}.json"
                            createDoc.launch(name)
                        },
                        enabled = !state.isBusy,
                    )
                    SecondaryButton(
                        text = "Importer une sauvegarde",
                        onClick = { openDoc.launch(arrayOf("application/zip", "application/json", "*/*")) },
                        enabled = !state.isBusy,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup automatique", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Choisis un dossier (ex : un dossier Google Drive synchronisé, OneDrive, Downloads…) — l'app y déposera automatiquement une sauvegarde ZIP chaque jour. Les 7 dernières sont conservées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val folderLabel = state.autoBackupFolderUri?.let {
                        runCatching { android.net.Uri.decode(it).substringAfterLast("%3A").substringAfterLast("/") }.getOrNull()
                    } ?: "Aucun dossier choisi"
                    Text("Dossier : $folderLabel", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Activer", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.Switch(
                            checked = state.autoBackupEnabled && state.autoBackupFolderUri != null,
                            onCheckedChange = { checked -> vm.setAutoBackupEnabled(checked && state.autoBackupFolderUri != null) },
                            enabled = state.autoBackupFolderUri != null,
                        )
                    }
                    SecondaryButton(
                        text = if (state.autoBackupFolderUri == null) "Choisir un dossier" else "Changer de dossier",
                        onClick = { openTree.launch(null) },
                        enabled = !state.isBusy,
                    )
                    if (state.autoBackupFolderUri != null) {
                        PrimaryButton(
                            text = "Sauvegarder maintenant",
                            onClick = vm::runBackupNow,
                            enabled = !state.isBusy,
                        )
                    }
                    if (state.lastBackupAtMs > 0L) {
                        val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
                            .format(java.util.Date(state.lastBackupAtMs))
                        val sizeKb = state.lastBackupSizeBytes / 1024
                        Text(
                            "Dernier backup : $date · ${sizeKb} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.lastBackupError?.let {
                        Text("Dernière erreur : $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            state.lastMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it, modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("À propos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TrackMyWeight", style = MaterialTheme.typography.titleMedium)
                    Text("Suivi de transformation physique — assistant complet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}
