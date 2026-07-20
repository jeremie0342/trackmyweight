package com.kps.trackmyweight.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.domain.calc.ReadinessLevel
import com.kps.trackmyweight.domain.calc.SleepDuration
import com.kps.trackmyweight.ui.common.PrimaryButton

@Composable
fun HomeScreen(
    onOpenReports: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showReadiness by remember { mutableStateOf(false) }
    var showWater by remember { mutableStateOf(false) }
    var showPulse by remember { mutableStateOf(false) }
    var showReadinessHelp by remember { mutableStateOf(false) }

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
            Text(
                "Aujourd'hui",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                state.date.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReportsShortcut(onClick = onOpenReports)
            ReadinessCard(state = state, onOpen = { showReadiness = true }, onHelp = { showReadinessHelp = true })
            WeightSummary(state)
            MacrosSummary(state)
            SleepCard(state)
            WaterCard(mlToday = state.waterMl, targetMl = state.waterTargetMl, onAdd = { showWater = true })
            HabitsCard(state.habits, doneHabitIds = state.completions.filter { it.isDone }.map { it.habitId }.toSet(), onToggle = vm::toggleHabit)
            PulseCard(state.dailyLog?.restingHrBpm, onLog = { showPulse = true })

            Spacer(Modifier.height(120.dp))
        }
    }

    if (showReadiness) {
        ReadinessDialog(
            initSleep = state.dailyLog?.readinessSleep,
            initEnergy = state.dailyLog?.readinessEnergy,
            initSoreness = state.dailyLog?.readinessSoreness,
            initMood = state.dailyLog?.readinessMood,
            onDismiss = { showReadiness = false },
            onSave = { s, e, so, m -> vm.saveReadiness(s, e, so, m); showReadiness = false },
        )
    }
    if (showWater) {
        WaterDialog(
            onDismiss = { showWater = false },
            onLog = { ml -> vm.logWater(ml); showWater = false },
        )
    }
    if (showPulse) {
        PulseDialog(
            onDismiss = { showPulse = false },
            onLog = { bpm -> vm.logPulse(bpm); showPulse = false },
        )
    }
    if (showReadinessHelp) {
        AlertDialog(
            onDismissRequest = { showReadinessHelp = false },
            title = { Text("Check-in matinal, comment ça marche ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ton score sur 5 est la moyenne de 4 notes (1 à 5) que tu donnes en te levant :",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("• Sommeil — qualité perçue (pas les heures)", style = MaterialTheme.typography.bodySmall)
                    Text("• Énergie — 1 = à plat, 5 = pétant la forme", style = MaterialTheme.typography.bodySmall)
                    Text("• Courbatures — 1 = très courbaturé, 5 = aucune", style = MaterialTheme.typography.bodySmall)
                    Text("• Humeur — 1 = déprimé, 5 = excellent", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "\n< 2 = journée off · 2-2.8 = allège de 20-30% · 2.8-3.6 = standard · 3.6-4.4 = bonne forme · ≥ 4.4 = push day (vise des PRs).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showReadinessHelp = false }) { Text("Compris") } },
        )
    }
}

// ─────────── Cards ───────────

@Composable
private fun ReportsShortcut(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Rapport hebdo & Coach", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            Text("Synthèse de la semaine, projection vers l'objectif et conseils personnalisés.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun ReadinessCard(state: HomeUiState, onOpen: () -> Unit, onHelp: () -> Unit) {
    val r = state.readiness
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Readiness", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = onHelp) { Text("?") }
            }
            if (r == null || r.filledDimensions == 0) {
                Text("Fais ton check-in matinal", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap pour saisir sommeil / énergie / courbatures / humeur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(r.score), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("/ 5", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(r.level.name, style = MaterialTheme.typography.titleMedium, color = colorFor(r.level))
                Text(r.advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun colorFor(level: ReadinessLevel) = when (level) {
    ReadinessLevel.POOR, ReadinessLevel.LOW -> MaterialTheme.colorScheme.error
    ReadinessLevel.MODERATE -> MaterialTheme.colorScheme.tertiary
    ReadinessLevel.GOOD, ReadinessLevel.EXCELLENT -> MaterialTheme.colorScheme.primary
}

@Composable
private fun WeightSummary(state: HomeUiState) {
    val w = state.lastWeight ?: return
    val goal = state.goal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Poids", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f".format(w.weightKg), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(horizontal = 12.dp))
                if (goal != null) {
                    val delta = w.weightKg - goal.targetWeightKg
                    val txt = if (delta > 0) "+%.1f vs objectif".format(delta) else "%.1f vs objectif".format(delta)
                    Text(txt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MacrosSummary(state: HomeUiState) {
    val phase = state.phase ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alimentation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Calories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.0f / %d".format(state.kcalConsumed, phase.targetKcal), style = MaterialTheme.typography.titleMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Protéines", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.0f / %d g".format(state.proteinConsumed, phase.targetProteinG), style = MaterialTheme.typography.titleMedium)
                }
            }
            LinearProgressIndicator(
                progress = { (state.kcalConsumed / phase.targetKcal.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SleepCard(state: HomeUiState) {
    val s = state.sleepEntry
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sommeil", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s == null) {
                Text("Non renseigné", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(SleepDuration.format(s.durationMin), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                s.qualityRating?.let {
                    Text("Qualité $it / 5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WaterCard(mlToday: Int, targetMl: Int, onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalDrink, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text("Eau", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("$mlToday ml / $targetMl ml", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { (mlToday.toFloat() / targetMl).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HabitsCard(
    habits: List<HabitDefinitionEntity>,
    doneHabitIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
) {
    if (habits.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Habitudes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            habits.forEach { h ->
                val done = h.id in doneHabitIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(h.id, !done) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.padding(horizontal = 10.dp))
                    Text(h.displayName, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun PulseCard(bpm: Int?, onLog: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onLog),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Pouls repos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                bpm?.let { "$it bpm" } ?: "Non renseigné",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────── Dialogs ───────────

@Composable
private fun ReadinessDialog(
    initSleep: Int?,
    initEnergy: Int?,
    initSoreness: Int?,
    initMood: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?, Int?, Int?) -> Unit,
) {
    var sleep by remember { mutableFloatStateOf((initSleep ?: 3).toFloat()) }
    var energy by remember { mutableFloatStateOf((initEnergy ?: 3).toFloat()) }
    var soreness by remember { mutableFloatStateOf((initSoreness ?: 3).toFloat()) }
    var mood by remember { mutableFloatStateOf((initMood ?: 3).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check-in matinal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RatingRow("Sommeil", sleep) { sleep = it }
                RatingRow("Énergie", energy) { energy = it }
                RatingRow("Absence courbatures", soreness) { soreness = it }
                RatingRow("Humeur", mood) { mood = it }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                onClick = { onSave(sleep.toInt(), energy.toInt(), soreness.toInt(), mood.toInt()) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun RatingRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${value.toInt()} / 5", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = 1f..5f, steps = 3)
    }
}

@Composable
private fun WaterDialog(onDismiss: () -> Unit, onLog: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter de l'eau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(250, 500, 750, 1000).forEach { ml ->
                    androidx.compose.material3.Button(
                        onClick = { onLog(ml) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("+ $ml ml") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun PulseDialog(onDismiss: () -> Unit, onLog: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pouls au repos") },
        text = {
            com.kps.trackmyweight.ui.common.NumericField(
                label = "BPM (bat/min)", valueText = text,
                onValueChange = { text = it },
            )
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                onClick = { text.toIntOrNull()?.let(onLog) },
                enabled = text.toIntOrNull() != null,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
