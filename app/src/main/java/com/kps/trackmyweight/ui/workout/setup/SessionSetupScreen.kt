package com.kps.trackmyweight.ui.workout.setup

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.repository.PlannedExercise
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.ExerciseThumbnail
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.TextField
import com.kps.trackmyweight.ui.workout.exercise.CreateExerciseDialog
import com.kps.trackmyweight.ui.common.labelFr

/**
 * Préparation de la séance : on ajuste, puis on lance.
 * La séance n'existe en base qu'après appui sur « Lancer ».
 */
@Composable
fun SessionSetupScreen(
    onStarted: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit = {},
    vm: SessionSetupViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var createFromQuery by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.startedSessionId) {
        state.startedSessionId?.let(onStarted)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackHeader(title = state.title, onBack = onBack)

            if (state.gyms.size > 1) {
                Section("Salle") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.gyms.forEach { gym ->
                            com.kps.trackmyweight.ui.common.ChoiceTile(
                                title = gym.name,
                                selected = state.selectedGymId == gym.id,
                                onClick = { vm.selectGym(gym.id) },
                            )
                        }
                    }
                }
            }

            Section("Exercices") {
                if (state.plan.isEmpty()) {
                    Text(
                        "Aucun exercice pour l'instant. Ajoute-en pour pouvoir lancer la séance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.plan.forEachIndexed { index, planned ->
                            PlannedExerciseCard(
                                planned = planned,
                                position = index,
                                total = state.plan.size,
                                onRemove = { vm.removeAt(index) },
                                onMoveUp = { vm.moveUp(index) },
                                onMoveDown = { vm.moveDown(index) },
                                onChange = { transform -> vm.updateAt(index, transform) },
                                onOpenDetail = { onOpenExercise(planned.exerciseId) },
                                linkedWithNext = index + 1 < state.plan.size &&
                                    planned.supersetGroup != null &&
                                    planned.supersetGroup == state.plan[index + 1].supersetGroup,
                                onToggleSuperset = { vm.toggleSupersetWithNext(index) },
                            )
                        }
                    }
                }
                TextButton(onClick = { showPicker = true }) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Ajouter un exercice")
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            PrimaryButton(
                text = if (state.isStarting) "Lancement..." else "Lancer la séance",
                enabled = state.canStart,
                onClick = vm::start,
            )

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = state.pickerExercises,
            canFilterByGym = state.canFilterByGym,
            onlyMyGym = state.onlyMyGym,
            hiddenCount = state.hiddenByGymCount,
            onToggleGymFilter = vm::toggleOnlyMyGym,
            onCreateRequested = { query -> createFromQuery = query; showPicker = false },
            onDismiss = { showPicker = false },
            onPick = { vm.addExercise(it); showPicker = false },
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
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun PlannedExerciseCard(
    planned: PlannedExercise,
    position: Int,
    total: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onChange: ((PlannedExercise) -> PlannedExercise) -> Unit,
    onOpenDetail: () -> Unit,
    linkedWithNext: Boolean,
    onToggleSuperset: () -> Unit,
) {
    var sets by remember(planned.exerciseId) { mutableStateOf(planned.targetSets?.toString().orEmpty()) }
    var repsMin by remember(planned.exerciseId) { mutableStateOf(planned.targetRepsMin?.toString().orEmpty()) }
    var repsMax by remember(planned.exerciseId) { mutableStateOf(planned.targetRepsMax?.toString().orEmpty()) }
    var weight by remember(planned.exerciseId) {
        mutableStateOf(planned.targetWeightKg?.let { "%.1f".format(it) }.orEmpty())
    }
    var rest by remember(planned.exerciseId) { mutableStateOf(planned.restSecOverride?.toString().orEmpty()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenDetail)) {
                    planned.supersetGroup?.let { group ->
                        Text(
                            "Superset ${supersetLetter(group)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        planned.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                IconButton(onClick = onMoveUp, enabled = position > 0) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Monter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveDown, enabled = position < total - 1) {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = "Descendre",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Séries", valueText = sets, onValueChange = {
                        sets = it
                        onChange { p -> p.copy(targetSets = it.toIntOrNull()) }
                    })
                }
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Reps min", valueText = repsMin, onValueChange = {
                        repsMin = it
                        onChange { p -> p.copy(targetRepsMin = it.toIntOrNull()) }
                    })
                }
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Reps max", valueText = repsMax, onValueChange = {
                        repsMax = it
                        onChange { p -> p.copy(targetRepsMax = it.toIntOrNull()) }
                    })
                }
            }
            if (position < total - 1) {
                TextButton(onClick = onToggleSuperset) {
                    Text(
                        if (linkedWithNext) "Dissocier du suivant"
                        else "Enchaîner avec le suivant (superset)",
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Charge", valueText = weight, suffix = "kg", onValueChange = {
                        weight = it
                        onChange { p -> p.copy(targetWeightKg = it.toFloatOrNull()) }
                    })
                }
                Box(Modifier.weight(1f)) {
                    NumericField(label = "Repos", valueText = rest, suffix = "s", onValueChange = {
                        rest = it
                        onChange { p -> p.copy(restSecOverride = it.toIntOrNull()) }
                    })
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<ExerciseEntity>,
    canFilterByGym: Boolean,
    onlyMyGym: Boolean,
    hiddenCount: Int,
    onToggleGymFilter: () -> Unit,
    onCreateRequested: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
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
                TextField(label = "Rechercher", value = query, onValueChange = { query = it })
                if (canFilterByGym) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleGymFilter),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Uniquement ma salle",
                                style = MaterialTheme.typography.bodyMedium,
                            )
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
                                .clickable { onPick(ex) }
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

/** 1 devient A, 2 devient B… Les groupes sont numérotés à partir de 1. */
internal fun supersetLetter(group: Int): String =
    ('A' + ((group - 1).coerceAtLeast(0) % 26)).toString()
