package com.kps.trackmyweight.ui.workout.cardio

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.repository.CardioBlockDraft
import com.kps.trackmyweight.data.repository.CardioRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.MetCalories
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardioBlockUi(
    val type: CardioType,
    val durationMin: Int,
    val distanceKm: Float?,
    val rpe: Float?,
)

data class CardioLogUiState(
    val recent: List<CardioSessionEntity> = emptyList(),
    val blocks: List<CardioBlockUi> = emptyList(),
    val bodyWeightKg: Float? = null,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CardioLogViewModel @Inject constructor(
    private val cardioRepo: CardioRepository,
    private val weightRepo: WeightRepository,
) : ViewModel() {

    private val _draft = MutableStateFlow(CardioLogUiState())

    val state: StateFlow<CardioLogUiState> = combine(
        cardioRepo.observeRecent(30),
        weightRepo.observeLast(),
        _draft,
    ) { recent, weight, draft ->
        draft.copy(recent = recent, bodyWeightKg = weight?.weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardioLogUiState())

    fun addBlock(block: CardioBlockUi) {
        _draft.update { it.copy(blocks = it.blocks + block, errorMessage = null) }
    }

    fun removeBlock(index: Int) {
        _draft.update { it.copy(blocks = it.blocks.toMutableList().apply { removeAt(index) }) }
    }

    fun save() {
        val s = _draft.value
        if (s.blocks.isEmpty()) {
            _draft.update { it.copy(errorMessage = "Ajoute au moins un bloc.") }
            return
        }
        val body = s.bodyWeightKg ?: 70f
        _draft.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                cardioRepo.logMultiBlock(
                    blocks = s.blocks.map { b ->
                        CardioBlockDraft(
                            type = b.type,
                            durationSec = b.durationMin * 60,
                            distanceM = b.distanceKm?.let { it * 1000f },
                            avgRpe = b.rpe,
                        )
                    },
                    bodyWeightKg = body,
                )
            }.onSuccess {
                _draft.update {
                    CardioLogUiState(bodyWeightKg = s.bodyWeightKg, recent = it.recent, savedOk = true)
                }
            }.onFailure { e ->
                _draft.update { it.copy(isSaving = false, errorMessage = e.message ?: "Erreur d'enregistrement") }
            }
        }
    }

    fun consumeSaved() = _draft.update { it.copy(savedOk = false) }
}

@Composable
fun CardioLogScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = onDone,
    vm: CardioLogViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showAddBlock by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedOk) { if (state.savedOk) { vm.consumeSaved(); onDone() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::save,
                icon = { Icon(Icons.Outlined.Check, null) },
                text = { Text(if (state.isSaving) "Enregistrement..." else "Terminer la séance") },
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
            com.kps.trackmyweight.ui.common.BackHeader(title = "Cardio", onBack = onBack)

            Text(
                "Enchaîne plusieurs blocs (ex : 20min elliptique + 30min tapis + corde à sauter).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.bodyWeightKg == null) {
                Text(
                    "Aucune pesée loguée — kcal estimés sur 70 kg par défaut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.blocks.isNotEmpty()) {
                val effWeight = state.bodyWeightKg ?: 70f
                val totalMin = state.blocks.sumOf { it.durationMin }
                val totalKcal = state.blocks.sumOf {
                    MetCalories.estimate(it.type, it.durationMin * 60, effWeight, it.rpe)
                }
                Text(
                    "$totalMin min · ~$totalKcal kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                state.blocks.forEachIndexed { i, b ->
                    BlockRow(index = i + 1, block = b, weightKg = effWeight, onRemove = { vm.removeBlock(i) })
                }
            }

            TextButton(
                onClick = { showAddBlock = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Ajouter un bloc")
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Text("Historique", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.recent.isEmpty()) {
                Text("Aucune séance loguée.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.recent.take(10).forEach { s -> CardioRow(s) }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showAddBlock) {
        AddBlockDialog(
            weightKg = state.bodyWeightKg ?: 70f,
            onDismiss = { showAddBlock = false },
            onConfirm = { b -> vm.addBlock(b); showAddBlock = false },
        )
    }
}

@Composable
private fun BlockRow(index: Int, block: CardioBlockUi, weightKg: Float, onRemove: () -> Unit) {
    val kcal = MetCalories.estimate(block.type, block.durationMin * 60, weightKg, block.rpe)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bloc $index · ${block.type.labelFr()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val details = buildString {
                    append("${block.durationMin} min")
                    block.distanceKm?.let { append(" · $it km") }
                    block.rpe?.let { append(" · RPE $it") }
                    append(" · ~$kcal kcal")
                }
                Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddBlockDialog(
    weightKg: Float,
    onDismiss: () -> Unit,
    onConfirm: (CardioBlockUi) -> Unit,
) {
    var type by remember { mutableStateOf(CardioType.ELLIPTICAL) }
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf("") }

    val estimatedKcal = duration.toIntOrNull()?.takeIf { it > 0 }?.let { min ->
        MetCalories.estimate(type, min * 60, weightKg, rpe.toFloatOrNull())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau bloc") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(520.dp).verticalScroll(rememberScrollState()),
            ) {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CardioType.entries.forEach { t ->
                    ChoiceTile(title = t.labelFr(), selected = type == t, onClick = { type = t })
                }
                NumericField(label = "Durée", valueText = duration, suffix = "min", onValueChange = { duration = it })
                NumericField(label = "Distance (optionnel)", valueText = distance, suffix = "km", onValueChange = { distance = it })
                NumericField(label = "RPE moyen (optionnel)", valueText = rpe, onValueChange = { rpe = it })
                estimatedKcal?.let {
                    Text("~$it kcal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Ajouter",
                enabled = (duration.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onConfirm(
                        CardioBlockUi(
                            type = type,
                            durationMin = duration.toInt(),
                            distanceKm = distance.toFloatOrNull(),
                            rpe = rpe.toFloatOrNull(),
                        )
                    )
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun CardioRow(s: CardioSessionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${s.type.labelFr()} · ${s.durationSec / 60} min", style = MaterialTheme.typography.bodyLarge)
            Text("${s.date} · ${s.caloriesEstimated.toInt()} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun CardioType.labelFr() = when (this) {
    CardioType.WALK -> "Marche"
    CardioType.RUN -> "Course"
    CardioType.LISS -> "LISS"
    CardioType.BIKE -> "Vélo"
    CardioType.ROWER -> "Rameur"
    CardioType.ELLIPTICAL -> "Elliptique"
    CardioType.JUMP_ROPE -> "Corde à sauter"
    CardioType.HIIT -> "HIIT"
    CardioType.SWIM -> "Natation"
    CardioType.BATTLE_ROPES -> "Battle ropes"
    CardioType.JUMPING_JACKS -> "Jumping jacks"
    CardioType.BURPEES -> "Burpees"
    CardioType.MOUNTAIN_CLIMBERS -> "Mountain climbers"
    CardioType.STAIR_MASTER -> "Stair master"
    CardioType.OTHER -> "Autre"
}
