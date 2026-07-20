package com.kps.trackmyweight.ui.workout.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.IconButton
import com.kps.trackmyweight.domain.calc.VoiceSetParser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.workout.cardio.labelFr

@Composable
fun SessionActiveScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    onBack: () -> Unit = onFinished,
    vm: SessionActiveViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.isDone) { if (state.isDone) onFinished() }

    var showFinishDialog by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var showWarmupDialog by remember { mutableStateOf(false) }

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
                com.kps.trackmyweight.ui.common.BackHeader(
                    title = "Séance en cours",
                    onBack = onBack,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showFinishDialog = true }) {
                    Text("Terminer")
                }
            }

            WarmupSection(
                warmup = state.warmup,
                onAdd = { showWarmupDialog = true },
            )

            if (state.restTotalSec > 0 && state.restRemainingSec > 0) {
                RestTimerCard(
                    remaining = state.restRemainingSec,
                    total = state.restTotalSec,
                    onCancel = vm::cancelRest,
                )
            }

            if (state.exercises.isEmpty()) {
                Text(
                    "Ajoute ton premier exercice avec le bouton +.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.exercises.forEach { card ->
                    ExerciseCardView(card = card, onLogSet = { w, r, rpe -> vm.logSet(card.performed, w, r, rpe) })
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }

    if (showFinishDialog) {
        FinishDialog(
            isFinishing = state.isFinishing,
            error = state.errorMessage,
            onDismiss = { showFinishDialog = false; vm.clearError() },
            onFinish = { rpe, notes ->
                vm.finishSession(rpe, notes)
                showFinishDialog = false
            },
        )
    }

    if (showAddExerciseSheet) {
        AddExerciseDialog(
            exercises = state.allExercises,
            onDismiss = { showAddExerciseSheet = false },
            onPick = { id -> vm.addExercise(id); showAddExerciseSheet = false },
        )
    }

    if (showWarmupDialog) {
        WarmupDialog(
            onDismiss = { showWarmupDialog = false },
            onConfirm = { type, min, rpe ->
                vm.logWarmup(type, min, rpe)
                showWarmupDialog = false
            },
        )
    }
}

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
private fun WarmupDialog(
    onDismiss: () -> Unit,
    onConfirm: (CardioType, Int, Float?) -> Unit,
) {
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

@Composable
private fun RestTimerCard(remaining: Int, total: Int, onCancel: () -> Unit) {
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
                    Text("Skip", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text(
                formatMinSec(remaining),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { (total - remaining).toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
            )
        }
    }
}

private fun formatMinSec(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun ExerciseCardView(
    card: ExerciseCard,
    onLogSet: (Float, Int, Float?) -> Unit,
) {
    var weightText by remember(card.performed.id) {
        mutableStateOf(card.lastSetPreview?.weightKg?.let { "%.1f".format(it) } ?: "")
    }
    var repsText by remember(card.performed.id) {
        mutableStateOf(card.lastSetPreview?.reps?.toString() ?: "")
    }
    var rpeText by remember(card.performed.id) { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                card.performed.exerciseNameSnapshot,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            card.lastSetPreview?.let { last ->
                Text(
                    "Dernière séance : %.1f kg × %d".format(last.weightKg, last.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Sets déjà loggées
            if (card.sets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    card.sets.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "S${s.setNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Text(
                                "%.1f kg × %d${s.rpe?.let { " @$it" }.orEmpty()}".format(s.weightKg, s.reps),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (s.isPrCandidate) {
                                Text(
                                    "  PR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                val transcript = res.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (transcript != null) {
                    VoiceSetParser.parse(transcript)?.let { parsed ->
                        weightText = "%.1f".format(parsed.weightKg)
                        repsText = parsed.reps.toString()
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    NumericField(label = "Poids", valueText = weightText, suffix = "kg", onValueChange = { weightText = it })
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumericField(label = "Reps", valueText = repsText, onValueChange = { repsText = it })
                }
                Box(modifier = Modifier.weight(1f)) {
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
            Button(
                onClick = {
                    val w = weightText.toFloatOrNull() ?: return@Button
                    val r = repsText.toIntOrNull() ?: return@Button
                    val rpe = rpeText.toFloatOrNull()
                    onLogSet(w, r, rpe)
                    rpeText = ""
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    filtered.take(60).forEach { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(ex.id) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    ex.primaryMuscle.name,
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
