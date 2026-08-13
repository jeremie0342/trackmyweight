package com.kps.trackmyweight.ui.workout.rotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Un groupe de rotation avec ses templates, dans l'ordre. */
data class RotationWithTemplates(
    val group: TemplateRotationGroupEntity,
    val templates: List<WorkoutTemplateEntity>,
)

/** Édition en cours d'un groupe. Null = aucun formulaire ouvert. */
data class RotationDraft(
    val groupId: Long? = null,
    val name: String = "",
    val dayOfWeek: Int = 1,
    val templateIds: List<Long> = emptyList(),
) {
    val isValid: Boolean get() = name.isNotBlank() && templateIds.size >= 2
}

data class RotationsUiState(
    val rotations: List<RotationWithTemplates> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val draft: RotationDraft? = null,
    val errorMessage: String? = null,
)

/**
 * Rotations : « le lundi, j'alterne bras et jambes d'une semaine sur l'autre ».
 *
 * La résolution existait déjà (`nextTemplateInRotation`, une requête SQL récursive
 * dans le DAO) mais aucun écran ne permettait de créer un groupe : la
 * fonctionnalité était inatteignable.
 */
@HiltViewModel
class RotationsViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
) : ViewModel() {

    private val draft = MutableStateFlow<RotationDraft?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<RotationsUiState> = combine(
        workoutRepo.observeRotationGroups(),
        workoutRepo.observeAllRotationMembers(),
        workoutRepo.observeTemplates(),
        draft,
        error,
    ) { groups, members, templates, currentDraft, currentError ->
        val templatesById = templates.associateBy { it.id }
        RotationsUiState(
            rotations = groups.map { group ->
                RotationWithTemplates(
                    group = group,
                    templates = members
                        .filter { it.rotationGroupId == group.id }
                        .sortedBy { it.orderInRotation }
                        .mapNotNull { templatesById[it.templateId] },
                )
            },
            templates = templates,
            draft = currentDraft,
            errorMessage = currentError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RotationsUiState())

    fun startNew() {
        draft.value = RotationDraft()
    }

    fun startEdit(rotation: RotationWithTemplates) {
        draft.value = RotationDraft(
            groupId = rotation.group.id,
            name = rotation.group.name,
            dayOfWeek = rotation.group.dayOfWeek,
            templateIds = rotation.templates.map { it.id },
        )
    }

    fun cancelEdit() {
        draft.value = null
    }

    fun setName(value: String) = draft.update { it?.copy(name = value) }

    fun setDayOfWeek(value: Int) = draft.update { it?.copy(dayOfWeek = value) }

    /** Ajoute ou retire un template de la rotation, en conservant l'ordre d'ajout. */
    fun toggleTemplate(templateId: Long) = draft.update { current ->
        current ?: return@update null
        val ids = current.templateIds
        current.copy(
            templateIds = if (templateId in ids) ids - templateId else ids + templateId,
        )
    }

    fun moveTemplate(index: Int, delta: Int) = draft.update { current ->
        current ?: return@update null
        val ids = current.templateIds.toMutableList()
        val target = index + delta
        if (index !in ids.indices || target !in ids.indices) return@update current
        ids.add(target, ids.removeAt(index))
        current.copy(templateIds = ids)
    }

    fun save() {
        val current = draft.value ?: return
        if (!current.isValid) {
            error.value = "Un nom et au moins deux templates sont nécessaires."
            return
        }
        viewModelScope.launch {
            runCatching {
                workoutRepo.saveRotation(
                    groupId = current.groupId,
                    name = current.name.trim(),
                    dayOfWeek = current.dayOfWeek,
                    templateIdsInOrder = current.templateIds,
                )
            }
                .onSuccess { draft.value = null; error.value = null }
                .onFailure { e -> error.value = e.message ?: "Échec de l'enregistrement" }
        }
    }

    fun delete(groupId: Long) {
        viewModelScope.launch {
            runCatching { workoutRepo.deleteRotation(groupId) }
                .onFailure { e -> error.value = e.message }
        }
    }

    fun clearError() {
        error.value = null
    }
}
