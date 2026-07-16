package com.kps.trackmyweight.ui.workout

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.PrKind

@Composable
fun WorkoutOverviewScreen(
    onStartSession: (Long) -> Unit,
    onEditTemplate: (Long?) -> Unit = {},
    onOpenCardio: () -> Unit = {},
    vm: WorkoutOverviewViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startSession(templateId = null, onStarted = onStartSession) },
                icon = { Icon(Icons.Outlined.PlayArrow, null) },
                text = { Text("Séance libre") },
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
            Spacer(Modifier.height(16.dp))
            Text(
                "Séance",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            Section("Templates") {
                if (state.templates.isEmpty()) {
                    Text(
                        "Aucun template pour l'instant. Crée-en un ou démarre une séance libre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.templates.forEach { t ->
                            TemplateRow(
                                t = t,
                                onStart = { vm.startSession(t.id, onStartSession) },
                                onEdit = { onEditTemplate(t.id) },
                            )
                        }
                    }
                }
                androidx.compose.material3.TextButton(
                    onClick = { onEditTemplate(null) },
                    modifier = Modifier,
                ) { Text("+ Nouveau template") }
            }

            Section("Cardio") {
                androidx.compose.material3.Button(
                    onClick = onOpenCardio,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Loguer une séance cardio") }
            }

            Section("Records récents") {
                if (state.recentPrs.isEmpty()) {
                    Text(
                        "Pas encore de PR — ils apparaîtront ici à mesure que tu progresses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.recentPrs.forEach { pr -> PrRow(pr) }
                    }
                }
            }

            Section("Historique") {
                if (state.recentSessions.isEmpty()) {
                    Text(
                        "Aucune séance loguée.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.recentSessions.forEach { s -> SessionRow(s) }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
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

@Composable
private fun TemplateRow(t: WorkoutTemplateEntity, onStart: () -> Unit, onEdit: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onStart),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                t.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            androidx.compose.material3.TextButton(onClick = onEdit) { Text("Modifier") }
            Icon(Icons.Outlined.PlayArrow, contentDescription = "Démarrer", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PrRow(pr: PersonalRecordEntity) {
    val label = when (pr.kind) {
        PrKind.MAX_WEIGHT_ANY_REPS -> "Poids max : %.1f kg".format(pr.value)
        PrKind.ONE_RM_EST -> "1RM estimé : %.1f kg".format(pr.value)
        PrKind.MAX_REPS_AT_WEIGHT -> "${pr.value.toInt()} reps à %.1f kg".format(pr.referenceValue ?: 0f)
        PrKind.THREE_RM -> "3RM : %.1f kg".format(pr.value)
        PrKind.FIVE_RM -> "5RM : %.1f kg".format(pr.value)
        PrKind.MAX_VOLUME_SESSION -> "Volume max séance : %.0f kg".format(pr.value)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(pr.achievedAt.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionRow(s: WorkoutSessionEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Séance ${s.date}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Volume : %.0f kg%s".format(s.totalVolumeKg, s.sessionRpe?.let { " · RPE $it" }.orEmpty()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
