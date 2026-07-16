package com.kps.trackmyweight.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.BmiCalculator
import com.kps.trackmyweight.domain.calc.BmiCategory
import com.kps.trackmyweight.domain.calc.DatedValue
import com.kps.trackmyweight.domain.calc.SmoothedPoint
import com.kps.trackmyweight.domain.calc.StagnationDetector
import com.kps.trackmyweight.domain.calc.TimeSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class WeightUiState(
    val entries: List<WeightEntryEntity> = emptyList(),
    val smoothed: List<SmoothedPoint> = emptyList(),
    val lastWeightKg: Float? = null,
    val previousWeightKg: Float? = null,
    val bmi: Float? = null,
    val bmiCategory: BmiCategory? = null,
    val projectedAtTargetKg: Float? = null,
    val etaForTarget: LocalDate? = null,
    val stagnating: Boolean = false,
    val stagnationNetChangeKg: Float = 0f,
    val goal: GoalEntity? = null,
    val profile: UserProfileEntity? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val weightRepo: WeightRepository,
    userRepo: UserProfileRepository,
    goalRepo: GoalRepository,
) : ViewModel() {

    private data class SaveState(val isSaving: Boolean = false, val error: String? = null)
    private val _saveState = MutableStateFlow(SaveState())

    val state: StateFlow<WeightUiState> = combine(
        weightRepo.observeRecent(180),
        userRepo.observe(),
        goalRepo.observeActive(),
        _saveState,
    ) { entries, profile, goal, save ->
        buildState(entries, profile, goal, save.isSaving, save.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUiState())

    private fun buildState(
        entries: List<WeightEntryEntity>,
        profile: UserProfileEntity?,
        goal: GoalEntity?,
        isSaving: Boolean,
        errorMessage: String?,
    ): WeightUiState {
        val sorted = entries.sortedBy { it.date }
        val points = sorted.map { DatedValue(it.date, it.weightKg) }
        val smoothed = TimeSeries.trailingMovingAverage(points, windowDays = 7)
        val last = sorted.lastOrNull()
        val prev = sorted.dropLast(1).lastOrNull()
        val bmi = if (last != null && profile != null) BmiCalculator.compute(last.weightKg, profile.heightCm) else null

        val targetKg = goal?.targetWeightKg
        val targetDate = goal?.targetDate
        val projected = if (targetDate != null && sorted.size >= 2) TimeSeries.projectAt(points, targetDate) else null
        val eta = if (targetKg != null && sorted.size >= 2) TimeSeries.etaFor(points, targetKg) else null

        val stagVerdict = StagnationDetector.detect(points, windowDays = 14, thresholdAbs = 0.3f, today = todayLocal())

        return WeightUiState(
            entries = sorted,
            smoothed = smoothed,
            lastWeightKg = last?.weightKg,
            previousWeightKg = prev?.weightKg,
            bmi = bmi,
            bmiCategory = bmi?.let(BmiCalculator::categorize),
            projectedAtTargetKg = projected,
            etaForTarget = eta,
            stagnating = stagVerdict.isStagnating,
            stagnationNetChangeKg = stagVerdict.netChange,
            goal = goal,
            profile = profile,
            isSaving = isSaving,
            errorMessage = errorMessage,
        )
    }

    fun logWeight(weightKg: Float, date: LocalDate = todayLocal()) {
        if (weightKg <= 0f || weightKg > 400f) {
            _saveState.value = SaveState(isSaving = false, error = "Poids invalide")
            return
        }
        _saveState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching { weightRepo.log(date, weightKg) }
                .onSuccess { _saveState.value = SaveState() }
                .onFailure { _saveState.value = SaveState(error = it.message ?: "Erreur d'enregistrement") }
        }
    }

    fun softDelete(id: Long) {
        viewModelScope.launch { weightRepo.softDelete(id) }
    }

    fun clearError() {
        _saveState.update { it.copy(error = null) }
    }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
