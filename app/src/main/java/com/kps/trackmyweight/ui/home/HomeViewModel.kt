package com.kps.trackmyweight.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.DailyLogEntity
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.HabitCompletionEntity
import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity
import com.kps.trackmyweight.data.db.entity.SleepEntryEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.HabitRepository
import com.kps.trackmyweight.data.repository.NutritionRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.ReadinessInputs
import com.kps.trackmyweight.domain.calc.ReadinessLevel
import com.kps.trackmyweight.domain.calc.ReadinessScore
import com.kps.trackmyweight.domain.calc.ReadinessVerdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val date: LocalDate = LocalDate(2000, 1, 1),
    val profile: UserProfileEntity? = null,
    val goal: GoalEntity? = null,
    val lastWeight: WeightEntryEntity? = null,
    val dailyLog: DailyLogEntity? = null,
    val readiness: ReadinessVerdict? = null,
    val habits: List<HabitDefinitionEntity> = emptyList(),
    val completions: List<HabitCompletionEntity> = emptyList(),
    val sleepEntry: SleepEntryEntity? = null,
    val waterMl: Int = 0,
    val phase: DietPhaseEntity? = null,
    val kcalConsumed: Float = 0f,
    val proteinConsumed: Float = 0f,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepo: UserProfileRepository,
    private val goalRepo: GoalRepository,
    private val weightRepo: WeightRepository,
    private val habitRepo: HabitRepository,
    private val nutritionRepo: NutritionRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(todayLocal())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    init {
        viewModelScope.launch {
            habitRepo.seedIfEmpty()
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        listOf(
            _date,
            userRepo.observe(),
            goalRepo.observeActive(),
            weightRepo.observeLast(),
            habitRepo.observeHabits(),
        )
    ) { arr ->
        val d = arr[0] as LocalDate
        val prof = arr[1] as UserProfileEntity?
        val goal = arr[2] as GoalEntity?
        val weight = arr[3] as WeightEntryEntity?
        val habits = arr[4] as List<HabitDefinitionEntity>
        Fivet(d, prof, goal, weight, habits)
    }.let { firstCombine ->
        combine(
            firstCombine,
            habitRepo.observeDailyLog(_date.value),
            habitRepo.observeSleepForDate(_date.value),
            habitRepo.observeCompletionsForDate(_date.value),
            habitRepo.observeWaterMlForDate(_date.value),
        ) { f, log, sleep, comps, waterMl ->
            HomeUiState(
                date = f.date,
                profile = f.profile,
                goal = f.goal,
                lastWeight = f.weight,
                dailyLog = log,
                readiness = log?.let {
                    ReadinessScore.compute(
                        ReadinessInputs(it.readinessSleep, it.readinessEnergy, it.readinessSoreness, it.readinessMood)
                    )
                },
                habits = f.habits,
                completions = comps,
                sleepEntry = sleep,
                waterMl = waterMl,
            )
        }.combine(nutritionRepo.observeActivePhase()) { s, phase -> s.copy(phase = phase) }
            .combine(nutritionRepo.observeDailyMacros(_date.value)) { s, macros ->
                s.copy(kcalConsumed = macros.kcal, proteinConsumed = macros.protein)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun toggleHabit(habitId: Long, done: Boolean) {
        viewModelScope.launch { habitRepo.toggleCompletion(habitId, _date.value, done) }
    }

    fun saveReadiness(sleep: Int?, energy: Int?, soreness: Int?, mood: Int?) {
        viewModelScope.launch { habitRepo.saveReadiness(_date.value, sleep, energy, soreness, mood) }
    }

    fun logWater(ml: Int) {
        viewModelScope.launch { habitRepo.logWater(_date.value, ml) }
    }

    fun logSleep(bedtime: Instant, wakeTime: Instant, quality: Int?) {
        viewModelScope.launch { habitRepo.logSleep(_date.value, bedtime, wakeTime, quality) }
    }

    fun logPulse(bpm: Int) {
        viewModelScope.launch { habitRepo.saveMorningPulse(_date.value, bpm) }
    }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Intermediate holder because combine max arity = 5
    private data class Fivet(
        val date: LocalDate,
        val profile: UserProfileEntity?,
        val goal: GoalEntity?,
        val weight: WeightEntryEntity?,
        val habits: List<HabitDefinitionEntity>,
    )
}
