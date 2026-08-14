package com.kps.trackmyweight.ui.workout.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.ProgramDayEntity
import com.kps.trackmyweight.data.db.entity.ProgramEntity
import com.kps.trackmyweight.data.db.entity.TemplateRotationGroupEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.repository.WorkoutRepository
import com.kps.trackmyweight.domain.calc.MesocycleProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** Ce qu'un jour de la semaine porte, dans le formulaire. */
sealed interface DaySlot {
    data object Rest : DaySlot
    data object Empty : DaySlot
    data class Template(val templateId: Long) : DaySlot
    data class Rotation(val rotationGroupId: Long) : DaySlot
}

data class ProgramDraft(
    val programId: Long? = null,
    val name: String = "",
    val mesocycleWeeks: String = "5",
    /** Indexé par jour ISO, 1 = lundi. */
    val days: Map<Int, DaySlot> = (1..7).associateWith { DaySlot.Empty },
    val makeActive: Boolean = true,
) {
    val weeksValue: Int? get() = mesocycleWeeks.toIntOrNull()?.takeIf { it in 1..24 }

    val hasTraining: Boolean
        get() = days.values.any { it is DaySlot.Template || it is DaySlot.Rotation }

    val isValid: Boolean get() = name.isNotBlank() && weeksValue != null && hasTraining
}

data class ProgramWithDays(
    val program: ProgramEntity,
    val days: List<ProgramDayEntity>,
) {
    fun progressAt(today: LocalDate): MesocycleProgress = MesocycleProgress.of(program, today)

    val trainingDayCount: Int get() = days.count { !it.isRest }
}

data class ProgramsUiState(
    val programs: List<ProgramWithDays> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val rotations: List<TemplateRotationGroupEntity> = emptyList(),
    val draft: ProgramDraft? = null,
    val today: LocalDate = LocalDate(2000, 1, 1),
    val errorMessage: String? = null,
) {
    val activeProgram: ProgramWithDays? get() = programs.firstOrNull { it.program.isActive }
}

/**
 * Programmes d'entraînement : le planning de la semaine, répété sur un bloc.
 *
 * Les tables `program` et `program_day` existaient depuis le début sans aucun
 * écran pour les créer ni les lire.
 */
@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
) : ViewModel() {

    private val draft = MutableStateFlow<ProgramDraft?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ProgramsUiState> = combine(
        workoutRepo.observePrograms(),
        workoutRepo.observeAllProgramDays(),
        workoutRepo.observeTemplates(),
        workoutRepo.observeRotationGroups(),
        combine(draft, error) { d, e -> d to e },
    ) { programs, allDays, templates, rotations, (currentDraft, currentError) ->
        ProgramsUiState(
            programs = programs.map { program ->
                ProgramWithDays(program, allDays.filter { it.programId == program.id })
            },
            templates = templates,
            rotations = rotations,
            draft = currentDraft,
            today = todayLocal(),
            errorMessage = currentError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgramsUiState())

    fun startNew() {
        draft.value = ProgramDraft()
    }

    fun startEdit(item: ProgramWithDays) {
        val slots = (1..7).associateWith { day ->
            val entry = item.days.firstOrNull { it.dayOfWeek == day }
            when {
                entry == null -> DaySlot.Empty
                entry.isRest -> DaySlot.Rest
                entry.templateId != null -> DaySlot.Template(entry.templateId)
                entry.rotationGroupId != null -> DaySlot.Rotation(entry.rotationGroupId)
                else -> DaySlot.Empty
            }
        }
        draft.value = ProgramDraft(
            programId = item.program.id,
            name = item.program.name,
            mesocycleWeeks = item.program.mesocycleWeeks.toString(),
            days = slots,
            makeActive = item.program.isActive,
        )
    }

    fun cancelEdit() { draft.value = null }

    fun setName(value: String) = draft.update { it?.copy(name = value) }

    fun setWeeks(value: String) = draft.update { it?.copy(mesocycleWeeks = value) }

    fun setDay(day: Int, slot: DaySlot) = draft.update { current ->
        current?.copy(days = current.days + (day to slot))
    }

    fun toggleMakeActive() = draft.update { it?.copy(makeActive = !it.makeActive) }

    fun save() {
        val current = draft.value ?: return
        if (!current.isValid) {
            error.value = "Il faut un nom, une durée entre 1 et 24 semaines, et au moins un jour d'entraînement."
            return
        }
        viewModelScope.launch {
            runCatching {
                workoutRepo.saveProgram(
                    programId = current.programId,
                    name = current.name.trim(),
                    // Le bloc démarre aujourd'hui à la création, et garde sa date
                    // d'origine en édition : la recaler ferait repartir le
                    // compteur de semaines à zéro sans que l'utilisateur l'ait voulu.
                    startDate = current.programId
                        ?.let { id -> state.value.programs.firstOrNull { it.program.id == id }?.program?.startDate }
                        ?: todayLocal(),
                    mesocycleWeeks = current.weeksValue ?: DEFAULT_WEEKS,
                    days = current.days.mapNotNull { (day, slot) -> slot.toEntity(day) },
                    makeActive = current.makeActive,
                )
            }
                .onSuccess { draft.value = null; error.value = null }
                .onFailure { e -> error.value = e.message ?: "Échec de l'enregistrement" }
        }
    }

    fun activate(programId: Long) {
        viewModelScope.launch {
            runCatching { workoutRepo.activateProgram(programId) }
                .onFailure { e -> error.value = e.message }
        }
    }

    fun deactivateAll() {
        viewModelScope.launch { runCatching { workoutRepo.deactivateAllPrograms() } }
    }

    fun delete(programId: Long) {
        viewModelScope.launch {
            runCatching { workoutRepo.deleteProgram(programId) }
                .onFailure { e -> error.value = e.message }
        }
    }

    fun clearError() { error.value = null }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun DaySlot.toEntity(day: Int): ProgramDayEntity? = when (this) {
        DaySlot.Empty -> null
        DaySlot.Rest -> ProgramDayEntity(programId = 0, dayOfWeek = day, isRest = true)
        is DaySlot.Template -> ProgramDayEntity(programId = 0, dayOfWeek = day, templateId = templateId)
        is DaySlot.Rotation -> ProgramDayEntity(programId = 0, dayOfWeek = day, rotationGroupId = rotationGroupId)
    }

    private companion object {
        const val DEFAULT_WEEKS = 5
    }
}
