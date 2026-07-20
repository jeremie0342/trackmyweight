package com.kps.trackmyweight.ui.workout.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.data.repository.WorkoutRepository
import com.kps.trackmyweight.domain.calc.RestTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseCard(
    val performed: PerformedExerciseEntity,
    val exercise: ExerciseEntity?,
    val sets: List<PerformedSetEntity>,
    val lastSetPreview: PerformedSetEntity?,
)

data class SessionActiveUiState(
    val session: WorkoutSessionEntity? = null,
    val exercises: List<ExerciseCard> = emptyList(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    val warmup: CardioSessionEntity? = null,
    val bodyWeightKg: Float? = null,
    val restRemainingSec: Int = 0,
    val restTotalSec: Int = 0,
    val isFinishing: Boolean = false,
    val isDone: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SessionActiveViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val weightRepo: WeightRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedState.get<Long>("id") ?: 0L

    private val _state = MutableStateFlow(SessionActiveUiState())
    val state: StateFlow<SessionActiveUiState> = _state.asStateFlow()

    private var restJob: Job? = null

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val session = workoutRepo.getSession(sessionId) ?: workoutRepo.getActiveOrLatestSession()
            val perfList = workoutRepo.performedExercisesForSession(sessionId)
            val cards = perfList.map { pe ->
                val ex = exerciseRepo.getById(pe.exerciseId)
                val sets = workoutRepo.setsForPerformedExercise(pe.id)
                val last = workoutRepo.lastSetForExercise(pe.exerciseId)
                ExerciseCard(pe, ex, sets, last)
            }
            val all = exerciseRepo.observeAll().first()
            val warmup = session?.warmupCardioSessionId?.let { workoutRepo.getCardio(it) }
            val weight = weightRepo.observeLast().first()?.weightKg
            _state.update {
                it.copy(
                    session = session,
                    exercises = cards,
                    allExercises = all,
                    warmup = warmup,
                    bodyWeightKg = weight,
                )
            }
        }
    }

    fun logWarmup(type: CardioType, durationMin: Int, rpe: Float?) {
        viewModelScope.launch {
            val body = _state.value.bodyWeightKg ?: 70f
            runCatching { workoutRepo.logWarmupCardio(sessionId, type, durationMin, body, rpe) }
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            val order = _state.value.exercises.size
            workoutRepo.getOrCreatePerformedExercise(sessionId, exerciseId, order)
            refresh()
        }
    }

    fun logSet(
        performed: PerformedExerciseEntity,
        weightKg: Float,
        reps: Int,
        rpe: Float?,
        type: SetType = SetType.WORKING,
    ) {
        viewModelScope.launch {
            val setNumber = (workoutRepo.setsForPerformedExercise(performed.id).size) + 1
            workoutRepo.logSet(
                sessionId = sessionId,
                exerciseId = performed.exerciseId,
                performedExerciseId = performed.id,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                type = type,
                restBeforeSec = null,
            )
            // Démarrer le chrono repos en fonction de l'exercice
            val mechanics = exerciseRepo.getById(performed.exerciseId)?.mechanics
            val restSec = mechanics?.let { RestTime.defaultSecFor(it) } ?: 120
            startRest(restSec)
            refresh()
        }
    }

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _state.update { it.copy(restTotalSec = seconds, restRemainingSec = seconds) }
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _state.update { it.copy(restRemainingSec = remaining) }
            }
        }
    }

    fun cancelRest() {
        restJob?.cancel()
        _state.update { it.copy(restRemainingSec = 0, restTotalSec = 0) }
    }

    fun finishSession(sessionRpe: Float?, notes: String?) {
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            runCatching { workoutRepo.endSession(sessionId, sessionRpe, notes) }
                .onSuccess { _state.update { it.copy(isFinishing = false, isDone = true) } }
                .onFailure { e -> _state.update { it.copy(isFinishing = false, errorMessage = e.message) } }
        }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
