package com.kps.trackmyweight.ui.gyms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.db.enums.EquipmentCategory
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.TextField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GymsUiState(
    val gyms: List<GymEntity> = emptyList(),
    val equipment: List<EquipmentEntity> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class GymsViewModel @Inject constructor(
    private val gymRepo: GymRepository,
) : ViewModel() {

    private val _msg = MutableStateFlow<String?>(null)

    val state: StateFlow<GymsUiState> = combine(
        gymRepo.observeGyms(),
        gymRepo.observeAllEquipment(),
        _msg,
    ) { gyms, equipment, msg ->
        GymsUiState(gyms = gyms, equipment = equipment, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymsUiState())

    fun setDefault(gymId: Long) {
        viewModelScope.launch {
            gymRepo.setDefaultGym(gymId)
            _msg.value = "Salle par défaut mise à jour."
        }
    }

    fun createGym(name: String, equipmentIds: Set<Long>) {
        viewModelScope.launch {
            gymRepo.createGymWithEquipment(name, equipmentIds, makeDefault = true)
            _msg.value = "Salle créée."
        }
    }

    fun clearMessage() { _msg.value = null }
}

@Composable
fun GymsScreen(
    onBack: () -> Unit = {},
    onEditGym: (Long) -> Unit = {},
    vm: GymsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Outlined.Check, null) },
                text = { Text("Nouvelle salle") },
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
            com.kps.trackmyweight.ui.common.BackHeader(title = "Mes salles", onBack = onBack)
            Text(
                "La salle active filtre les exercices proposés en séance selon son équipement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (state.gyms.isEmpty()) {
                Text("Aucune salle. Crée-en une avec le bouton +.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.gyms.forEach { g ->
                    GymRow(g, onSetDefault = { vm.setDefault(g.id) }, onEdit = { onEditGym(g.id) })
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showCreate) {
        CreateGymDialog(
            equipment = state.equipment,
            onDismiss = { showCreate = false },
            onCreate = { name, ids -> vm.createGym(name, ids); showCreate = false },
        )
    }
}

@Composable
private fun GymRow(g: GymEntity, onSetDefault: () -> Unit, onEdit: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSetDefault),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (g.isDefault) "Salle active" else "Tap pour définir par défaut",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (g.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.TextButton(onClick = onEdit) { Text("Modifier") }
            if (g.isDefault) {
                Icon(Icons.Outlined.Star, contentDescription = "Actif", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CreateGymDialog(
    equipment: List<EquipmentEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, Set<Long>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val byCat = equipment.groupBy { it.category }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une salle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(500.dp)) {
                TextField(label = "Nom de la salle", value = name, onValueChange = { name = it })
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    byCat.forEach { (cat, list) ->
                        Text(cat.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                        list.forEach { eq ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selected = if (eq.id in selected) selected - eq.id else selected + eq.id
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(eq.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(checked = eq.id in selected, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Créer",
                onClick = { onCreate(name.trim().ifBlank { "Ma salle" }, selected) },
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
