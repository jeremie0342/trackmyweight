package com.kps.trackmyweight.ui.workout.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** Ce qui a été fait un jour donné. */
data class DayDetail(
    val date: LocalDate,
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val cardio: List<CardioSessionEntity> = emptyList(),
) {
    val isEmpty: Boolean get() = sessions.isEmpty() && cardio.isEmpty()

    val totalVolumeKg: Float get() = sessions.sumOf { it.totalVolumeKg.toDouble() }.toFloat()

    val totalCardioMin: Int get() = cardio.sumOf { it.durationSec } / 60
}

data class CalendarUiState(
    val month: LocalDate = LocalDate(2000, 1, 1),
    val today: LocalDate = LocalDate(2000, 1, 1),
    /** Volume par jour d'entraînement du mois affiché. */
    val trainingDays: Map<LocalDate, Float> = emptyMap(),
    val selected: DayDetail? = null,
    val isLoading: Boolean = true,
) {
    /**
     * Cases de la grille, alignées sur une semaine commençant le lundi.
     * Les positions vides avant le 1er du mois sont des nulls.
     */
    val grid: List<LocalDate?>
        get() {
            val first = LocalDate(month.year, month.monthNumber, 1)
            val blanks = first.dayOfWeek.isoDayNumber - 1
            val daysInMonth = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)).dayOfMonth
            return List(blanks) { null } + (1..daysInMonth).map { LocalDate(month.year, month.monthNumber, it) }
        }

    val trainedDayCount: Int get() = trainingDays.size

    val monthVolumeKg: Float get() = trainingDays.values.sum()
}

/**
 * Calendrier d'entraînement : quels jours ont été travaillés, et quoi.
 *
 * `getSessionsOnDate` existait depuis les fondations du projet sans jamais
 * servir — aucun écran n'avait de vue par jour.
 */
@HiltViewModel
class TrainingCalendarViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _state.update { it.copy(month = today, today = today) }
        loadMonth(today)
    }

    fun previousMonth() = shiftMonth(-1)

    fun nextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Int) {
        val target = if (delta > 0) {
            _state.value.month.plus(DatePeriod(months = delta))
        } else {
            _state.value.month.minus(DatePeriod(months = -delta))
        }
        // La selection appartient au mois qu'on quitte : la garder afficherait
        // un detail sans rapport avec la grille visible.
        _state.update { it.copy(month = target, selected = null, isLoading = true) }
        loadMonth(target)
    }

    private fun loadMonth(month: LocalDate) {
        viewModelScope.launch {
            val days = runCatching { workoutRepo.trainingDaysIn(month.year, month.monthNumber) }
                .getOrDefault(emptyMap())
            _state.update { it.copy(trainingDays = days, isLoading = false) }
        }
    }

    fun select(date: LocalDate) {
        if (_state.value.selected?.date == date) {
            _state.update { it.copy(selected = null) }
            return
        }
        viewModelScope.launch {
            val detail = DayDetail(
                date = date,
                sessions = runCatching { workoutRepo.sessionsOn(date) }.getOrDefault(emptyList()),
                cardio = runCatching { workoutRepo.cardioOn(date) }.getOrDefault(emptyList()),
            )
            _state.update { it.copy(selected = detail) }
        }
    }
}
