package com.kps.trackmyweight.ui.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity
import com.kps.trackmyweight.data.db.entity.BodyMeasurementSessionEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.entity.WeightEntryEntity
import com.kps.trackmyweight.data.repository.MeasurementRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.RatioCalculator
import com.kps.trackmyweight.domain.calc.WhtrCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class MeasurementsUiState(
    val sessions: List<BodyMeasurementSessionEntity> = emptyList(),
    val lastSession: BodyMeasurementSessionEntity? = null,
    val previousSession: BodyMeasurementSessionEntity? = null,
    val lastComposition: BodyCompositionSnapshotEntity? = null,
    val whtr: Float? = null,
    val whtrCategory: WhtrCategory? = null,
    val whr: Float? = null,
    val profile: UserProfileEntity? = null,
    val lastWeight: WeightEntryEntity? = null,
    // Draft (form inputs)
    val draft: MeasurementDraft = MeasurementDraft(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class MeasurementDraft(
    val date: LocalDate = LocalDate(2000, 1, 1),
    val neck: String = "",
    val shoulder: String = "",
    val chest: String = "",
    val waist: String = "",
    val hip: String = "",
    val armLeft: String = "",
    val armRight: String = "",
    val forearmLeft: String = "",
    val forearmRight: String = "",
    val thighLeft: String = "",
    val thighRight: String = "",
    val calfLeft: String = "",
    val calfRight: String = "",
    val wrist: String = "",
    val notes: String = "",
) {
    fun anyValueSet(): Boolean = listOf(
        neck, shoulder, chest, waist, hip, armLeft, armRight,
        forearmLeft, forearmRight, thighLeft, thighRight, calfLeft, calfRight, wrist,
    ).any { it.isNotBlank() }
}

@HiltViewModel
class MeasurementsViewModel @Inject constructor(
    private val measurementRepo: MeasurementRepository,
    private val userRepo: UserProfileRepository,
    private val weightRepo: WeightRepository,
) : ViewModel() {

    private val _draft = MutableStateFlow(MeasurementDraft(date = todayLocal()))
    private val _saving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<MeasurementsUiState> = combine(
        measurementRepo.observeAll(),
        measurementRepo.observeLastComposition(),
        userRepo.observe(),
        weightRepo.observeLast(),
        combine(_draft, _saving, _error) { d, s, e -> Triple(d, s, e) },
    ) { sessions, comp, profile, weight, triple ->
        val (draft, saving, error) = triple
        val sorted = sessions.sortedByDescending { it.date }
        val last = sorted.firstOrNull()
        val prev = sorted.getOrNull(1)
        val whtr = last?.waistCm?.let { w -> profile?.heightCm?.let { h -> RatioCalculator.whtr(w, h) } }
        val whr = last?.let { RatioCalculator.whr(it.waistCm, it.hipCm) }
        MeasurementsUiState(
            sessions = sorted,
            lastSession = last,
            previousSession = prev,
            lastComposition = comp,
            whtr = whtr,
            whtrCategory = whtr?.let(RatioCalculator::categorizeWhtr),
            whr = whr,
            profile = profile,
            lastWeight = weight,
            draft = draft,
            isSaving = saving,
            errorMessage = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeasurementsUiState())

    fun updateDraft(transform: (MeasurementDraft) -> MeasurementDraft) {
        _draft.update(transform)
    }

    fun loadDraftFromLast() {
        viewModelScope.launch {
            val today = todayLocal()
            val existing = measurementRepo.getOnDate(today)
            _draft.value = if (existing != null) fromEntity(existing) else {
                val last = measurementRepo.observeLast()
                // Preload with previous values as hint
                MeasurementDraft(date = today)
            }
        }
    }

    fun save() {
        val d = _draft.value
        val profile = state.value.profile
        val weight = state.value.lastWeight
        _saving.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                measurementRepo.save(
                    date = d.date,
                    neckCm = d.neck.parseCm(),
                    shoulderCm = d.shoulder.parseCm(),
                    chestCm = d.chest.parseCm(),
                    waistCm = d.waist.parseCm(),
                    hipCm = d.hip.parseCm(),
                    armLeftCm = d.armLeft.parseCm(),
                    armRightCm = d.armRight.parseCm(),
                    forearmLeftCm = d.forearmLeft.parseCm(),
                    forearmRightCm = d.forearmRight.parseCm(),
                    thighLeftCm = d.thighLeft.parseCm(),
                    thighRightCm = d.thighRight.parseCm(),
                    calfLeftCm = d.calfLeft.parseCm(),
                    calfRightCm = d.calfRight.parseCm(),
                    wristCm = d.wrist.parseCm(),
                    notes = d.notes.takeIf { it.isNotBlank() },
                    sex = profile?.sex,
                    heightCm = profile?.heightCm,
                    weightKg = weight?.weightKg,
                )
            }.onSuccess {
                _saving.value = false
                _draft.value = MeasurementDraft(date = todayLocal())
            }.onFailure {
                _saving.value = false
                _error.value = it.message ?: "Erreur d'enregistrement"
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun String.parseCm(): Float? = replace(',', '.').toFloatOrNull()

    private fun fromEntity(e: BodyMeasurementSessionEntity): MeasurementDraft = MeasurementDraft(
        date = e.date,
        neck = e.neckCm?.toStr().orEmpty(),
        shoulder = e.shoulderCm?.toStr().orEmpty(),
        chest = e.chestCm?.toStr().orEmpty(),
        waist = e.waistCm?.toStr().orEmpty(),
        hip = e.hipCm?.toStr().orEmpty(),
        armLeft = e.armLeftCm?.toStr().orEmpty(),
        armRight = e.armRightCm?.toStr().orEmpty(),
        forearmLeft = e.forearmLeftCm?.toStr().orEmpty(),
        forearmRight = e.forearmRightCm?.toStr().orEmpty(),
        thighLeft = e.thighLeftCm?.toStr().orEmpty(),
        thighRight = e.thighRightCm?.toStr().orEmpty(),
        calfLeft = e.calfLeftCm?.toStr().orEmpty(),
        calfRight = e.calfRightCm?.toStr().orEmpty(),
        wrist = e.wristCm?.toStr().orEmpty(),
        notes = e.notes.orEmpty(),
    )

    private fun Float.toStr(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    init { loadDraftFromLast() }
}
