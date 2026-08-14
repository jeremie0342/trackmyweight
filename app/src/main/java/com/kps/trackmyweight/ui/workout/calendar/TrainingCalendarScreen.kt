package com.kps.trackmyweight.ui.workout.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.formatFr
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.common.toMonthLabelFr
import com.kps.trackmyweight.ui.theme.tabular
import kotlinx.datetime.LocalDate

private val WEEKDAY_INITIALS = listOf("L", "M", "M", "J", "V", "S", "D")

/**
 * Calendrier d'entraînement : quels jours ont été travaillés, et quoi.
 */
@Composable
fun TrainingCalendarScreen(
    onBack: () -> Unit,
    vm: TrainingCalendarViewModel = hiltViewModel(),
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
            BackHeader(title = "Calendrier", onBack = onBack)

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::previousMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mois précédent")
                }
                Text(
                    "%04d-%02d".format(state.month.year, state.month.monthNumber).toMonthLabelFr()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = vm::nextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Mois suivant")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                WEEKDAY_INITIALS.forEach { initial ->
                    Text(
                        initial,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Grille manuelle plutot qu'une LazyVerticalGrid : imbriquer une
            // grille scrollable dans une colonne scrollable exige une hauteur
            // bornee, alors qu'un mois tient toujours en 6 lignes au plus.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.grid.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        week.forEach { date ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (date == null) {
                                    Spacer(Modifier.fillMaxWidth().aspectRatio(1f))
                                } else {
                                    DayCell(
                                        date = date,
                                        volumeKg = state.trainingDays[date],
                                        isToday = date == state.today,
                                        isSelected = state.selected?.date == date,
                                        onClick = { vm.select(date) },
                                    )
                                }
                            }
                        }
                        // Complete la derniere semaine pour garder des cases carrees.
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f)) {
                                Spacer(Modifier.fillMaxWidth().aspectRatio(1f))
                            }
                        }
                    }
                }
            }

            Text(
                "${state.trainedDayCount} jour(s) travaillé(s) · %.0f kg soulevés ce mois-ci"
                    .format(state.monthVolumeKg),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.selected?.let { DayDetailCard(it) }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    volumeKg: Float?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val trained = volumeKg != null
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        trained -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val foreground = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        trained -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                // Le jour courant est cercle plutot que colore : sinon il se
                // confondrait avec un jour d'entrainement.
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = foreground,
            fontWeight = if (trained) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DayDetailCard(detail: DayDetail) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                detail.date.formatFr(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )

            if (detail.isEmpty) {
                Text(
                    "Rien de logué ce jour-là.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                detail.sessions.forEach { session ->
                    Text(
                        "Muscu · %.0f kg de volume%s".format(
                            session.totalVolumeKg,
                            session.sessionRpe?.let { " · RPE $it" }.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                    )
                    session.notes?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                detail.cardio.forEach { cardio ->
                    Text(
                        "${cardio.type.labelFr()} · ${cardio.durationSec / 60} min · " +
                            "${cardio.caloriesEstimated.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                    )
                }
            }
        }
    }
}
