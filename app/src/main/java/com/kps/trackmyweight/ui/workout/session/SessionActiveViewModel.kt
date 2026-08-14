package com.kps.trackmyweight.ui.workout.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.CardioSessionEntity
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedExerciseEntity
import com.kps.trackmyweight.data.db.entity.PerformedSetEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.enums.CardioType
import com.kps.trackmyweight.data.db.enums.SetType
import com.kps.trackmyweight.data.repository.ExerciseRepository
import com.kps.trackmyweight.data.repository.GymRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.data.repository.WorkoutRepository
import com.kps.trackmyweight.domain.calc.RestTime
import com.kps.trackmyweight.domain.calc.WarmupCalculator
import com.kps.trackmyweight.domain.calc.WarmupSet
import com.kps.trackmyweight.ui.workout.exercise.CustomExerciseDraft
import com.kps.trackmyweight.ui.workout.pain.PainDraft
import com.kps.trackmyweight.workout.rest.RestTimerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class ExerciseCard(
    val performed: PerformedExerciseEntity,
    val exercise: ExerciseEntity?,
    val sets: List<PerformedSetEntity>,
    val lastSetPreview: PerformedSetEntity?,
) {
    /** Objectif lisible, ex. « 3 × 8-10 @ 60 kg ». Null si séance libre. */
    val targetLabel: String?
        get() {
            val targetSets = performed.targetSets ?: return null
            val reps = when {
                performed.targetRepsMin != null && performed.targetRepsMax != null ->
                    if (performed.targetRepsMin == performed.targetRepsMax) "${performed.targetRepsMin}"
                    else "${performed.targetRepsMin}-${performed.targetRepsMax}"
                performed.targetRepsMin != null -> "${performed.targetRepsMin}+"
                performed.targetRepsMax != null -> "≤${performed.targetRepsMax}"
                else -> null
            }
            return buildString {
                append("$targetSets série${if (targetSets > 1) "s" else ""}")
                reps?.let { append(" × $it") }
                performed.targetWeightKg?.let { append(" @ %.1f kg".format(it)) }
                performed.targetRpe?.let { append(" · RPE $it") }
            }
        }

    /** Séries comptabilisées (l'échauffement ne compte pas dans l'objectif). */
    val completedSets: Int get() = sets.count { it.type != SetType.WARMUP }

    val isTargetReached: Boolean
        get() = performed.targetSets?.let { completedSets >= it } == true
}

data class SessionActiveUiState(
    val session: WorkoutSessionEntity? = null,
    val exercises: List<ExerciseCard> = emptyList(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    /** Exercices réalisables dans la salle de cette séance. */
    val gymExerciseIds: Set<Long> = emptySet(),
    val onlyMyGym: Boolean = true,
    /** Référentiel d'équipement, pour la création d'un exercice personnalisé. */
    val allEquipment: List<EquipmentEntity> = emptyList(),
    val warmup: CardioSessionEntity? = null,
    val bodyWeightKg: Float? = null,
    val now: Instant = Instant.DISTANT_PAST,
    val restEndsAt: Instant? = null,
    val restTotalSec: Int = 0,
    val isFinishing: Boolean = false,
    val isDone: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Durée écoulée depuis le début de la séance, en secondes. */
    val elapsedSec: Int
        get() = session?.let { (now - it.startedAt).inWholeSeconds.coerceAtLeast(0L).toInt() } ?: 0

    /** Secondes de repos restantes, 0 si aucun repos en cours. */
    val restRemainingSec: Int
        get() = restEndsAt?.let { (it - now).inWholeSeconds.coerceAtLeast(0L).toInt() } ?: 0

    val isResting: Boolean get() = restRemainingSec > 0

    /** Voir `SessionSetupUiState.pickerExercises` : on ne filtre jamais jusqu'à vider la liste. */
    val pickerExercises: List<ExerciseEntity>
        get() = if (onlyMyGym && gymExerciseIds.isNotEmpty()) {
            allExercises.filter { it.id in gymExerciseIds }
        } else {
            allExercises
        }

    val canFilterByGym: Boolean get() = gymExerciseIds.isNotEmpty()

    val hiddenByGymCount: Int
        get() = if (canFilterByGym) allExercises.size - gymExerciseIds.size else 0

    val totalSets: Int get() = exercises.sumOf { it.completedSets }

    val volumeKg: Float
        get() = exercises.sumOf { card ->
            card.sets.filter { it.type != SetType.WARMUP }
                .sumOf { (it.weightKg * it.reps).toDouble() }
        }.toFloat()
}

@HiltViewModel
class SessionActiveViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val weightRepo: WeightRepository,
    private val gymRepo: GymRepository,
    private val restTimer: RestTimerScheduler,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedState.get<Long>("id") ?: 0L

    private val _state = MutableStateFlow(SessionActiveUiState(now = Clock.System.now()))
    val state: StateFlow<SessionActiveUiState> = _state.asStateFlow()

    init {
        // Le chrono de repos est une échéance persistée : il survit à la
        // destruction du ViewModel et à la mort du process.
        val restored = savedState.get<Long>(KEY_REST_ENDS_AT)?.let(Instant::fromEpochMilliseconds)
        val restoredTotal = savedState.get<Int>(KEY_REST_TOTAL) ?: 0
        _state.update { it.copy(restEndsAt = restored, restTotalSec = restoredTotal) }

        refresh()
        startTicker()
    }

    /**
     * Une seule horloge pour tout l'écran : chrono de séance et chrono de repos
     * se déduisent de `now`. Aucun compteur décrémenté, donc aucune dérive.
     */
    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(now = Clock.System.now()) }
                delay(1000)
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val session = workoutRepo.getSession(sessionId)
            val cards = workoutRepo.performedExercisesForSession(sessionId).map { pe ->
                ExerciseCard(
                    performed = pe,
                    exercise = exerciseRepo.getById(pe.exerciseId),
                    sets = workoutRepo.setsForPerformedExercise(pe.id),
                    lastSetPreview = workoutRepo.lastSetForExercise(pe.exerciseId),
                )
            }
            _state.update {
                it.copy(
                    session = session,
                    exercises = cards,
                    allExercises = exerciseRepo.observeAll().first(),
                    warmup = session?.warmupCardioSessionId?.let { id -> workoutRepo.getCardio(id) },
                    bodyWeightKg = weightRepo.observeLast().first()?.weightKg,
                    allEquipment = gymRepo.observeAllEquipment().first(),
                    now = Clock.System.now(),
                )
            }
            val gymId = session?.gymId
            val ids = gymId
                ?.let { exerciseRepo.observeAvailableInGym(it).first().map(ExerciseEntity::id).toSet() }
                .orEmpty()
            _state.update { it.copy(gymExerciseIds = ids) }
        }
    }

    fun toggleOnlyMyGym() = _state.update { it.copy(onlyMyGym = !it.onlyMyGym) }

    fun logWarmup(type: CardioType, durationMin: Int, rpe: Float?) {
        viewModelScope.launch {
            val body = _state.value.bodyWeightKg ?: DEFAULT_BODY_WEIGHT_KG
            runCatching { workoutRepo.logWarmupCardio(sessionId, type, durationMin, body, rpe) }
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            workoutRepo.getOrCreatePerformedExercise(sessionId, exerciseId, _state.value.exercises.size)
            refresh()
        }
    }

    /**
     * Génère et enregistre les séries d'échauffement menant à la charge cible.
     *
     * [WarmupCalculator] était écrit et testé sans aucun appelant. Les séries
     * produites sont typées WARMUP : elles ne comptent ni dans le volume, ni
     * dans les records, ni dans l'objectif de séries.
     *
     * Aucun repos n'est déclenché : on enchaîne l'échauffement, le chrono
     * démarrera à la première vraie série.
     */
    fun generateWarmupSets(performed: PerformedExerciseEntity, topSetKg: Float) {
        viewModelScope.launch {
            val mechanics = exerciseRepo.getById(performed.exerciseId)?.mechanics ?: return@launch
            val sets = WarmupCalculator.generate(topSetKg, mechanics)
            if (sets.isEmpty()) {
                _state.update { it.copy(errorMessage = "Charge trop légère pour un échauffement progressif.") }
                return@launch
            }
            runCatching {
                var setNumber = workoutRepo.setsForPerformedExercise(performed.id).size
                sets.forEach { warmup ->
                    setNumber++
                    workoutRepo.logSet(
                        sessionId = sessionId,
                        exerciseId = performed.exerciseId,
                        performedExerciseId = performed.id,
                        setNumber = setNumber,
                        weightKg = warmup.weightKg,
                        reps = warmup.reps,
                        rpe = null,
                        type = SetType.WARMUP,
                        restBeforeSec = warmup.restSec,
                    )
                }
            }.onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
            refresh()
        }
    }

    /** Suggestion d'échauffement pour un exercice, sans rien enregistrer. */
    fun warmupPreview(performed: PerformedExerciseEntity, topSetKg: Float): List<WarmupSet> {
        val mechanics = _state.value.exercises
            .firstOrNull { it.performed.id == performed.id }?.exercise?.mechanics
            ?: return emptyList()
        return WarmupCalculator.generate(topSetKg, mechanics)
    }

    /** Crée un exercice hors catalogue et l'ajoute à la séance en cours. */
    fun createCustomExercise(draft: CustomExerciseDraft) {
        viewModelScope.launch {
            runCatching {
                exerciseRepo.createCustomExercise(
                    name = draft.name,
                    primaryMuscle = draft.primaryMuscle,
                    mechanics = draft.mechanics,
                    force = draft.force,
                    equipmentIds = draft.equipmentIds,
                )
            }
                .onSuccess { id ->
                    workoutRepo.getOrCreatePerformedExercise(sessionId, id, _state.value.exercises.size)
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    /** Monte l'exercice d'un cran dans la séance. */
    fun moveExerciseUp(performedExerciseId: Long) = moveExercise(performedExerciseId, -1)

    /** Descend l'exercice d'un cran dans la séance. */
    fun moveExerciseDown(performedExerciseId: Long) = moveExercise(performedExerciseId, 1)

    private fun moveExercise(performedExerciseId: Long, delta: Int) {
        viewModelScope.launch {
            runCatching { workoutRepo.moveExerciseInSession(performedExerciseId, delta) }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
            refresh()
        }
    }

    /**
     * Enregistre une douleur ressentie sur un exercice de la séance.
     * L'exercice sert de contexte : c'est ce qui permettra de rapprocher une
     * zone récurrente des mouvements qui la sollicitent.
     */
    fun logPain(exerciseId: Long, draft: PainDraft) {
        viewModelScope.launch {
            runCatching {
                workoutRepo.logPain(
                    area = draft.area,
                    intensity = draft.intensity,
                    contextExerciseId = exerciseId,
                    notes = draft.notes,
                )
            }.onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun removeExercise(performedExerciseId: Long) {
        viewModelScope.launch {
            workoutRepo.removeExerciseFromSession(performedExerciseId)
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
            val setNumber = workoutRepo.setsForPerformedExercise(performed.id).size + 1
            runCatching {
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
            }.onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }

            // Dans un superset, on enchaîne : le repos ne se déclenche qu'après
            // le dernier exercice du groupe, pas entre A1 et A2.
            if (isLastOfSuperset(performed)) {
                // Le repos planifié à la préparation prime sur le défaut par mécanique.
                val restSec = performed.restSecOverride
                    ?: exerciseRepo.getById(performed.exerciseId)?.mechanics?.let(RestTime::defaultSecFor)
                    ?: DEFAULT_REST_SEC
                startRest(restSec)
            }
            refresh()
        }
    }

    /**
     * Vrai si cet exercice est isolé, ou s'il ferme son superset.
     * Détermine si l'on part en repos ou si l'on enchaîne.
     */
    private fun isLastOfSuperset(performed: PerformedExerciseEntity): Boolean {
        val group = performed.supersetGroup ?: return true
        val siblings = _state.value.exercises
            .map { it.performed }
            .filter { it.supersetGroup == group }
        return siblings.maxByOrNull { it.orderIndex }?.id == performed.id
    }

    fun updateSet(setId: Long, weightKg: Float, reps: Int, rpe: Float?, type: SetType) {
        viewModelScope.launch {
            runCatching { workoutRepo.updateSet(setId, weightKg, reps, rpe, type) }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
            refresh()
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            runCatching { workoutRepo.deleteSet(setId) }
                .onFailure { e -> _state.update { it.copy(errorMessage = e.message) } }
            refresh()
        }
    }

    // ─────── Chrono de repos ───────

    fun startRest(seconds: Int) {
        if (seconds <= 0) return
        val endsAt = Clock.System.now() + seconds.seconds
        persistRest(endsAt, seconds)
        restTimer.schedule(endsAt)
    }

    /** Allonge ou raccourcit le repos en cours. Ne descend pas sous l'instant présent. */
    fun adjustRest(deltaSec: Int) {
        val current = _state.value
        val base = current.restEndsAt ?: return
        val now = Clock.System.now()
        val endsAt = (base + deltaSec.seconds).coerceAtLeast(now)
        val total = (current.restTotalSec + deltaSec).coerceAtLeast(0)
        if (endsAt <= now) {
            cancelRest()
        } else {
            persistRest(endsAt, total)
            restTimer.schedule(endsAt)
        }
    }

    fun cancelRest() {
        persistRest(null, 0)
        restTimer.cancel()
    }

    private fun persistRest(endsAt: Instant?, totalSec: Int) {
        savedState[KEY_REST_ENDS_AT] = endsAt?.toEpochMilliseconds()
        savedState[KEY_REST_TOTAL] = totalSec
        _state.update { it.copy(restEndsAt = endsAt, restTotalSec = totalSec, now = Clock.System.now()) }
    }

    // ─────── Fin de vie ───────

    fun finishSession(sessionRpe: Float?, notes: String?) {
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            runCatching { workoutRepo.endSession(sessionId, sessionRpe, notes) }
                .onSuccess {
                    restTimer.cancel()
                    _state.update { it.copy(isFinishing = false, isDone = true) }
                }
                .onFailure { e -> _state.update { it.copy(isFinishing = false, errorMessage = e.message) } }
        }
    }

    /** Abandonne la séance : elle sort de l'historique et du bandeau de reprise. */
    fun abandonSession() {
        _state.update { it.copy(isFinishing = true) }
        viewModelScope.launch {
            runCatching { workoutRepo.abandonSession(sessionId) }
                .onSuccess {
                    restTimer.cancel()
                    _state.update { it.copy(isFinishing = false, isDone = true) }
                }
                .onFailure { e -> _state.update { it.copy(isFinishing = false, errorMessage = e.message) } }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    private companion object {
        const val KEY_REST_ENDS_AT = "restEndsAt"
        const val KEY_REST_TOTAL = "restTotalSec"
        const val DEFAULT_REST_SEC = 120
        const val DEFAULT_BODY_WEIGHT_KG = 70f
    }
}
