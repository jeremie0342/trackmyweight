package com.kps.trackmyweight.ui.workout.rotation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SelectableChip
import com.kps.trackmyweight.ui.common.TextField

/** Lundi = 1, conformément à la norme ISO utilisée en base. */
private val DAYS_FR = listOf(
    "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche",
)

internal fun dayLabelFr(isoDay: Int): String = DAYS_FR.getOrElse(isoDay - 1) { "Jour $isoDay" }

@Composable
fun RotationsScreen(
    onBack: () -> Unit,
    vm: RotationsViewModel = hiltViewModel(),
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
            BackHeader(title = "Rotations", onBack = onBack)

            Text(
                "Une rotation alterne plusieurs templates sur un même jour de la " +
                    "semaine. L'app te propose celui qui vient, en fonction du dernier fait.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.templates.size < 2) {
                Text(
                    "Il faut au moins deux templates pour créer une rotation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (state.rotations.isEmpty()) {
                    Text(
                        "Aucune rotation pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.rotations.forEach { rotation ->
                            RotationCard(
                                rotation = rotation,
                                onEdit = { vm.startEdit(rotation) },
                                onDelete = { vm.delete(rotation.group.id) },
                            )
                        }
                    }
                }
                TextButton(onClick = vm::startNew) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Nouvelle rotation")
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    state.draft?.let { draft ->
        RotationEditorDialog(
            draft = draft,
            templates = state.templates,
            onName = vm::setName,
            onDay = vm::setDayOfWeek,
            onToggleTemplate = vm::toggleTemplate,
            onMove = vm::moveTemplate,
            onDismiss = vm::cancelEdit,
            onSave = vm::save,
        )
    }
}

@Composable
private fun RotationCard(
    rotation: RotationWithTemplates,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rotation.group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        dayLabelFr(rotation.group.dayOfWeek),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onEdit) { Text("Modifier") }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                rotation.templates.joinToString(" → ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RotationEditorDialog(
    draft: RotationDraft,
    templates: List<com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity>,
    onName: (String) -> Unit,
    onDay: (Int) -> Unit,
    onToggleTemplate: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val templatesById = templates.associateBy { it.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.groupId == null) "Nouvelle rotation" else "Modifier la rotation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                TextField(label = "Nom", value = draft.name, onValueChange = onName)

                Text(
                    "Jour de la semaine",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..7).forEach { day ->
                        SelectableChip(
                            label = dayLabelFr(day).take(3),
                            selected = draft.dayOfWeek == day,
                            onClick = { onDay(day) },
                        )
                    }
                }

                Text(
                    "Templates à alterner",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    templates.forEach { template ->
                        SelectableChip(
                            label = template.name,
                            selected = template.id in draft.templateIds,
                            onClick = { onToggleTemplate(template.id) },
                        )
                    }
                }

                if (draft.templateIds.size >= 2) {
                    Text(
                        "Ordre d'alternance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    draft.templateIds.forEachIndexed { index, id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${index + 1}. ${templatesById[id]?.name ?: "?"}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Monter")
                            }
                            IconButton(
                                onClick = { onMove(index, 1) },
                                enabled = index < draft.templateIds.lastIndex,
                            ) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Descendre")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(text = "Enregistrer", enabled = draft.isValid, onClick = onSave)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
