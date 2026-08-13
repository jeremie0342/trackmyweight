package com.kps.trackmyweight.ui.workout.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.db.entity.MuscleGroupVolumeWeeklyEntity
import com.kps.trackmyweight.data.repository.MaxLoadSummary
import com.kps.trackmyweight.data.repository.StrengthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.kps.trackmyweight.domain.calc.VolumeVerdict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressionUiState(
    val monthlyTonnage: List<MonthlyTonnageRow> = emptyList(),
    val maxLoads: List<MaxLoadSummary> = emptyList(),
    val weeklyVolume: List<VolumeVerdict> = emptyList(),
    val volumeHistory: List<MuscleGroupVolumeWeeklyEntity> = emptyList(),
    val isLoading: Boolean = true,
) {
    /**
     * Séries totales par semaine, tous groupes confondus, du plus ancien au plus
     * récent. Donne la tendance de charge de travail globale.
     */
    val weeklySetTotals: List<Pair<String, Int>>
        get() = volumeHistory
            .groupBy { it.isoWeek }
            .map { (week, rows) -> week to rows.sumOf { it.totalSets } }
            .sortedBy { it.first }

    /** Le mois en cours est le dernier de la série (triée par ordre chronologique). */
    val currentMonth: MonthlyTonnageRow? get() = monthlyTonnage.lastOrNull()

    val previousMonth: MonthlyTonnageRow? get() = monthlyTonnage.getOrNull(monthlyTonnage.size - 2)

    /** Variation du tonnage par rapport au mois précédent, en pourcentage. */
    val monthOverMonthPercent: Float?
        get() {
            val previous = previousMonth?.volumeKg ?: return null
            val current = currentMonth?.volumeKg ?: return null
            if (previous <= 0f) return null
            return (current - previous) / previous * 100f
        }

    val totalVolumeKg: Float get() = monthlyTonnage.sumOf { it.volumeKg.toDouble() }.toFloat()

    val improving: List<MaxLoadSummary>
        get() = maxLoads.filter { (it.deltaKg ?: 0f) > 0f }.sortedByDescending { it.deltaKg }

    val stalling: List<MaxLoadSummary>
        get() = maxLoads.filter { summary ->
            // `deltaKg` est une propriété calculée : pas de smart cast possible,
            // on capture la valeur avant de la tester.
            val delta = summary.deltaKg
            delta == null || delta <= 0f
        }
}

/**
 * Où j'en suis au fil du temps : tonnage mensuel et évolution des charges
 * maximales par exercice.
 */
@HiltViewModel
class ProgressionViewModel @Inject constructor(
    private val strengthRepo: StrengthRepository,
) : ViewModel() {

    private val weeklyVolume = MutableStateFlow<List<VolumeVerdict>>(emptyList())

    init {
        // Recalculé à l'ouverture plutôt qu'à chaque série loguée : l'agrégat
        // ne sert qu'ici, et le recalculer en séance coûterait pour rien.
        viewModelScope.launch {
            runCatching { strengthRepo.refreshWeeklyVolume() }
                .onSuccess { weeklyVolume.value = it }
        }
    }

    val state: StateFlow<ProgressionUiState> = combine(
        strengthRepo.observeMonthlyTonnage(months = 12),
        strengthRepo.observeMaxLoadSummaries(windowDays = 90),
        strengthRepo.observeWeeklyVolumeHistory(weeks = 8),
        weeklyVolume,
    ) { tonnage, maxLoads, history, volume ->
        ProgressionUiState(
            monthlyTonnage = tonnage,
            maxLoads = maxLoads,
            volumeHistory = history,
            weeklyVolume = volume,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressionUiState())
}
