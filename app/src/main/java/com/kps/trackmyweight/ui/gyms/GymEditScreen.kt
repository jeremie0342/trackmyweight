package com.kps.trackmyweight.ui.gyms

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
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.TextField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GymEditUiState(
    val gym: GymEntity? = null,
    val name: String = "",
    val allEquipment: List<EquipmentEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class GymEditViewModel @Inject constructor(
    private val gymRepo: GymRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val gymId: Long = savedState.get<Long>("id") ?: 0L
    private val _state = MutableStateFlow(GymEditUiState())

    val state: StateFlow<GymEditUiState> = combine(
        _state,
        gymRepo.observeAllEquipment(),
    ) { s, equipment ->
        s.copy(allEquipment = equipment)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymEditUiState())

    init {
        viewModelScope.launch {
            val gym = gymRepo.getGym(gymId) ?: return@launch
            val selected = gymRepo.observeEquipmentForGym(gymId).first().map { it.id }.toSet()
            _state.update { it.copy(gym = gym, name = gym.name, selectedIds = selected) }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }

    fun toggleEquipment(id: Long) = _state.update {
        it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(errorMessage = "Nom requis") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { gymRepo.updateGym(gymId, s.name.trim(), s.selectedIds) }
                .onSuccess { _state.update { it.copy(isSaving = false, savedOk = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }
}

@Composable
fun GymEditScreen(
    onBack: () -> Unit,
    vm: GymEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedOk) { if (state.savedOk) onBack() }

    val byCategory = state.allEquipment.groupBy { it.category }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::save,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackHeader(title = "Modifier salle", onBack = onBack)

            TextField(label = "Nom de la salle", value = state.name, onValueChange = vm::setName)

            Text(
                "Équipement disponible",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            byCategory.forEach { (cat, list) ->
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                list.forEach { eq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.toggleEquipment(eq.id) }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(eq.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = eq.id in state.selectedIds, onCheckedChange = null)
                    }
                }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}
