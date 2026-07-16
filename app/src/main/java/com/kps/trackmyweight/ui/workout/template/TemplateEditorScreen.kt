package com.kps.trackmyweight.ui.workout.template

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.TextField

@Composable
fun TemplateEditorScreen(
    templateId: Long?,
    onSaved: () -> Unit,
    vm: TemplateEditorViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.savedId) { if (state.savedId != null) onSaved() }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.save() },
                icon = { Icon(Icons.Outlined.Save, null) },
                text = { Text(if (state.isSaving) "Enregistrement..." else "Enregistrer") },
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
            Spacer(Modifier.height(16.dp))
            Text(
                if (state.templateId == null) "Nouveau template" else "Modifier template",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            TextField(label = "Nom", value = state.name, onValueChange = vm::setName)
            TextField(label = "Notes (optionnel)", value = state.notes, onValueChange = vm::setNotes)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exercices", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.padding(horizontal = 2.dp)); Text("Ajouter")
                }
            }

            if (state.draftExercises.isEmpty()) {
                Text(
                    "Aucun exercice ajouté. Utilise + pour en ajouter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.draftExercises.forEachIndexed { i, d ->
                    DraftExerciseCard(
                        draft = d,
                        onChangeSets = { v -> vm.updateExercise(i) { it.copy(targetSets = v) } },
                        onChangeMin = { v -> vm.updateExercise(i) { it.copy(targetRepsMin = v) } },
                        onChangeMax = { v -> vm.updateExercise(i) { it.copy(targetRepsMax = v) } },
                        onRemove = { vm.removeAt(i) },
                    )
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            exercises = state.allExercises,
            onDismiss = { showAddDialog = false },
            onPick = { ex -> vm.addExercise(ex); showAddDialog = false },
        )
    }
}

@Composable
private fun DraftExerciseCard(
    draft: TemplateDraftExercise,
    onChangeSets: (Int) -> Unit,
    onChangeMin: (Int?) -> Unit,
    onChangeMax: (Int?) -> Unit,
    onRemove: () -> Unit,
) {
    var setsText by remember(draft.exercise.id) { mutableStateOf(draft.targetSets.toString()) }
    var minText by remember(draft.exercise.id) { mutableStateOf(draft.targetRepsMin?.toString().orEmpty()) }
    var maxText by remember(draft.exercise.id) { mutableStateOf(draft.targetRepsMax?.toString().orEmpty()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(draft.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Close, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    NumericField(label = "Séries", valueText = setsText, onValueChange = {
                        setsText = it
                        it.toIntOrNull()?.let(onChangeSets)
                    })
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumericField(label = "Reps min", valueText = minText, onValueChange = {
                        minText = it; onChangeMin(it.toIntOrNull())
                    })
                }
                Box(modifier = Modifier.weight(1f)) {
                    NumericField(label = "Reps max", valueText = maxText, onValueChange = {
                        maxText = it; onChangeMax(it.toIntOrNull())
                    })
                }
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises else exercises.filter { it.name.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir un exercice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(400.dp)) {
                TextField(label = "Rechercher", value = query, onValueChange = { query = it })
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    filtered.take(60).forEach { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(ex) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(ex.primaryMuscle.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}
