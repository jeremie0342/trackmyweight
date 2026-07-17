package com.kps.trackmyweight.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutOverviewUiState(
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val recentPrs: List<PersonalRecordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val startingSessionId: Long? = null,
)

@HiltViewModel
class WorkoutOverviewViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val gymRepo: GymRepository,
) : ViewModel() {

    private val _startingSessionId = MutableStateFlow<Long?>(null)

    val state: StateFlow<WorkoutOverviewUiState> = combine(
        workoutRepo.observeTemplates(),
        workoutRepo.observeRecentSessions(10),
        workoutRepo.observeRecentPrs(10),
        _startingSessionId,
    ) { templates, sessions, prs, starting ->
        WorkoutOverviewUiState(
            templates = templates,
            recentSessions = sessions,
            recentPrs = prs,
            isLoading = false,
            startingSessionId = starting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutOverviewUiState())

    init {
        viewModelScope.launch { exerciseRepo.seedIfEmpty() }
    }

    /** Démarre une nouvelle séance basée sur un template (ou libre si null). */
    fun startSession(templateId: Long?, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val gymId = gymRepo.getDefaultGym()?.id
            val id = workoutRepo.startSession(templateId, gymId)
            _startingSessionId.value = id
            onStarted(id)
        }
    }

    fun consumeStartingId() { _startingSessionId.value = null }

    /** Renvoie le texte formaté à envoyer au coach pour une session. */
    suspend fun coachTextFor(sessionId: Long): String =
        workoutRepo.formatSessionForCoach(sessionId)
}
