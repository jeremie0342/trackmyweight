package com.kps.trackmyweight.ui.workout.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.GymEntity
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.PlannedExercise
import com.kps.trackmyweight.data.repository.WorkoutRepository
import com.kps.trackmyweight.ui.workout.exercise.CustomExerciseDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSetupUiState(
    val templateId: Long? = null,
    val templateName: String? = null,
    val gyms: List<GymEntity> = emptyList(),
    val selectedGymId: Long? = null,
    val plan: List<PlannedExercise> = emptyList(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    /** Exercices réalisables avec l'équipement de la salle sélectionnée. */
    val gymExerciseIds: Set<Long> = emptySet(),
    val onlyMyGym: Boolean = true,
    /** Référentiel d'équipement, pour la création d'un exercice personnalisé. */
    val allEquipment: List<EquipmentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isStarting: Boolean = false,
    val startedSessionId: Long? = null,
    val errorMessage: String? = null,
) {
    val canStart: Boolean get() = !isStarting && plan.isNotEmpty()
    val title: String get() = templateName ?: "Séance libre"

    /**
     * Exercices proposés au sélecteur.
     *
     * Le filtre est ignoré tant que la salle n'a aucun équipement renseigné :
     * mieux vaut proposer tout le catalogue qu'une liste vide dans laquelle
     * l'utilisateur serait bloqué.
     */
    val pickerExercises: List<ExerciseEntity>
        get() = if (onlyMyGym && gymExerciseIds.isNotEmpty()) {
            allExercises.filter { it.id in gymExerciseIds }
        } else {
            allExercises
        }

    val canFilterByGym: Boolean get() = gymExerciseIds.isNotEmpty()

    val hiddenByGymCount: Int
        get() = if (canFilterByGym) allExercises.size - gymExerciseIds.size else 0
}

/**
 * Écran de préparation : on compose la séance, puis on la lance.
 *
 * Rien n'est écrit en base tant que [start] n'a pas été appelé. Auparavant, un
 * simple appui sur un template créait immédiatement la séance et naviguait
 * dessus — impossible de la configurer, et le moindre appui accidentel laissait
 * une séance vide dans l'historique.
 */
@HiltViewModel
class SessionSetupViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val gymRepo: GymRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    /** 0 signifie « séance libre », sans template de départ. */
    private val templateId: Long? = savedState.get<Long>("templateId")?.takeIf { it > 0L }

    private val _state = MutableStateFlow(SessionSetupUiState())
    val state: StateFlow<SessionSetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepo.syncCatalog()
            val gyms = gymRepo.observeGyms().first()
            val defaultGym = gymRepo.getDefaultGym()
            val template = templateId?.let { workoutRepo.getTemplate(it) }
            _state.update {
                it.copy(
                    templateId = templateId,
                    templateName = template?.template?.name,
                    gyms = gyms,
                    selectedGymId = defaultGym?.id ?: gyms.firstOrNull()?.id,
                    plan = templateId?.let { id -> workoutRepo.planFromTemplate(id) }.orEmpty(),
                    allExercises = exerciseRepo.observeAll().first(),
                    allEquipment = gymRepo.observeAllEquipment().first(),
                    isLoading = false,
                )
            }
            loadGymFilter(_state.value.selectedGymId)
        }
    }

    fun selectGym(gymId: Long) {
        _state.update { it.copy(selectedGymId = gymId) }
        loadGymFilter(gymId)
    }

    fun toggleOnlyMyGym() = _state.update { it.copy(onlyMyGym = !it.onlyMyGym) }

    /** Recalcule l'ensemble des exercices réalisables dans la salle donnée. */
    private fun loadGymFilter(gymId: Long?) {
        viewModelScope.launch {
            val ids = gymId
                ?.let { exerciseRepo.observeAvailableInGym(it).first().map(ExerciseEntity::id).toSet() }
                .orEmpty()
            _state.update { it.copy(gymExerciseIds = ids) }
        }
    }

    fun addExercise(exercise: ExerciseEntity) = _state.update { s ->
        s.copy(
            plan = s.plan + PlannedExercise(
                exerciseId = exercise.id,
                name = exercise.name,
                targetSets = DEFAULT_TARGET_SETS,
                restSecOverride = exercise.defaultRestSec,
            ),
        )
    }

    /**
     * Crée un exercice hors catalogue et l'ajoute directement au plan : le
     * geste part d'une recherche infructueuse, on ne renvoie pas l'utilisateur
     * vers un autre écran.
     */
    fun createCustomExercise(draft: CustomExerciseDraft) {
        viewModelScope.launch {
            runCatching {
                val id = exerciseRepo.createCustomExercise(
                    name = draft.name,
                    primaryMuscle = draft.primaryMuscle,
                    mechanics = draft.mechanics,
                    force = draft.force,
                    equipmentIds = draft.equipmentIds,
                )
                exerciseRepo.getById(id)
            }
                .onSuccess { created ->
                    if (created != null) {
                        _state.update { it.copy(allExercises = it.allExercises + created) }
                        addExercise(created)
                    }
                }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun removeAt(index: Int) = _state.update { s ->
        s.copy(plan = renumberSupersets(s.plan.filterIndexed { i, _ -> i != index }))
    }

    fun moveUp(index: Int) = swap(index, index - 1)

    fun moveDown(index: Int) = swap(index, index + 1)

    private fun swap(from: Int, to: Int) = _state.update { s ->
        if (from !in s.plan.indices || to !in s.plan.indices) return@update s
        val moved = s.plan.toMutableList().apply { add(to, removeAt(from)) }
        s.copy(plan = renumberSupersets(moved))
    }

    /**
     * Associe ou dissocie cet exercice de celui qui le suit, formant un superset.
     *
     * Les supersets se composent d'exercices **consécutifs** : c'est ainsi qu'on
     * les exécute, et ça évite d'avoir à gérer des groupes discontinus dans
     * l'affichage comme dans la logique de repos.
     */
    fun toggleSupersetWithNext(index: Int) = _state.update { s ->
        val plan = s.plan
        if (index !in plan.indices || index + 1 !in plan.indices) return@update s
        val current = plan[index]
        val next = plan[index + 1]

        val updated = if (current.supersetGroup != null && current.supersetGroup == next.supersetGroup) {
            // Dissociation : le suivant et toute sa suite quittent le groupe.
            val group = current.supersetGroup
            plan.mapIndexed { i, p ->
                if (i > index && p.supersetGroup == group) p.copy(supersetGroup = null) else p
            }
        } else {
            val group = current.supersetGroup ?: ((plan.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
            plan.mapIndexed { i, p ->
                when (i) {
                    index -> p.copy(supersetGroup = group)
                    index + 1 -> p.copy(supersetGroup = group)
                    else -> p
                }
            }
        }
        s.copy(plan = renumberSupersets(updated))
    }

    /**
     * Renumérote les groupes après un déplacement ou une suppression.
     *
     * Un groupe qui ne contient plus qu'un exercice n'est plus un superset :
     * il est dissous, sinon on afficherait « Superset A » sur un exercice seul.
     */
    private fun renumberSupersets(plan: List<PlannedExercise>): List<PlannedExercise> {
        var nextGroup = 0
        var previousGroup: Int? = null
        var currentGroup: Int? = null
        val remapped = plan.map { planned ->
            when {
                planned.supersetGroup == null -> {
                    previousGroup = null
                    currentGroup = null
                    planned
                }
                planned.supersetGroup == previousGroup -> planned.copy(supersetGroup = currentGroup)
                else -> {
                    previousGroup = planned.supersetGroup
                    nextGroup++
                    currentGroup = nextGroup
                    planned.copy(supersetGroup = nextGroup)
                }
            }
        }
        val counts = remapped.mapNotNull { it.supersetGroup }.groupingBy { it }.eachCount()
        return remapped.map { p ->
            if (p.supersetGroup != null && counts[p.supersetGroup] == 1) p.copy(supersetGroup = null) else p
        }
    }

    fun updateAt(index: Int, transform: (PlannedExercise) -> PlannedExercise) = _state.update { s ->
        s.copy(plan = s.plan.mapIndexed { i, p -> if (i == index) transform(p) else p })
    }

    /** Crée la séance et publie son id — la navigation s'y accroche. */
    fun start() {
        val current = _state.value
        if (!current.canStart) return
        _state.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                workoutRepo.startSession(
                    templateId = current.templateId,
                    gymId = current.selectedGymId,
                    plan = current.plan,
                )
            }
                .onSuccess { id -> _state.update { it.copy(isStarting = false, startedSessionId = id) } }
                .onFailure { e ->
                    _state.update { it.copy(isStarting = false, errorMessage = e.message ?: "Échec du lancement") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    private companion object {
        const val DEFAULT_TARGET_SETS = 3
    }
}
