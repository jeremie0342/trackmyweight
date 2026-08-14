package com.kps.trackmyweight.ui.workout

import android.content.Intent
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.PersonalRecordEntity
import com.kps.trackmyweight.data.db.entity.WorkoutSessionEntity
import com.kps.trackmyweight.data.db.entity.WorkoutTemplateEntity
import com.kps.trackmyweight.data.db.enums.PrKind
import com.kps.trackmyweight.data.repository.DayPlan
import com.kps.trackmyweight.ui.common.formatDateFr
import com.kps.trackmyweight.ui.common.formatFr
import com.kps.trackmyweight.ui.common.formatTimeFr
import com.kps.trackmyweight.ui.theme.tabular
import kotlinx.coroutines.launch

@Composable
fun WorkoutOverviewScreen(
    onPrepareSession: (Long?) -> Unit,
    onResumeSession: (Long) -> Unit,
    onEditTemplate: (Long?) -> Unit = {},
    onOpenCardio: () -> Unit = {},
    onOpenProgression: () -> Unit = {},
    onOpenRotations: () -> Unit = {},
    onOpenPrograms: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    vm: WorkoutOverviewViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onPrepareSession(null) },
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
            Text("Séance", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)

            state.activeSession?.let { active ->
                ResumeCard(session = active, onResume = { onResumeSession(active.id) })
            }

            state.todaysPlan?.let { plan ->
                TodaysPlanCard(plan = plan, onPrepare = { onPrepareSession(it) })
            }

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
                                onPrepare = { onPrepareSession(t.id) },
                                onEdit = { onEditTemplate(t.id) },
                            )
                        }
                    }
                }
                TextButton(onClick = { onEditTemplate(null) }) { Text("+ Nouveau template") }
                TextButton(onClick = onOpenRotations) { Text("Gérer les rotations") }
                TextButton(onClick = onOpenPrograms) { Text("Gérer les programmes") }
            }

            Section("Cardio") {
                Button(
                    onClick = onOpenCardio,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Loguer une séance cardio") }
            }

            Section("Progression") {
                Button(
                    onClick = onOpenProgression,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Charges max et volume mensuel") }
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
                Button(
                    onClick = onOpenCalendar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Calendrier d'entraînement") }
                if (state.recentSessions.isEmpty()) {
                    Text(
                        "Aucune séance terminée.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val ctx = LocalContext.current
                    val scope = rememberCoroutineScope()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.recentSessions.forEach { s ->
                            SessionRow(s, onShare = {
                                scope.launch {
                                    val text = vm.coachTextFor(s.id)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        putExtra(Intent.EXTRA_SUBJECT, "Séance ${s.date.formatFr()}")
                                    }
                                    ctx.startActivity(Intent.createChooser(intent, "Partager la séance"))
                                }
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

/**
 * Bandeau de reprise. Sans lui, une séance quittée devenait inaccessible :
 * elle restait ouverte en base sans qu'aucun écran ne la propose.
 */
@Composable
private fun ResumeCard(session: WorkoutSessionEntity, onResume: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onResume),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Séance en cours",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "Commencée le ${session.date.formatFr()} à ${session.startedAt.formatTimeFr()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "Reprendre",
                tint = MaterialTheme.colorScheme.onPrimary,
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

@Composable
private fun TemplateRow(t: WorkoutTemplateEntity, onPrepare: () -> Unit, onEdit: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPrepare),
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
            TextButton(onClick = onEdit) { Text("Modifier") }
            Icon(Icons.Outlined.PlayArrow, contentDescription = "Préparer", tint = MaterialTheme.colorScheme.primary)
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
            Text(label, style = MaterialTheme.typography.bodyLarge.tabular())
            Text(
                pr.achievedAt.formatDateFr(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionRow(s: WorkoutSessionEntity, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Séance du ${s.date.formatFr()}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Volume : %.0f kg%s".format(s.totalVolumeKg, s.sessionRpe?.let { " · RPE $it" }.orEmpty()),
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Outlined.Share, contentDescription = "Partager", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Ce qui est prevu aujourd'hui.
 *
 * Distingue trois cas que le modele separe deja : une seance planifiee, un
 * repos explicite, et l'absence de consigne. Confondre les deux derniers
 * laisserait croire a un oubli de planification un jour de repos voulu.
 */
@Composable
private fun TodaysPlanCard(plan: DayPlan, onPrepare: (Long) -> Unit) {
    val meso = plan.mesocycle
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().let { m ->
            if (plan is DayPlan.Training) m.clickable { onPrepare(plan.template.id) } else m
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Au programme aujourd'hui",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (plan) {
                    is DayPlan.Training -> Text(
                        plan.template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    is DayPlan.Rest -> {
                        Text(
                            "Repos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        plan.notes?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is DayPlan.Nothing -> Text(
                        "Rien de planifie",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                meso?.let {
                    Text(
                        "Semaine ${it.currentWeek}/${it.totalWeeks}" +
                            if (it.isOverdue) " · bloc termine" else "",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = if (it.isOverdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (plan is DayPlan.Training) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Preparer",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
