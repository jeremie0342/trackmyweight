package com.kps.trackmyweight.ui.habits

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.TextField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepo: HabitRepository,
) : ViewModel() {

    val habits: StateFlow<List<HabitDefinitionEntity>> = habitRepo.observeAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch { habitRepo.setActive(id, active) }
    }

    fun save(def: HabitDefinitionEntity) {
        viewModelScope.launch { habitRepo.saveHabit(def) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { habitRepo.deleteHabit(id) }
    }
}

@Composable
fun HabitsScreen(
    onBack: () -> Unit,
    vm: HabitsViewModel = hiltViewModel(),
) {
    val habits by vm.habits.collectAsState()
    var editing by remember { mutableStateOf<HabitDefinitionEntity?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<HabitDefinitionEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Nouvelle habitude") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackHeader(title = "Mes habitudes", onBack = onBack)
            Text(
                "Active, désactive ou modifie tes habitudes quotidiennes. La cible des habitudes 'Eau' et 'Pas' définit aussi les objectifs affichés sur l'écran d'accueil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            habits.forEach { h ->
                HabitRow(
                    habit = h,
                    onToggleActive = { vm.setActive(h.id, it) },
                    onEdit = { editing = h },
                    onDelete = { confirmDelete = h },
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    editing?.let { h ->
        EditHabitDialog(
            initial = h,
            onDismiss = { editing = null },
            onSave = { updated ->
                vm.save(updated)
                editing = null
            },
        )
    }

    if (showNew) {
        EditHabitDialog(
            initial = HabitDefinitionEntity(
                key = "custom_${System.currentTimeMillis()}",
                displayName = "",
                targetPerWeek = 7,
                orderIndex = habits.size,
            ),
            onDismiss = { showNew = false },
            onSave = { new ->
                vm.save(new)
                showNew = false
            },
        )
    }

    confirmDelete?.let { h ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Supprimer l'habitude ?") },
            text = { Text("« ${h.displayName} » sera retirée définitivement, avec son historique de complétions.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(h.id); confirmDelete = null }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun HabitRow(
    habit: HabitDefinitionEntity,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                    Text(
                        habit.displayName.ifBlank { "(sans nom)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    val subtitle = buildString {
                        habit.dailyTarget?.let { t ->
                            append("Cible : ")
                            append(if (t == t.toInt().toFloat()) t.toInt().toString() else "%.1f".format(t))
                            habit.unit?.let { u -> append(" $u/jour") }
                        }
                        if (habit.dailyTarget != null && habit.targetPerWeek != null) append(" · ")
                        habit.targetPerWeek?.let { append("${it}x / semaine") }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = habit.isActive, onCheckedChange = onToggleActive)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EditHabitDialog(
    initial: HabitDefinitionEntity,
    onDismiss: () -> Unit,
    onSave: (HabitDefinitionEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initial.displayName) }
    var perWeek by remember { mutableStateOf(initial.targetPerWeek?.toString().orEmpty()) }
    var dailyTarget by remember { mutableStateOf(initial.dailyTarget?.let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() }.orEmpty()) }
    var unit by remember { mutableStateOf(initial.unit.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nouvelle habitude" else "Modifier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                TextField(label = "Nom", value = name, onValueChange = { name = it })
                Text(
                    "Cible quotidienne (optionnelle) : ex 2.5 pour '2.5 L d'eau', 10000 pour '10000 pas'.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        NumericField(label = "Cible/jour", valueText = dailyTarget, onValueChange = { dailyTarget = it })
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TextField(label = "Unité (L, pas, h, g...)", value = unit, onValueChange = { unit = it })
                    }
                }
                NumericField(label = "Objectif hebdomadaire (jours/sem)", valueText = perWeek, onValueChange = { perWeek = it })
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            displayName = name.trim(),
                            targetPerWeek = perWeek.toIntOrNull(),
                            dailyTarget = dailyTarget.toFloatOrNull(),
                            unit = unit.trim().takeIf { it.isNotBlank() },
                        ),
                    )
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
