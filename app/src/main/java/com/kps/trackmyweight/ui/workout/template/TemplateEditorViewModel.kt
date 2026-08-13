package com.kps.trackmyweight.ui.workout.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.TemplateExerciseEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class TemplateDraftExercise(
    val exercise: ExerciseEntity,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    /** Exercices consécutifs partageant ce numéro : superset. Null = isolé. */
    val supersetGroup: Int? = null,
)

data class TemplateEditorUiState(
    val templateId: Long? = null,
    val name: String = "",
    val notes: String = "",
    val draftExercises: List<TemplateDraftExercise> = emptyList(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val editingId: Long? = savedState.get<Long>("id")?.takeIf { it > 0L }

    private val _state = MutableStateFlow(TemplateEditorUiState(templateId = editingId))
    val state: StateFlow<TemplateEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val all = exerciseRepo.observeAll().first()
            val existing = editingId?.let { workoutRepo.getTemplate(it) }
            _state.update {
                if (existing != null) {
                    it.copy(
                        allExercises = all,
                        name = existing.template.name,
                        notes = existing.template.notes.orEmpty(),
                        draftExercises = existing.exercises.mapNotNull { te ->
                            val ex = all.firstOrNull { e -> e.id == te.templateExercise.exerciseId } ?: return@mapNotNull null
                            TemplateDraftExercise(
                                exercise = ex,
                                targetSets = te.templateExercise.targetSets,
                                targetRepsMin = te.templateExercise.targetRepsMin,
                                targetRepsMax = te.templateExercise.targetRepsMax,
                                supersetGroup = te.templateExercise.supersetGroup,
                            )
                        },
                    )
                } else it.copy(allExercises = all)
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }

    fun addExercise(exercise: ExerciseEntity, targetSets: Int = 3, min: Int = 8, max: Int = 12) {
        _state.update {
            it.copy(draftExercises = it.draftExercises + TemplateDraftExercise(exercise, targetSets, min, max))
        }
    }

    fun removeAt(index: Int) {
        _state.update {
            val remaining = it.draftExercises.toMutableList().apply { removeAt(index) }
            it.copy(draftExercises = renumberSupersets(remaining))
        }
    }

    /**
     * Associe ou dissocie cet exercice de celui qui le suit.
     * Même règle qu'à la préparation : les supersets sont consécutifs.
     */
    fun toggleSupersetWithNext(index: Int) {
        _state.update { s ->
            val plan = s.draftExercises
            if (index !in plan.indices || index + 1 !in plan.indices) return@update s
            val current = plan[index]
            val next = plan[index + 1]
            val updated = if (current.supersetGroup != null && current.supersetGroup == next.supersetGroup) {
                val group = current.supersetGroup
                plan.mapIndexed { i, d ->
                    if (i > index && d.supersetGroup == group) d.copy(supersetGroup = null) else d
                }
            } else {
                val group = current.supersetGroup
                    ?: ((plan.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
                plan.mapIndexed { i, d ->
                    if (i == index || i == index + 1) d.copy(supersetGroup = group) else d
                }
            }
            s.copy(draftExercises = renumberSupersets(updated))
        }
    }

    /** Voir `SessionSetupViewModel` : un groupe réduit à un exercice est dissous. */
    private fun renumberSupersets(plan: List<TemplateDraftExercise>): List<TemplateDraftExercise> {
        var nextGroup = 0
        var previousGroup: Int? = null
        var currentGroup: Int? = null
        val remapped = plan.map { draft ->
            when {
                draft.supersetGroup == null -> {
                    previousGroup = null
                    currentGroup = null
                    draft
                }
                draft.supersetGroup == previousGroup -> draft.copy(supersetGroup = currentGroup)
                else -> {
                    previousGroup = draft.supersetGroup
                    nextGroup++
                    currentGroup = nextGroup
                    draft.copy(supersetGroup = nextGroup)
                }
            }
        }
        val counts = remapped.mapNotNull { it.supersetGroup }.groupingBy { it }.eachCount()
        return remapped.map { d ->
            if (d.supersetGroup != null && counts[d.supersetGroup] == 1) d.copy(supersetGroup = null) else d
        }
    }

    fun updateExercise(index: Int, transform: (TemplateDraftExercise) -> TemplateDraftExercise) {
        _state.update { s ->
            s.copy(
                draftExercises = s.draftExercises.toMutableList().apply {
                    if (index in indices) this[index] = transform(this[index])
                },
            )
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.draftExercises.isEmpty()) {
            _state.update { it.copy(errorMessage = "Nom + au moins un exercice requis") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val now = Clock.System.now()
            val templateEntity = WorkoutTemplateEntity(
                id = s.templateId ?: 0L,
                name = s.name.trim(),
                notes = s.notes.trim().takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            )
            val exercisesEntities = s.draftExercises.map { d ->
                TemplateExerciseEntity(
                    templateId = 0L, exerciseId = d.exercise.id, orderIndex = 0,
                    targetSets = d.targetSets,
                    targetRepsMin = d.targetRepsMin,
                    targetRepsMax = d.targetRepsMax,
                    supersetGroup = d.supersetGroup,
                )
            }
            runCatching { workoutRepo.saveTemplate(templateEntity, exercisesEntities) }
                .onSuccess { id -> _state.update { it.copy(isSaving = false, savedId = id) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Erreur") } }
        }
    }
}
