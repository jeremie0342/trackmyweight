package com.kps.trackmyweight.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.repository.ExerciseRepository
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
    val activeSession: WorkoutSessionEntity? = null,
    /** Template proposé par la rotation calée sur le jour courant. */
    val rotationSuggestion: WorkoutTemplateEntity? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class WorkoutOverviewViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
) : ViewModel() {

    private val rotationSuggestion = MutableStateFlow<WorkoutTemplateEntity?>(null)

    val state: StateFlow<WorkoutOverviewUiState> = combine(
        workoutRepo.observeTemplates(),
        workoutRepo.observeFinishedSessions(10),
        workoutRepo.observeRecentPrs(10),
        workoutRepo.observeActiveSession(),
        rotationSuggestion,
    ) { templates, sessions, prs, active, suggestion ->
        WorkoutOverviewUiState(
            templates = templates,
            recentSessions = sessions,
            recentPrs = prs,
            activeSession = active,
            rotationSuggestion = suggestion,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutOverviewUiState())

    init {
        viewModelScope.launch {
            exerciseRepo.syncCatalog()
            // Rattrape les séances laissées ouvertes par l'ancien comportement
            // (quitter l'écran ne clôturait rien). Sans ce ménage, le bandeau de
            // reprise proposerait indéfiniment une séance vieille de plusieurs jours.
            workoutRepo.closeStaleSessions()
            rotationSuggestion.value = workoutRepo.todaysRotationSuggestion()
        }
    }

    /** Renvoie le texte formaté à envoyer au coach pour une session. */
    suspend fun coachTextFor(sessionId: Long): String =
        workoutRepo.formatSessionForCoach(sessionId)
}
