package com.kps.trackmyweight.ui.workout.cardio

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.repository.CardioRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.NumericField
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardioLogUiState(
    val recent: List<CardioSessionEntity> = emptyList(),
    val type: CardioType = CardioType.RUN,
    val durationMin: String = "",
    val distanceKm: String = "",
    val rpe: String = "",
    val notes: String = "",
    val bodyWeightKg: Float? = null,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
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

    fun setType(t: CardioType) = _draft.update { it.copy(type = t) }
    fun setDurationMin(v: String) = _draft.update { it.copy(durationMin = v) }
    fun setDistanceKm(v: String) = _draft.update { it.copy(distanceKm = v) }
    fun setRpe(v: String) = _draft.update { it.copy(rpe = v) }
    fun setNotes(v: String) = _draft.update { it.copy(notes = v) }

    fun save() {
        val s = _draft.value
        val minutes = s.durationMin.toIntOrNull() ?: return
        val body = s.bodyWeightKg ?: return
        _draft.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            cardioRepo.log(
                type = s.type,
                durationSec = minutes * 60,
                bodyWeightKg = body,
                distanceM = s.distanceKm.toFloatOrNull()?.let { it * 1000f },
                avgRpe = s.rpe.toFloatOrNull(),
                notes = s.notes.takeIf { it.isNotBlank() },
            )
            _draft.update {
                CardioLogUiState(
                    type = s.type, bodyWeightKg = body, recent = it.recent, savedOk = true,
                )
            }
        }
    }

    fun consumeSaved() = _draft.update { it.copy(savedOk = false) }
}

@Composable
fun CardioLogScreen(
    onDone: () -> Unit,
    vm: CardioLogViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.savedOk) { if (state.savedOk) { vm.consumeSaved(); onDone() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::save,
                icon = { Icon(Icons.Outlined.Check, null) },
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
            Text("Cardio", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)

            Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    CardioType.WALK to "Marche",
                    CardioType.RUN to "Course",
                    CardioType.LISS to "LISS (basse intensité)",
                    CardioType.BIKE to "Vélo",
                    CardioType.ROWER to "Rameur",
                    CardioType.ELLIPTICAL to "Elliptique",
                    CardioType.JUMP_ROPE to "Corde à sauter",
                    CardioType.HIIT to "HIIT",
                    CardioType.SWIM to "Natation",
                ).forEach { (t, label) ->
                    ChoiceTile(title = label, selected = state.type == t, onClick = { vm.setType(t) })
                }
            }

            NumericField(label = "Durée", valueText = state.durationMin, suffix = "min", onValueChange = vm::setDurationMin)
            NumericField(label = "Distance (optionnel)", valueText = state.distanceKm, suffix = "km", onValueChange = vm::setDistanceKm)
            NumericField(label = "RPE moyen (1-10, optionnel)", valueText = state.rpe, onValueChange = vm::setRpe)

            if (state.bodyWeightKg == null) {
                Text(
                    "Log au moins une pesée pour estimer les calories.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                val kcal = state.durationMin.toIntOrNull()?.let { min ->
                    com.kps.trackmyweight.domain.calc.MetCalories.estimate(
                        type = state.type, durationSec = min * 60,
                        bodyWeightKg = state.bodyWeightKg!!,
                        avgRpe = state.rpe.toFloatOrNull(),
                    )
                }
                if (kcal != null && kcal > 0) {
                    Text(
                        "Estimation : ~$kcal kcal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

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
            Text("${s.type.name}  ${s.durationSec / 60} min", style = MaterialTheme.typography.bodyLarge)
            Text("${s.date} · ${s.caloriesEstimated.toInt()} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
