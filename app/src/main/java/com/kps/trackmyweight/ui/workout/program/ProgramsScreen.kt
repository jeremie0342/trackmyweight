package com.kps.trackmyweight.ui.workout.program

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SelectableChip
import com.kps.trackmyweight.ui.common.TextField
import com.kps.trackmyweight.ui.theme.tabular
import com.kps.trackmyweight.ui.workout.rotation.dayLabelFr

/**
 * Programmes : le planning de la semaine, répété sur un bloc de N semaines.
 */
@Composable
fun ProgramsScreen(
    onBack: () -> Unit,
    vm: ProgramsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackHeader(title = "Programmes", onBack = onBack)

            Text(
                "Un programme fixe ce que tu fais chaque jour de la semaine, et se " +
                    "répète sur un bloc de quelques semaines. Le programme actif prend " +
                    "le pas sur les rotations isolées.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.templates.isEmpty()) {
                Text(
                    "Crée au moins un template avant de bâtir un programme.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (state.programs.isEmpty()) {
                    Text(
                        "Aucun programme pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.programs.forEach { item ->
                            ProgramCard(
                                item = item,
                                todayLabel = state.today,
                                templateNameOf = { id -> state.templates.firstOrNull { it.id == id }?.name },
                                rotationNameOf = { id -> state.rotations.firstOrNull { it.id == id }?.name },
                                onEdit = { vm.startEdit(item) },
                                onActivate = { vm.activate(item.program.id) },
                                onDeactivate = vm::deactivateAll,
                                onDelete = { vm.delete(item.program.id) },
                            )
                        }
                    }
                }
                TextButton(onClick = vm::startNew) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Nouveau programme")
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    state.draft?.let { draft ->
        ProgramEditorDialog(
            draft = draft,
            state = state,
            onName = vm::setName,
            onWeeks = vm::setWeeks,
            onDay = vm::setDay,
            onToggleActive = vm::toggleMakeActive,
            onDismiss = vm::cancelEdit,
            onSave = vm::save,
        )
    }
}

@Composable
private fun ProgramCard(
    item: ProgramWithDays,
    todayLabel: kotlinx.datetime.LocalDate,
    templateNameOf: (Long) -> String?,
    rotationNameOf: (Long) -> String?,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = item.progressAt(todayLabel)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.program.isActive) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.program.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${item.trainingDayCount} jour(s) d'entraînement par semaine",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item.program.isActive) {
                Text(
                    "Semaine ${progress.currentWeek} sur ${progress.totalWeeks}" +
                        when {
                            progress.isOverdue -> " · bloc terminé, il est temps d'en planifier un nouveau"
                            progress.isFinalWeek -> " · dernière semaine du bloc"
                            else -> ""
                        },
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = if (progress.isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..7).forEach { day ->
                    val entry = item.days.firstOrNull { it.dayOfWeek == day }
                    val label = when {
                        entry == null -> "—"
                        entry.isRest -> "Repos"
                        entry.templateId != null -> templateNameOf(entry.templateId) ?: "?"
                        entry.rotationGroupId != null ->
                            "Rotation ${rotationNameOf(entry.rotationGroupId) ?: "?"}"
                        else -> "—"
                    }
                    Row {
                        Text(
                            dayLabelFr(day),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(88.dp),
                        )
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Modifier") }
                if (item.program.isActive) {
                    TextButton(onClick = onDeactivate) { Text("Désactiver") }
                } else {
                    TextButton(onClick = onActivate) { Text("Activer") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramEditorDialog(
    draft: ProgramDraft,
    state: ProgramsUiState,
    onName: (String) -> Unit,
    onWeeks: (String) -> Unit,
    onDay: (Int, DaySlot) -> Unit,
    onToggleActive: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.programId == null) "Nouveau programme" else "Modifier le programme") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            ) {
                TextField(label = "Nom", value = draft.name, onValueChange = onName)
                NumericField(
                    label = "Durée du bloc",
                    valueText = draft.mesocycleWeeks,
                    suffix = "semaines",
                    onValueChange = onWeeks,
                )

                (1..7).forEach { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            dayLabelFr(day),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val slot = draft.days[day] ?: DaySlot.Empty
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SelectableChip(
                                label = "—",
                                selected = slot is DaySlot.Empty,
                                onClick = { onDay(day, DaySlot.Empty) },
                            )
                            SelectableChip(
                                label = "Repos",
                                selected = slot is DaySlot.Rest,
                                onClick = { onDay(day, DaySlot.Rest) },
                            )
                            state.templates.forEach { template ->
                                SelectableChip(
                                    label = template.name,
                                    selected = slot is DaySlot.Template && slot.templateId == template.id,
                                    onClick = { onDay(day, DaySlot.Template(template.id)) },
                                )
                            }
                            state.rotations.forEach { rotation ->
                                SelectableChip(
                                    label = "↻ ${rotation.name}",
                                    selected = slot is DaySlot.Rotation &&
                                        slot.rotationGroupId == rotation.id,
                                    onClick = { onDay(day, DaySlot.Rotation(rotation.id)) },
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        Text(
                            "Rendre ce programme actif",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(checked = draft.makeActive, onCheckedChange = { onToggleActive() })
                }
                Text(
                    "Un seul programme peut être actif à la fois.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(text = "Enregistrer", enabled = draft.isValid, onClick = onSave)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
