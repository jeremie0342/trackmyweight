package com.kps.trackmyweight.ui.workout.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.db.enums.MaxLoadSource
import com.kps.trackmyweight.domain.calc.WorkingLoadRow
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.toMonthLabelFr
import com.kps.trackmyweight.ui.common.EquipmentThumbnail
import com.kps.trackmyweight.ui.common.ExerciseDemo
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.Sparkline
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.theme.tabular

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    vm: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showMaxDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val exercise = state.exercise
            BackHeader(title = exercise?.name ?: "Exercice", onBack = onBack)

            when {
                state.isLoading -> InlineLoader()
                exercise == null -> Text(
                    "Cet exercice est introuvable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    ExerciseDemo(
                        mediaPath = exercise.mediaPath,
                        contentDescription = "Démonstration : ${exercise.name}",
                    )

                    Section("Muscles") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Chip(exercise.primaryMuscle.labelFr(), primary = true)
                            exercise.secondaryMuscles.forEach { Chip(it.labelFr(), primary = false) }
                        }
                    }

                    Section("Type") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(exercise.mechanics.labelFr(), primary = false)
                            Chip(exercise.force.labelFr(), primary = false)
                            Chip("Repos ${exercise.defaultRestSec / 60} min", primary = false)
                        }
                    }

                    exercise.cues?.takeIf { it.isNotBlank() }?.let { cues ->
                        Section("Exécution") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    cues,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }

                    if (state.equipment.isNotEmpty()) {
                        Section("Matériel") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.equipment.forEach { EquipmentRow(it) }
                            }
                        }
                    }

                    state.lastSet?.let { last ->
                        Section("Dernière performance") {
                            Text(
                                "%.1f kg × %d".format(last.weightKg, last.reps) +
                                    last.rpe?.let { " · RPE $it" }.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall.tabular(),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    MaxLoadSection(
                        state = state,
                        onUpdate = { showMaxDialog = true },
                    )

                    if (state.workingLoads.isNotEmpty()) {
                        Section("Charges de travail") {
                            WorkingLoadTable(state.workingLoads)
                        }
                    }

                    if (state.monthlyTonnage.isNotEmpty()) {
                        Section("Volume par mois") {
                            MonthlyTonnageList(state.monthlyTonnage)
                        }
                    }

                    if (state.substitutes.isNotEmpty()) {
                        Section("Alternatives") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.substitutes.forEach {
                                    Text(it.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showMaxDialog) {
        MaxLoadDialog(
            onDismiss = { showMaxDialog = false },
            onTested = { kg -> vm.recordTestedMax(kg); showMaxDialog = false },
            onDeclared = { kg -> vm.recordDeclaredMax(kg); showMaxDialog = false },
            onEstimated = { kg, reps -> vm.recordEstimatedMax(kg, reps); showMaxDialog = false },
        )
    }
}

/**
 * Indicateur de chargement à hauteur fixe.
 *
 * `FullScreenLoader` de `ui.common` ne convient pas ici : son `fillMaxSize()`
 * recevrait une contrainte de hauteur infinie dans une colonne scrollable.
 */
@Composable
private fun InlineLoader() {
    Box(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun Chip(label: String, primary: Boolean) {
    val background = if (primary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val foreground = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun EquipmentRow(equipment: EquipmentEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EquipmentThumbnail(equipmentKey = equipment.key, contentDescription = equipment.displayName)
        Text(equipment.displayName, style = MaterialTheme.typography.bodyLarge)
    }
}

// ────────────────────────── Charge maximale ───────────────────────────────

@Composable
private fun MaxLoadSection(state: ExerciseDetailUiState, onUpdate: () -> Unit) {
    Section("Charge maximale") {
        val current = state.currentMax
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (current == null) {
                    Text(
                        "Aucune charge maximale connue pour cet exercice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Renseigne-la, ou logue simplement tes séries : le max estimé " +
                            "se mettra à jour tout seul à chaque record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%.1f".format(current.oneRmKg),
                            style = MaterialTheme.typography.displaySmall.tabular(),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            " kg",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        val pct = state.progressionPercent
                        if (pct != null && state.maxLoadHistory.size >= 2) ProgressionBadge(pct)
                    }
                    Text(
                        current.source.sourceLabelFr() + describeSource(current.sourceWeightKg, current.sourceReps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // La courbe n'a de sens qu'à partir de deux points de mesure.
                    if (state.maxCurve.size >= 2) {
                        Sparkline(
                            raw = state.maxCurve,
                            smoothed = state.maxCurve,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "%d mesures · de %.1f à %.1f kg".format(
                                state.maxLoadHistory.size,
                                state.firstMax?.oneRmKg ?: 0f,
                                current.oneRmKg,
                            ),
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onUpdate) {
                    Text(if (current == null) "Renseigner mon max" else "Mettre à jour")
                }
            }
        }
    }
}

private fun describeSource(weightKg: Float?, reps: Int?): String =
    if (weightKg != null && reps != null) " · %.1f kg × %d".format(weightKg, reps) else ""

@Composable
private fun ProgressionBadge(percent: Float) {
    val positive = percent >= 0f
    val color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (positive) "+" else ""
    Text(
        "$sign%.1f %%".format(percent),
        style = MaterialTheme.typography.labelLarge.tabular(),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun MaxLoadSource.sourceLabelFr(): String = when (this) {
    MaxLoadSource.TESTED -> "Testé"
    MaxLoadSource.DECLARED -> "Déclaré"
    MaxLoadSource.ESTIMATED -> "Estimé depuis une série"
}

/** Table des charges de travail, en pourcentage du max de référence. */
@Composable
private fun WorkingLoadTable(rows: List<WorkingLoadRow>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${row.percentOfMax} %",
                        style = MaterialTheme.typography.labelLarge.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(56.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "%.1f kg".format(row.weightKg),
                            style = MaterialTheme.typography.bodyLarge.tabular(),
                            fontWeight = FontWeight.Medium,
                        )
                        row.plates?.let { load ->
                            Text(
                                platesLabel(load.platesPerSide),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        "~${row.estimatedReps} reps",
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Ex. `par côté : 20 + 10 + 2.5`. */
private fun platesLabel(platesPerSide: List<Float>): String {
    if (platesPerSide.isEmpty()) return "barre à vide"
    val formatted = platesPerSide.joinToString(" + ") { plate ->
        if (plate % 1f == 0f) "%.0f".format(plate) else "%.2f".format(plate).trimEnd('0').trimEnd('.')
    }
    return "par côté : $formatted"
}

@Composable
private fun MonthlyTonnageList(rows: List<MonthlyTonnageRow>) {
    val max = rows.maxOfOrNull { it.volumeKg }?.takeIf { it > 0f } ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.reversed().forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(
                        row.month.toMonthLabelFr(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "%.0f kg".format(row.volumeKg),
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        fontWeight = FontWeight.Medium,
                    )
                }
                LinearProgressIndicator(
                    progress = { row.volumeKg / max },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
        }
    }
}

// ───────────────────────────── Saisie du max ──────────────────────────────

@Composable
private fun MaxLoadDialog(
    onDismiss: () -> Unit,
    onTested: (Float) -> Unit,
    onDeclared: (Float) -> Unit,
    onEstimated: (Float, Int) -> Unit,
) {
    var mode by remember { mutableStateOf(MaxLoadSource.ESTIMATED) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    val weightValue = weight.toFloatOrNull()
    val repsValue = reps.toIntOrNull()
    val canSave = when (mode) {
        MaxLoadSource.ESTIMATED ->
            weightValue != null && weightValue > 0f && repsValue != null && repsValue > 0
        else -> weightValue != null && weightValue > 0f
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charge maximale") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                ChoiceTile(
                    title = "Estimer depuis une série",
                    subtitle = "Le 1RM est calculé (Epley/Brzycki)",
                    selected = mode == MaxLoadSource.ESTIMATED,
                    onClick = { mode = MaxLoadSource.ESTIMATED },
                )
                ChoiceTile(
                    title = "J'ai testé mon max",
                    subtitle = "Une répétition à cette charge",
                    selected = mode == MaxLoadSource.TESTED,
                    onClick = { mode = MaxLoadSource.TESTED },
                )
                ChoiceTile(
                    title = "Je connais mon max",
                    subtitle = "Valeur du coach, ancien carnet…",
                    selected = mode == MaxLoadSource.DECLARED,
                    onClick = { mode = MaxLoadSource.DECLARED },
                )
                NumericField(
                    label = if (mode == MaxLoadSource.DECLARED) "1RM" else "Charge",
                    valueText = weight,
                    suffix = "kg",
                    onValueChange = { weight = it },
                )
                if (mode == MaxLoadSource.ESTIMATED) {
                    NumericField(label = "Répétitions", valueText = reps, onValueChange = { reps = it })
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                enabled = canSave,
                onClick = {
                    val w = weightValue ?: return@PrimaryButton
                    when (mode) {
                        MaxLoadSource.TESTED -> onTested(w)
                        MaxLoadSource.DECLARED -> onDeclared(w)
                        MaxLoadSource.ESTIMATED -> onEstimated(w, repsValue ?: return@PrimaryButton)
                    }
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
