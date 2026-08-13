package com.kps.trackmyweight.ui.workout.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.MonthlyTonnageRow
import com.kps.trackmyweight.data.repository.MaxLoadSummary
import com.kps.trackmyweight.domain.calc.VolumeStatus
import com.kps.trackmyweight.domain.calc.VolumeVerdict
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.adviceFr
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.common.Sparkline
import com.kps.trackmyweight.ui.common.toMonthLabelFr
import com.kps.trackmyweight.ui.theme.tabular

/**
 * Progression : combien je soulève chaque mois, et comment mes charges
 * maximales évoluent.
 */
@Composable
fun ProgressionScreen(
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit = {},
    vm: ProgressionViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackHeader(title = "Progression", onBack = onBack)

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                state.monthlyTonnage.isEmpty() && state.maxLoads.isEmpty() &&
                    state.weeklyVolume.isEmpty() -> Text(
                    "Rien à afficher pour l'instant. Termine quelques séances et " +
                        "tes courbes se rempliront d'elles-mêmes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> {
                    if (state.monthlyTonnage.isNotEmpty()) {
                        TonnageHeadline(state)
                        Section("Volume par mois") {
                            if (state.monthlyTonnage.size >= 2) {
                                Sparkline(
                                    raw = state.monthlyTonnage.map { it.volumeKg },
                                    smoothed = state.monthlyTonnage.map { it.volumeKg },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            MonthlyBars(state.monthlyTonnage)
                        }
                    }

                    if (state.weeklyVolume.isNotEmpty()) {
                        Section("Volume de la semaine") {
                            Text(
                                "Séries par groupe musculaire, comparées aux repères " +
                                    "MEV / MAV / MRV. Les muscles secondaires comptent pour moitié.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.weeklyVolume.forEach { VolumeRow(it) }
                            }
                        }
                    }

                    if (state.weeklySetTotals.size >= 2) {
                        Section("Tendance des 8 dernières semaines") {
                            Sparkline(
                                raw = state.weeklySetTotals.map { it.second.toFloat() },
                                smoothed = state.weeklySetTotals.map { it.second.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "De ${state.weeklySetTotals.first().second} à " +
                                    "${state.weeklySetTotals.last().second} séries par semaine.",
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (state.improving.isNotEmpty()) {
                        Section("Charges en progression") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.improving.forEach {
                                    MaxLoadRow(it, onClick = { onOpenExercise(it.exerciseId) })
                                }
                            }
                        }
                    }

                    if (state.stalling.isNotEmpty()) {
                        Section("Sans progression sur 90 jours") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.stalling.forEach {
                                    MaxLoadRow(it, onClick = { onOpenExercise(it.exerciseId) })
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TonnageHeadline(state: ProgressionUiState) {
    val current = state.currentMonth ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                current.month.toMonthLabelFr(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.0f".format(current.volumeKg),
                    style = MaterialTheme.typography.displaySmall.tabular(),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    " kg soulevés",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                state.monthOverMonthPercent?.let { DeltaBadge(it) }
            }
            Text(
                "${current.sessionCount} séance${if (current.sessionCount > 1) "s" else ""} · " +
                    "${current.setCount} séries",
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthlyBars(rows: List<MonthlyTonnageRow>) {
    val max = rows.maxOfOrNull { it.volumeKg }?.takeIf { it > 0f } ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.reversed().forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(
                        row.month.toMonthLabelFr(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "%.0f kg".format(row.volumeKg),
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        fontWeight = FontWeight.Medium,
                    )
                }
                LinearProgressIndicator(
                    progress = { row.volumeKg / max },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
        }
    }
}

/**
 * Volume d'un groupe musculaire sur la semaine, situé entre MEV et MRV.
 * La barre est bornée au MRV : dépasser le maximum récupérable doit se voir.
 */
@Composable
private fun VolumeRow(verdict: VolumeVerdict) {
    val color = when (verdict.status) {
        VolumeStatus.UNDER_MEV -> MaterialTheme.colorScheme.tertiary
        VolumeStatus.WITHIN_RANGE -> MaterialTheme.colorScheme.primary
        VolumeStatus.AT_MAV -> MaterialTheme.colorScheme.primary
        VolumeStatus.OVER_MRV -> MaterialTheme.colorScheme.error
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                verdict.muscleGroup.labelFr(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${verdict.currentSets} séries",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                fontWeight = FontWeight.Medium,
            )
        }
        LinearProgressIndicator(
            progress = { (verdict.currentSets.toFloat() / verdict.landmarks.mrv).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Text(
            "${verdict.status.labelFr()} · ${verdict.adviceFr()} " +
                "(MEV ${verdict.landmarks.mev} · MAV ${verdict.landmarks.mav} · MRV ${verdict.landmarks.mrv})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MaxLoadRow(summary: MaxLoadSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(summary.exerciseName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "%.1f kg".format(summary.current.oneRmKg) +
                    (summary.deltaKg?.let { " · %+.1f kg sur 90 j".format(it) } ?: " · première mesure"),
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        summary.progressionPercent?.let { DeltaBadge(it) }
    }
}

@Composable
private fun DeltaBadge(percent: Float) {
    val positive = percent >= 0f
    val color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(
        "%+.1f %%".format(percent),
        style = MaterialTheme.typography.labelLarge.tabular(),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
