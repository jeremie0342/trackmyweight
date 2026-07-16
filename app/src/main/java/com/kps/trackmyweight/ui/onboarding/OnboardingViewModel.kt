package com.kps.trackmyweight.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.data.db.enums.UnitSystem
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.OnboardingRepository
import com.kps.trackmyweight.domain.calc.AgeCalculator
import com.kps.trackmyweight.domain.calc.NutritionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepo: OnboardingRepository,
    private val gymRepo: GymRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val equipment: StateFlow<List<EquipmentEntity>> = gymRepo.observeAllEquipment()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { gymRepo.seedEquipmentIfEmpty() }
    }

    // ── Step navigation ───────────────────────────────────
    fun next() {
        _state.update { current ->
            val next = OnboardingStep.entries.firstOrNull { it.order == current.step.order + 1 }
                ?: current.step
            current.copy(step = next)
        }
    }

    fun back() {
        _state.update { current ->
            val prev = OnboardingStep.entries.firstOrNull { it.order == current.step.order - 1 }
                ?: current.step
            current.copy(step = prev)
        }
    }

    // ── Identity ──────────────────────────────────────────
    fun setSex(sex: Sex) = _state.update { it.copy(sex = sex) }
    fun setBirthDate(date: LocalDate) = _state.update { it.copy(birthDate = date) }
    fun setHeightCm(v: Float?) = _state.update { it.copy(heightCm = v) }
    fun setCurrentWeightKg(v: Float?) = _state.update { it.copy(currentWeightKg = v) }

    // ── Goal ──────────────────────────────────────────────
    fun setTargetWeightKg(v: Float?) = _state.update { it.copy(targetWeightKg = v) }
    fun setTargetDate(date: LocalDate) = _state.update { it.copy(targetDate = date) }
    fun setPhaseOverride(phase: GoalPhase?) = _state.update { it.copy(phaseOverride = phase) }

    // ── Activity ──────────────────────────────────────────
    fun setActivityLevel(level: ActivityLevel) = _state.update { it.copy(activityLevel = level) }

    // ── Gym ───────────────────────────────────────────────
    fun setGymName(name: String) = _state.update { it.copy(gymName = name) }
    fun toggleEquipment(id: Long) = _state.update {
        it.copy(selectedEquipmentIds = it.selectedEquipmentIds.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        })
    }
    fun setSkipGym(skip: Boolean) = _state.update { it.copy(skipGym = skip) }

    // ── Coach mode ────────────────────────────────────────
    fun setCoachMode(enabled: Boolean) = _state.update { it.copy(coachModeEnabled = enabled) }

    // ── Recap ─────────────────────────────────────────────
    /** Calcule et cache les targets pour l'écran récap. */
    fun computeTargets(today: LocalDate = todayLocal()) {
        val s = _state.value
        val sex = s.sex ?: return
        val birth = s.birthDate ?: return
        val weight = s.currentWeightKg ?: return
        val height = s.heightCm ?: return
        val targetWeight = s.targetWeightKg ?: return
        val targetDate = s.targetDate ?: return
        val age = AgeCalculator.yearsBetween(birth, today)
        val targets = NutritionCalculator.compute(
            sex = sex,
            weightKg = weight,
            heightCm = height,
            ageYears = age,
            activityLevel = s.activityLevel,
            targetWeightKg = targetWeight,
            today = today,
            targetDate = targetDate,
            overridePhase = s.phaseOverride,
        )
        _state.update { it.copy(computedTargets = targets) }
    }

    // ── Finish ────────────────────────────────────────────
    fun finish() {
        val s = _state.value
        val sex = s.sex ?: return
        val birth = s.birthDate ?: return
        val height = s.heightCm ?: return
        val currentWeight = s.currentWeightKg ?: return
        val targetWeight = s.targetWeightKg ?: return
        val targetDate = s.targetDate ?: return
        val targets = s.computedTargets ?: return

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val now = Clock.System.now()
                val today = todayLocal()
                onboardingRepo.completeOnboarding(
                    profile = UserProfileEntity(
                        sex = sex,
                        birthDate = birth,
                        heightCm = height,
                        preferredUnit = UnitSystem.METRIC,
                        currency = "XOF",
                        locale = "fr",
                        activityLevel = s.activityLevel,
                        coachModeEnabled = s.coachModeEnabled,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    goal = GoalEntity(
                        targetWeightKg = targetWeight,
                        targetDate = targetDate,
                        phase = targets.recommendedPhase,
                        isActive = true,
                        startedAt = today,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    targets = targets,
                    gymName = if (s.skipGym) null else s.gymName.ifBlank { "Ma salle" },
                    equipmentIds = if (s.skipGym) emptySet() else s.selectedEquipmentIds,
                )
            }.onSuccess {
                _state.update { it.copy(isSaving = false, isDone = true) }
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Erreur inconnue") }
            }
        }
    }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
