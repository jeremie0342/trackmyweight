package com.kps.trackmyweight.ui.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.domain.calc.BmiCategory
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.Sparkline

@Composable
fun WeightScreen(
    onBack: () -> Unit = {},
    vm: WeightViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Pesée") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))
            com.kps.trackmyweight.ui.common.BackHeader(title = "Poids", onBack = onBack)

            HeroCard(state)
            if (state.entries.size >= 2) TrendCard(state)
            if (state.goal != null) GoalCard(state)
            if (state.stagnating) StagnationCard(state.stagnationNetChangeKg)
            if (state.bmi != null) BmiCard(state)
            if (state.entries.isNotEmpty()) HistoryList(state, onDelete = vm::softDelete)
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showDialog) {
        LogWeightDialog(
            initial = state.lastWeightKg,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismiss = { showDialog = false; vm.clearError() },
            onConfirm = { kg ->
                vm.logWeight(kg)
                showDialog = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Sections
// ─────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(state: WeightUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Dernière pesée", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val kg = state.lastWeightKg
            if (kg == null) {
                Text("Pas encore de donnée", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.1f".format(kg),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("kg", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val delta = state.previousWeightKg?.let { kg - it }
                if (delta != null) {
                    val color = when {
                        delta < 0 -> MaterialTheme.colorScheme.primary
                        delta > 0 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        (if (delta >= 0) "+" else "") + "%.1f kg vs pesée précédente".format(delta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendCard(state: WeightUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tendance (moyenne 7j)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            WeightChart(state)
        }
    }
}

@Composable
private fun WeightChart(state: WeightUiState) {
    val raw = state.entries.map { it.weightKg }
    val smoothed = state.smoothed.map { it.smoothed }
    Sparkline(
        raw = raw,
        smoothed = smoothed,
        targetLine = state.goal?.targetWeightKg,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GoalCard(state: WeightUiState) {
    val goal = state.goal ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Objectif", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "%.1f kg d'ici le ${goal.targetDate}".format(goal.targetWeightKg),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            state.etaForTarget?.let {
                Text(
                    "Selon la tendance actuelle : ETA $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.projectedAtTargetKg?.let {
                Text(
                    "Projection à la date cible : %.1f kg".format(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StagnationCard(netChangeKg: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Stagnation détectée", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
            Text(
                "Variation de %+.1f kg sur les 14 derniers jours.".format(netChangeKg),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Envisage d'ajuster : -100 kcal / jour ou +1000 pas / jour.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BmiCard(state: WeightUiState) {
    val bmi = state.bmi ?: return
    val cat = state.bmiCategory ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("IMC", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f".format(bmi), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(cat.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun BmiCategory.label() = when (this) {
    BmiCategory.UNDERWEIGHT -> "Maigreur"
    BmiCategory.NORMAL -> "Corpulence normale"
    BmiCategory.OVERWEIGHT -> "Surpoids"
    BmiCategory.OBESITY_CLASS_I -> "Obésité I"
    BmiCategory.OBESITY_CLASS_II -> "Obésité II"
    BmiCategory.OBESITY_CLASS_III -> "Obésité III"
}

@Composable
private fun HistoryList(state: WeightUiState, onDelete: (Long) -> Unit) {
    Text("Historique", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.entries.asReversed().take(20).forEach { e ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("%.1f kg".format(e.weightKg), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(e.date.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onDelete(e.id) }.padding(8.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Dialog de saisie
// ─────────────────────────────────────────────────────────────

@Composable
private fun LogWeightDialog(
    initial: Float?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var text by remember { mutableStateOf(initial?.let { "%.1f".format(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle pesée") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.imePadding(),
            ) {
                NumericField(
                    label = "Poids",
                    valueText = text,
                    suffix = "kg",
                    onValueChange = { text = it },
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSaving) "Enregistrement..." else "Enregistrer",
                enabled = !isSaving && text.toFloatOrNull() != null,
                onClick = { text.toFloatOrNull()?.let(onConfirm) },
                modifier = Modifier,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
