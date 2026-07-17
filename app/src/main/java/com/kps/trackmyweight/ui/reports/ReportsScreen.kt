package com.kps.trackmyweight.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.repository.AnalyticsRepository
import com.kps.trackmyweight.data.repository.CoachAutoApply
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.NutritionRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.data.repository.WeeklyReport
import com.kps.trackmyweight.domain.calc.CoachAdvice
import com.kps.trackmyweight.domain.calc.CoachAdviceKind
import com.kps.trackmyweight.domain.calc.NutritionCalculator
import com.kps.trackmyweight.ui.common.PrimaryButton
import androidx.compose.material3.TextButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ReportsUiState(
    val report: WeeklyReport? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val analyticsRepo: AnalyticsRepository,
    private val goalRepo: GoalRepository,
    private val nutritionRepo: NutritionRepository,
    private val weightRepo: WeightRepository,
    private val coachAuto: CoachAutoApply,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init { generate() }

    fun generate() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val today = analyticsRepo.todayLocal()
            val goal = goalRepo.observeActive().first()
            val phase = nutritionRepo.observeActivePhase().first()
            if (goal == null || phase == null) {
                _state.value = ReportsUiState(isLoading = false)
                return@launch
            }
            val weeksInPhase = ((today.toEpochDays() - phase.startDate.toEpochDays()) / 7).toInt().coerceAtLeast(1)
            val report = analyticsRepo.generateWeekly(
                today = today,
                goalPhase = goal.phase,
                goalTargetKg = goal.targetWeightKg,
                goalTargetDate = goal.targetDate,
                proteinTargetG = phase.targetProteinG,
                kcalTargetG = phase.targetKcal,
                weeksInPhase = weeksInPhase,
            )
            _state.value = ReportsUiState(report = report, isLoading = false)
        }
    }

    fun applyAdvice(advice: CoachAdvice) {
        viewModelScope.launch {
            val today = analyticsRepo.todayLocal()
            val phase = nutritionRepo.observeActivePhase().first() ?: return@launch
            when (advice.kind) {
                CoachAdviceKind.REFEED_DUE -> {
                    // TDEE approximatif = phase.targetKcal + 400 (estimation moyenne du déficit actuel)
                    val tdeeEstimate = phase.targetKcal + 400
                    coachAuto.applyRefeed(from = today, tdeeKcal = tdeeEstimate, proteinTargetG = phase.targetProteinG)
                    generate()
                }
                CoachAdviceKind.DELOAD_DUE -> {
                    // Un flag pourrait être ajouté dans un DataStore ; on notifie juste pour l'instant
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ReportsScreen(vm: ReportsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Rapport hebdo", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)

            val r = state.report
            if (r == null) {
                Text(
                    if (state.isLoading) "Calcul du rapport..." else "Complète l'onboarding pour générer un rapport.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AdherenceCard(r.summary.adherencePct)
                NarrativeCard(r.summary.narrative)
                MetricsGrid(r.summary)
                r.projection?.let { ProjectionCard(it) }
                CoachAdvicesCard(r.advices, onApply = vm::applyAdvice)
            }
            PrimaryButton(text = "Régénérer", onClick = vm::generate, enabled = !state.isLoading)
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AdherenceCard(pct: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Adhérence semaine", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${pct.toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(progress = { (pct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NarrativeCard(narrative: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Synthèse", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(narrative, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MetricsGrid(s: com.kps.trackmyweight.domain.calc.WeeklySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Métriques", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Metric(label = "Δ poids", value = "%+.1f kg".format(s.weightDeltaKg))
                Metric(label = "Séances", value = "${s.sessionsCount}")
                Metric(label = "Cardio", value = "${s.cardioCount}")
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Metric(label = "Protéines moy.", value = "%.0f g".format(s.avgProteinG))
                Metric(label = "Kcal moy.", value = "%.0f".format(s.avgKcal))
                Metric(label = "Sommeil moy.", value = com.kps.trackmyweight.domain.calc.SleepDuration.format(s.avgSleepMin.toInt()))
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Metric(label = "Readiness moy.", value = "%.1f".format(s.avgReadiness))
                Metric(label = "Pas totaux", value = "${s.totalSteps / 1000}k")
                Metric(label = "Volume", value = "%.0f kg".format(s.totalVolumeKg))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProjectionCard(p: com.kps.trackmyweight.domain.calc.ProjectionResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Projection vers l'objectif", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("À date cible : %.1f kg".format(p.projectedWeightAtTarget), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Tendance : %+.2f kg/sem · confiance %.0f%%".format(p.trendKgPerWeek, p.confidence * 100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.etaForTarget?.let {
                Text("ETA : $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text(p.narrative, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoachAdvicesCard(
    advices: List<com.kps.trackmyweight.domain.calc.CoachAdvice>,
    onApply: (com.kps.trackmyweight.domain.calc.CoachAdvice) -> Unit = {},
) {
    if (advices.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Coach", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            advices.forEach { a ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(a.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(a.message, style = MaterialTheme.typography.bodyMedium)
                    if (a.kind == CoachAdviceKind.REFEED_DUE) {
                        TextButton(onClick = { onApply(a) }) { Text("Programmer une semaine de refeed") }
                    }
                }
            }
        }
    }
}
