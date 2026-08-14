package com.kps.trackmyweight.ui.workout.pain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.PainContextRow
import com.kps.trackmyweight.data.db.entity.PainHotspotRow
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PainHistoryUiState(
    val hotspots: List<PainHotspotRow> = emptyList(),
    val entries: List<PainLogEntity> = emptyList(),
    /** Exercices associés, par zone. Chargé à la demande quand on déplie une zone. */
    val contextByArea: Map<PainArea, List<PainContextRow>> = emptyMap(),
    val expandedArea: PainArea? = null,
    val showLogDialog: Boolean = false,
) {
    val hasHistory: Boolean get() = entries.isNotEmpty()
}

/**
 * Historique des douleurs et zones récurrentes.
 *
 * `pain_log` et son enum de zones existaient depuis le début sans aucun écran
 * pour écrire dedans ni les lire.
 */
@HiltViewModel
class PainHistoryViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
) : ViewModel() {

    private val contextByArea = MutableStateFlow<Map<PainArea, List<PainContextRow>>>(emptyMap())
    private val expandedArea = MutableStateFlow<PainArea?>(null)
    private val showLogDialog = MutableStateFlow(false)

    val state: StateFlow<PainHistoryUiState> = combine(
        workoutRepo.observePainHotspots(days = 90),
        workoutRepo.observeRecentPain(limit = 60),
        contextByArea,
        expandedArea,
        showLogDialog,
    ) { hotspots, entries, contexts, expanded, dialog ->
        PainHistoryUiState(
            hotspots = hotspots,
            entries = entries,
            contextByArea = contexts,
            expandedArea = expanded,
            showLogDialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PainHistoryUiState())

    /**
     * Déplie une zone et charge les exercices associés.
     *
     * Chargé à la demande plutôt que pour toutes les zones d'un coup : une
     * requête par zone au chargement serait du gaspillage, l'utilisateur n'en
     * consulte qu'une ou deux.
     */
    fun toggleArea(area: PainArea) {
        if (expandedArea.value == area) {
            expandedArea.value = null
            return
        }
        expandedArea.value = area
        if (contextByArea.value.containsKey(area)) return
        viewModelScope.launch {
            val context = runCatching { workoutRepo.painContextFor(area) }.getOrDefault(emptyList())
            contextByArea.value = contextByArea.value + (area to context)
        }
    }

    fun openLogDialog() { showLogDialog.value = true }

    fun dismissLogDialog() { showLogDialog.value = false }

    fun logPain(draft: PainDraft) {
        viewModelScope.launch {
            runCatching {
                workoutRepo.logPain(
                    area = draft.area,
                    intensity = draft.intensity,
                    notes = draft.notes,
                )
            }
            showLogDialog.value = false
            // Le contexte devient obsolete des qu'une entree s'ajoute.
            contextByArea.value = emptyMap()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { workoutRepo.deletePain(id) }
            contextByArea.value = emptyMap()
        }
    }
}
