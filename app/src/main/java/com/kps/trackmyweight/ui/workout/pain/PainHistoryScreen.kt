package com.kps.trackmyweight.ui.workout.pain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.kps.trackmyweight.data.db.entity.PainHotspotRow
import com.kps.trackmyweight.data.db.entity.PainLogEntity
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.formatFr
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.common.painIntensityLabelFr
import com.kps.trackmyweight.ui.theme.tabular

/**
 * Où j'ai mal, à quelle fréquence, et sur quels mouvements.
 */
@Composable
fun PainHistoryScreen(
    onBack: () -> Unit,
    vm: PainHistoryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::openLogDialog,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Signaler") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackHeader(title = "Douleurs", onBack = onBack)

            if (!state.hasHistory) {
                Text(
                    "Aucune douleur signalée. Tant mieux — et si ça arrive, note-la : " +
                        "c'est la répétition d'une zone qui révèle un problème, pas un épisode isolé.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (state.hotspots.isNotEmpty()) {
                    Section("Zones récurrentes · 90 jours") {
                        val maxOccurrences = state.hotspots.maxOf { it.occurrences }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.hotspots.forEach { hotspot ->
                                HotspotRow(
                                    hotspot = hotspot,
                                    maxOccurrences = maxOccurrences,
                                    expanded = state.expandedArea == hotspot.area,
                                    contextExercises = state.contextByArea[hotspot.area]
                                        ?.joinToString(", ") { "${it.exerciseName} (${it.occurrences})" },
                                    onClick = { vm.toggleArea(hotspot.area) },
                                )
                            }
                        }
                    }
                }

                Section("Historique") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.entries.forEach { entry ->
                            PainRow(entry = entry, onDelete = { vm.delete(entry.id) })
                        }
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }

    if (state.showLogDialog) {
        PainDialog(
            onDismiss = vm::dismissLogDialog,
            onConfirm = vm::logPain,
        )
    }
}

@Composable
private fun HotspotRow(
    hotspot: PainHotspotRow,
    maxOccurrences: Int,
    expanded: Boolean,
    contextExercises: String?,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                hotspot.area.labelFr(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${hotspot.occurrences}×",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                fontWeight = FontWeight.Medium,
            )
        }
        LinearProgressIndicator(
            progress = { hotspot.occurrences.toFloat() / maxOccurrences },
            modifier = Modifier.fillMaxWidth(),
            // Le pic, pas la moyenne : une zone montée une fois à 9 mérite
            // l'alerte même si elle est habituellement supportable.
            color = if (hotspot.peakIntensity >= 7) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Text(
            "Intensité moyenne %.1f/10 · pic %d/10 · dernière fois le %s".format(
                hotspot.averageIntensity,
                hotspot.peakIntensity,
                hotspot.lastDate.formatFr(),
            ),
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            Text(
                if (contextExercises.isNullOrBlank()) {
                    "Aucun exercice associé à ces signalements."
                } else {
                    "Souvent signalé sur : $contextExercises. " +
                        "Corrélation, pas preuve — à regarder de plus près."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PainRow(entry: PainLogEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${entry.area.labelFr()} · ${entry.intensity}/10",
                style = MaterialTheme.typography.bodyMedium.tabular(),
            )
            Text(
                "${entry.date.formatFr()} · ${painIntensityLabelFr(entry.intensity)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.notes?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Supprimer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
