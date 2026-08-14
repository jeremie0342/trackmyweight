package com.kps.trackmyweight.ui.workout.session

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.domain.calc.VoiceSetParser
import com.kps.trackmyweight.domain.calc.WarmupSet
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.ExerciseThumbnail
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SelectableChip
import com.kps.trackmyweight.ui.common.descriptionFr
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.workout.exercise.CreateExerciseDialog
import com.kps.trackmyweight.ui.workout.pain.PainDialog
import com.kps.trackmyweight.ui.workout.setup.supersetLetter
import com.kps.trackmyweight.ui.theme.tabular

@Composable
fun SessionActiveScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    onLeaveRunning: () -> Unit = onFinished,
    onOpenExercise: (Long) -> Unit = {},
    vm: SessionActiveViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.isDone) { if (state.isDone) onFinished() }

    var showExitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showWarmupDialog by remember { mutableStateOf(false) }
    var showRestDialog by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<PerformedSetEntity?>(null) }
    var createFromQuery by remember { mutableStateOf<String?>(null) }
    var painForExercise by remember { mutableStateOf<ExerciseCard?>(null) }

    // Le retour système ne doit jamais faire disparaître une séance en silence.
    BackHandler(enabled = !state.isDone) { showExitDialog = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddExerciseSheet = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Ajouter") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackHeader(
                    title = "Séance en cours",
                    onBack = { showExitDialog = true },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showFinishDialog = true }) { Text("Terminer") }
            }

            SessionStatsCard(
                elapsedSec = state.elapsedSec,
                totalSets = state.totalSets,
                volumeKg = state.volumeKg,
            )

            if (state.isResting) {
                RestTimerCard(
                    remaining = state.restRemainingSec,
                    total = state.restTotalSec,
                    onAdjust = vm::adjustRest,
                    onCancel = vm::cancelRest,
                )
            } else {
                TextButton(onClick = { showRestDialog = true }) {
                    Icon(Icons.Outlined.Timer, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Démarrer un repos")
                }
            }

            WarmupSection(warmup = state.warmup, onAdd = { showWarmupDialog = true })

            if (state.exercises.isEmpty()) {
                Text(
                    "Aucun exercice dans cette séance. Ajoute-en avec le bouton +.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.exercises.forEachIndexed { index, card ->
                    ExerciseCardView(
                        card = card,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.exercises.lastIndex,
                        onMoveUp = { vm.moveExerciseUp(card.performed.id) },
                        onMoveDown = { vm.moveExerciseDown(card.performed.id) },
                        onLogSet = { w, r, rpe, type -> vm.logSet(card.performed, w, r, rpe, type) },
                        onEditSet = { editingSet = it },
                        onRemoveExercise = { vm.removeExercise(card.performed.id) },
                        onOpenDetail = { onOpenExercise(card.performed.exerciseId) },
                        onReportPain = { painForExercise = card },
                        onGenerateWarmup = { topSet -> vm.generateWarmupSets(card.performed, topSet) },
                        warmupPreview = { topSet -> vm.warmupPreview(card.performed, topSet) },
                    )
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(120.dp))
        }
    }

    if (showExitDialog) {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onKeepRunning = { showExitDialog = false; onLeaveRunning() },
            onAbandon = { showExitDialog = false; vm.abandonSession() },
            onFinish = { showExitDialog = false; showFinishDialog = true },
        )
    }

    if (showFinishDialog) {
        FinishDialog(
            isFinishing = state.isFinishing,
            error = state.errorMessage,
            onDismiss = { showFinishDialog = false; vm.clearError() },
            onFinish = { rpe, notes -> vm.finishSession(rpe, notes); showFinishDialog = false },
        )
    }

    if (showAddExerciseSheet) {
        AddExerciseDialog(
            exercises = state.pickerExercises,
            canFilterByGym = state.canFilterByGym,
            onlyMyGym = state.onlyMyGym,
            hiddenCount = state.hiddenByGymCount,
            onToggleGymFilter = vm::toggleOnlyMyGym,
            onCreateRequested = { query -> createFromQuery = query; showAddExerciseSheet = false },
            onDismiss = { showAddExerciseSheet = false },
            onPick = { id -> vm.addExercise(id); showAddExerciseSheet = false },
        )
    }

    painForExercise?.let { card ->
        PainDialog(
            contextExerciseName = card.performed.exerciseNameSnapshot,
            onDismiss = { painForExercise = null },
            onConfirm = { draft ->
                vm.logPain(card.performed.exerciseId, draft)
                painForExercise = null
            },
        )
    }

    createFromQuery?.let { initial ->
        CreateExerciseDialog(
            initialName = initial,
            availableEquipment = state.allEquipment,
            onDismiss = { createFromQuery = null },
            onCreate = { draft -> vm.createCustomExercise(draft); createFromQuery = null },
        )
    }

    if (showWarmupDialog) {
        WarmupDialog(
            onDismiss = { showWarmupDialog = false },
            onConfirm = { type, min, rpe -> vm.logWarmup(type, min, rpe); showWarmupDialog = false },
        )
    }

    if (showRestDialog) {
        ManualRestDialog(
            onDismiss = { showRestDialog = false },
            onStart = { sec -> vm.startRest(sec); showRestDialog = false },
        )
    }

    editingSet?.let { set ->
        EditSetDialog(
            set = set,
            onDismiss = { editingSet = null },
            onSave = { w, r, rpe, type ->
                vm.updateSet(set.id, w, r, rpe, type)
                editingSet = null
            },
            onDelete = { vm.deleteSet(set.id); editingSet = null },
        )
    }
}

// ─────────────────────────────── En-tête ──────────────────────────────────

@Composable
private fun SessionStatsCard(elapsedSec: Int, totalSets: Int, volumeKg: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Stat("Durée", formatDuration(elapsedSec), Modifier.weight(1f))
            Stat("Séries", totalSets.toString(), Modifier.weight(1f))
            Stat("Volume", "%.0f kg".format(volumeKg), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge.tabular(), fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDuration(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ─────────────────────────────── Repos ────────────────────────────────────

@Composable
private fun RestTimerCard(
    remaining: Int,
    total: Int,
    onAdjust: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Repos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) {
                    Text("Passer", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text(
                formatDuration(remaining),
                style = MaterialTheme.typography.displayLarge.tabular(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { if (total > 0) (total - remaining).toFloat() / total.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAdjust(-30) }) {
                    Text("−30 s", color = MaterialTheme.colorScheme.onPrimary)
                }
                TextButton(onClick = { onAdjust(30) }) {
                    Text("+30 s", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ManualRestDialog(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Démarrer un repos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180, 240).forEach { sec ->
                    ChoiceTile(
                        title = formatDuration(sec),
                        selected = false,
                        onClick = { onStart(sec) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ─────────────────────────── Échauffement ─────────────────────────────────

@Composable
private fun WarmupSection(warmup: CardioSessionEntity?, onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Échauffement cardio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (warmup == null) {
                Text(
                    "15-20 min de cardio léger avant la muscu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Ajouter un échauffement")
                }
            } else {
                Text(
                    "${warmup.type.labelFr()} · ${warmup.durationSec / 60} min · ~${warmup.caloriesEstimated.toInt()} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(onClick = onAdd) { Text("Remplacer") }
            }
        }
    }
}

@Composable
private fun WarmupDialog(onDismiss: () -> Unit, onConfirm: (CardioType, Int, Float?) -> Unit) {
    var type by remember { mutableStateOf(CardioType.ELLIPTICAL) }
    var duration by remember { mutableStateOf("15") }
    var rpe by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Échauffement cardio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(480.dp).verticalScroll(rememberScrollState()),
            ) {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CardioType.entries.forEach { t ->
                    ChoiceTile(title = t.labelFr(), selected = type == t, onClick = { type = t })
                }
                NumericField(label = "Durée", valueText = duration, suffix = "min", onValueChange = { duration = it })
                NumericField(label = "RPE (optionnel)", valueText = rpe, onValueChange = { rpe = it })
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Ajouter",
                enabled = (duration.toIntOrNull() ?: 0) > 0,
                onClick = { onConfirm(type, duration.toInt(), rpe.toFloatOrNull()) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ─────────────────────────────── Exercice ─────────────────────────────────

@Composable
private fun ExerciseCardView(
    card: ExerciseCard,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLogSet: (Float, Int, Float?, SetType) -> Unit,
    onEditSet: (PerformedSetEntity) -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenDetail: () -> Unit,
    onReportPain: () -> Unit,
    onGenerateWarmup: (Float) -> Unit,
    warmupPreview: (Float) -> List<WarmupSet>,
) {
    var showWarmupSets by remember(card.performed.id) { mutableStateOf(false) }
    var weightText by remember(card.performed.id) {
        mutableStateOf(
            card.performed.targetWeightKg?.let { "%.1f".format(it) }
                ?: card.lastSetPreview?.weightKg?.let { "%.1f".format(it) }
                ?: "",
        )
    }
    var repsText by remember(card.performed.id) {
        mutableStateOf(
            card.performed.targetRepsMin?.toString()
                ?: card.lastSetPreview?.reps?.toString()
                ?: "",
        )
    }
    var rpeText by remember(card.performed.id) { mutableStateOf("") }
    var setType by remember(card.performed.id) { mutableStateOf(SetType.WORKING) }

    if (showWarmupSets) {
        val topSet = weightText.toFloatOrNull() ?: 0f
        WarmupSetsDialog(
            sets = warmupPreview(topSet),
            topSetKg = topSet,
            onDismiss = { showWarmupSets = false },
            onConfirm = { onGenerateWarmup(topSet); showWarmupSets = false },
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExerciseThumbnail(
                    mediaPath = card.exercise?.mediaPath,
                    contentDescription = card.performed.exerciseNameSnapshot,
                    modifier = Modifier.clickable(onClick = onOpenDetail),
                )
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenDetail)) {
                    card.performed.supersetGroup?.let { group ->
                        Text(
                            "Superset ${supersetLetter(group)} · enchaîné, repos en fin de tour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        card.performed.exerciseNameSnapshot,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    card.targetLabel?.let { target ->
                        Text(
                            target,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (card.isTargetReached) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                card.performed.targetSets?.let { target ->
                    Text(
                        "${card.completedSets}/$target",
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        color = if (card.isTargetReached) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Monter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = "Descendre",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Retirer l'exercice",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            card.lastSetPreview?.let { last ->
                Text(
                    "Dernière séance : %.1f kg × %d".format(last.weightKg, last.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (card.sets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    card.sets.forEach { s -> SetRow(s, onClick = { onEditSet(s) }) }
                }
                Text(
                    "Touche une série pour la corriger ou la supprimer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val voiceLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { res ->
                res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                    ?.let { transcript ->
                        VoiceSetParser.parse(transcript)?.let { parsed ->
                            weightText = "%.1f".format(parsed.weightKg)
                            repsText = parsed.reps.toString()
                        }
                    }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Poids", valueText = weightText, suffix = "kg", onValueChange = { weightText = it })
                }
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Reps", valueText = repsText, onValueChange = { repsText = it })
                }
                Box(Modifier.weight(1f)) {
                    NumericField(label = "RPE", valueText = rpeText, onValueChange = { rpeText = it })
                }
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dis : 12 reps à 80 kilos")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                }) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voix", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Proposé uniquement avant la première série : l'échauffement se fait
            // en début d'exercice, pas au milieu.
            if (card.sets.isEmpty()) {
                val topSet = weightText.toFloatOrNull()
                if (topSet != null && topSet > 0f) {
                    TextButton(onClick = { showWarmupSets = true }) {
                        Text("Générer l'échauffement jusqu'à %.1f kg".format(topSet))
                    }
                }
            }

            SetTypePicker(selected = setType, onSelect = { setType = it })

            TextButton(onClick = onReportPain) {
                Text(
                    "Signaler une douleur",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    val w = weightText.toFloatOrNull() ?: return@Button
                    val r = repsText.toIntOrNull() ?: return@Button
                    onLogSet(w, r, rpeText.toFloatOrNull(), setType)
                    rpeText = ""
                    // Le type ne persiste pas d'une série à l'autre : l'oubli de
                    // repasser en « Normale » fausserait le volume en silence.
                    setType = SetType.WORKING
                },
                enabled = weightText.toFloatOrNull() != null && repsText.toIntOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Check, null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Enregistrer la série")
            }
        }
    }
}

/**
 * Aperçu des séries d'échauffement avant enregistrement.
 * On montre ce qui va être écrit plutôt que de loguer cinq séries d'un coup
 * sans prévenir.
 */
@Composable
private fun WarmupSetsDialog(
    sets: List<WarmupSet>,
    topSetKg: Float,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Échauffement") },
        text = {
            if (sets.isEmpty()) {
                Text(
                    "Cette charge est trop légère pour justifier un échauffement progressif.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Montée progressive jusqu'à %.1f kg :".format(topSetKg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    sets.forEachIndexed { index, set ->
                        Row {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Text(
                                "%.1f kg × %d".format(set.weightKg, set.reps),
                                style = MaterialTheme.typography.bodyMedium.tabular(),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "repos ${set.restSec} s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "Ces séries sont marquées échauffement : hors volume, hors records.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                enabled = sets.isNotEmpty(),
                onClick = onConfirm,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/**
 * Choix du type de série. Les six types existaient en base depuis le début,
 * mais seul WORKING était atteignable depuis l'écran.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetTypePicker(selected: SetType, onSelect: (SetType) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetType.entries.forEach { type ->
            SelectableChip(
                label = type.labelFr(),
                selected = type == selected,
                onClick = { onSelect(type) },
            )
        }
    }
}

@Composable
private fun SetRow(s: PerformedSetEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (s.type == SetType.WARMUP) "É" else "S${s.setNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        if (s.type != SetType.WORKING && s.type != SetType.WARMUP) {
            Text(
                s.type.labelFr(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Text(
            "%.1f kg × %d${s.rpe?.let { " @$it" }.orEmpty()}".format(s.weightKg, s.reps),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            modifier = Modifier.weight(1f),
        )
        if (s.isPrCandidate) {
            Text("PR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EditSetDialog(
    set: PerformedSetEntity,
    onDismiss: () -> Unit,
    onSave: (Float, Int, Float?, SetType) -> Unit,
    onDelete: () -> Unit,
) {
    var weight by remember { mutableStateOf("%.1f".format(set.weightKg)) }
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var rpe by remember { mutableStateOf(set.rpe?.toString().orEmpty()) }
    var type by remember { mutableStateOf(set.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corriger la série") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumericField(label = "Poids", valueText = weight, suffix = "kg", onValueChange = { weight = it })
                    }
                    Box(Modifier.weight(1f)) {
                        NumericField(label = "Reps", valueText = reps, onValueChange = { reps = it })
                    }
                }
                NumericField(label = "RPE (optionnel)", valueText = rpe, onValueChange = { rpe = it })
                SetTypePicker(selected = type, onSelect = { type = it })
                Text(
                    type.descriptionFr(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Supprimer cette série", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                enabled = weight.toFloatOrNull() != null && reps.toIntOrNull() != null,
                onClick = { onSave(weight.toFloat(), reps.toInt(), rpe.toFloatOrNull(), type) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ─────────────────────────────── Sortie ───────────────────────────────────

@Composable
private fun ExitDialog(
    onDismiss: () -> Unit,
    onKeepRunning: () -> Unit,
    onAbandon: () -> Unit,
    onFinish: () -> Unit,
) {
    var confirmAbandon by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmAbandon) "Abandonner la séance ?" else "Quitter la séance ?") },
        text = {
            if (confirmAbandon) {
                Text(
                    "La séance sera écartée : elle n'apparaîtra pas dans l'historique et " +
                        "ne comptera pas dans tes statistiques. Les records déjà détectés sont conservés.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ta séance reste en cours et tu pourras la reprendre depuis l'onglet Séance. " +
                            "Les séries déjà enregistrées sont conservées dans tous les cas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChoiceTile(
                        title = "Laisser en cours",
                        subtitle = "Reprendre plus tard là où tu t'es arrêté",
                        selected = false,
                        onClick = onKeepRunning,
                    )
                    ChoiceTile(
                        title = "Terminer maintenant",
                        subtitle = "Clôturer et enregistrer dans l'historique",
                        selected = false,
                        onClick = onFinish,
                    )
                    ChoiceTile(
                        title = "Abandonner",
                        subtitle = "Écarter la séance",
                        selected = false,
                        onClick = { confirmAbandon = true },
                    )
                }
            }
        },
        confirmButton = {
            if (confirmAbandon) {
                TextButton(onClick = onAbandon) {
                    Text("Abandonner", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Continuer la séance") }
            }
        },
        dismissButton = {
            if (confirmAbandon) {
                TextButton(onClick = { confirmAbandon = false }) { Text("Retour") }
            }
        },
    )
}

@Composable
private fun FinishDialog(
    isFinishing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFinish: (Float?, String?) -> Unit,
) {
    var rpe by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminer la séance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumericField(label = "RPE global (1-10)", valueText = rpe, onValueChange = { rpe = it })
                com.kps.trackmyweight.ui.common.TextField(
                    label = "Notes (optionnel)",
                    value = notes,
                    onValueChange = { notes = it },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isFinishing) "Enregistrement..." else "Terminer",
                enabled = !isFinishing,
                onClick = { onFinish(rpe.toFloatOrNull(), notes.takeIf { it.isNotBlank() }) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun AddExerciseDialog(
    exercises: List<ExerciseEntity>,
    canFilterByGym: Boolean,
    onlyMyGym: Boolean,
    hiddenCount: Int,
    onToggleGymFilter: () -> Unit,
    onCreateRequested: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises else exercises.filter { it.name.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un exercice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(400.dp)) {
                com.kps.trackmyweight.ui.common.TextField(
                    label = "Rechercher",
                    value = query,
                    onValueChange = { query = it },
                )
                if (canFilterByGym) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleGymFilter),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Uniquement ma salle", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (onlyMyGym) "$hiddenCount exercices masqués, faute d'équipement"
                                else "Catalogue complet",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = onlyMyGym, onCheckedChange = { onToggleGymFilter() })
                    }
                }
                if (query.isNotBlank() && filtered.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Aucun exercice ne correspond.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { onCreateRequested(query) }) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Créer « $query »")
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = { it.id }) { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(ex.id) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExerciseThumbnail(
                                mediaPath = ex.mediaPath,
                                contentDescription = ex.name,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    ex.primaryMuscle.labelFr(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}
