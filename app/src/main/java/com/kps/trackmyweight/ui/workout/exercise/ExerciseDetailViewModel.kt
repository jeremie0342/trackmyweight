package com.kps.trackmyweight.ui.workout.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.ExerciseMaxLoadEntity
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.StrengthRepository
import com.kps.trackmyweight.data.repository.WorkoutRepository
import com.kps.trackmyweight.domain.calc.WorkingLoad
import com.kps.trackmyweight.domain.calc.WorkingLoadRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val equipment: List<EquipmentEntity> = emptyList(),
    val substitutes: List<ExerciseEntity> = emptyList(),
    val lastSet: PerformedSetEntity? = null,
    val maxLoadHistory: List<ExerciseMaxLoadEntity> = emptyList(),
    val monthlyTonnage: List<MonthlyTonnageRow> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val currentMax: ExerciseMaxLoadEntity? get() = maxLoadHistory.lastOrNull()

    /** Première mesure connue, pour situer la progression totale. */
    val firstMax: ExerciseMaxLoadEntity? get() = maxLoadHistory.firstOrNull()

    val progressionPercent: Float?
        get() = WorkingLoad.progressionPercent(firstMax?.oneRmKg, currentMax?.oneRmKg)

    /** L'exercice se charge-t-il sur une barre ? Détermine l'affichage des disques. */
    private val isBarbell: Boolean
        get() = equipment.any { it.key.startsWith("barbell_") }

    val workingLoads: List<WorkingLoadRow>
        get() = currentMax?.let { WorkingLoad.table(it.oneRmKg, onBar = isBarbell) }.orEmpty()

    val maxCurve: List<Float> get() = maxLoadHistory.map { it.oneRmKg }
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    private val strengthRepo: StrengthRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val exerciseId: Long = savedState.get<Long>("exerciseId") ?: 0L

    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    exercise = exerciseRepo.getById(exerciseId),
                    equipment = exerciseRepo.getEquipmentFor(exerciseId),
                    substitutes = exerciseRepo.getSubstitutes(exerciseId),
                    lastSet = workoutRepo.lastSetForExercise(exerciseId),
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            strengthRepo.observeMaxLoadHistory(exerciseId).collect { history ->
                _state.update { it.copy(maxLoadHistory = history) }
            }
        }
        viewModelScope.launch {
            strengthRepo.observeMonthlyTonnageForExercise(exerciseId, months = 6).collect { rows ->
                _state.update { it.copy(monthlyTonnage = rows) }
            }
        }
    }

    /** Enregistre un vrai test de force : une répétition à cette charge. */
    fun recordTestedMax(weightKg: Float) = launchCatching {
        strengthRepo.recordTestedMax(exerciseId, weightKg)
    }

    /** Enregistre une valeur connue par ailleurs (coach, ancien carnet). */
    fun recordDeclaredMax(oneRmKg: Float) = launchCatching {
        strengthRepo.recordDeclaredMax(exerciseId, oneRmKg)
    }

    /** Estime le max depuis une série réalisée. */
    fun recordEstimatedMax(weightKg: Float, reps: Int) = launchCatching {
        strengthRepo.recordEstimatedMax(exerciseId, weightKg, reps)
    }

    fun deleteMaxLoad(id: Long) = launchCatching { strengthRepo.deleteMaxLoad(id) }

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }
}
