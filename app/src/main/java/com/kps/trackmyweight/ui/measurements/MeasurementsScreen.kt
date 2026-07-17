package com.kps.trackmyweight.ui.measurements

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
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.domain.calc.WhtrCategory
import com.kps.trackmyweight.ui.common.NumericField

@Composable
fun MeasurementsScreen(
    onBack: () -> Unit = {},
    vm: MeasurementsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::save,
                icon = { Icon(Icons.Outlined.Check, null) },
                text = { Text(if (state.isSaving) "Enregistrement..." else "Enregistrer") },
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
            com.kps.trackmyweight.ui.common.BackHeader(title = "Mensurations", onBack = onBack)

            state.lastComposition?.let { CompositionCard(it) }
            state.whtr?.let { WhtrCard(it, state.whtrCategory) }
            LastVsCurrentHint(state)

            FormSection("Tronc") {
                DraftField("Cou", state.draft.neck) { v -> vm.updateDraft { it.copy(neck = v) } }
                DraftField("Épaules", state.draft.shoulder) { v -> vm.updateDraft { it.copy(shoulder = v) } }
                DraftField("Poitrine", state.draft.chest) { v -> vm.updateDraft { it.copy(chest = v) } }
                DraftField("Taille (ventre)", state.draft.waist) { v -> vm.updateDraft { it.copy(waist = v) } }
                DraftField("Hanches", state.draft.hip) { v -> vm.updateDraft { it.copy(hip = v) } }
            }

            FormSection("Bras") {
                DraftField("Bras G", state.draft.armLeft) { v -> vm.updateDraft { it.copy(armLeft = v) } }
                DraftField("Bras D", state.draft.armRight) { v -> vm.updateDraft { it.copy(armRight = v) } }
                DraftField("Avant-bras G", state.draft.forearmLeft) { v -> vm.updateDraft { it.copy(forearmLeft = v) } }
                DraftField("Avant-bras D", state.draft.forearmRight) { v -> vm.updateDraft { it.copy(forearmRight = v) } }
                DraftField("Poignet", state.draft.wrist) { v -> vm.updateDraft { it.copy(wrist = v) } }
            }

            FormSection("Jambes") {
                DraftField("Cuisse G", state.draft.thighLeft) { v -> vm.updateDraft { it.copy(thighLeft = v) } }
                DraftField("Cuisse D", state.draft.thighRight) { v -> vm.updateDraft { it.copy(thighRight = v) } }
                DraftField("Mollet G", state.draft.calfLeft) { v -> vm.updateDraft { it.copy(calfLeft = v) } }
                DraftField("Mollet D", state.draft.calfRight) { v -> vm.updateDraft { it.copy(calfRight = v) } }
            }

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun DraftField(label: String, value: String, onChange: (String) -> Unit) {
    NumericField(label = label, valueText = value, suffix = "cm", onValueChange = onChange)
}

@Composable
private fun CompositionCard(comp: com.kps.trackmyweight.data.db.entity.BodyCompositionSnapshotEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Composition (Navy)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f%%".format(comp.bodyFatPct), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text("de masse grasse", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Muscle : %.1f kg  •  Gras : %.1f kg".format(comp.leanMassKg, comp.fatMassKg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WhtrCard(whtr: Float, cat: WhtrCategory?) {
    val label = when (cat) {
        WhtrCategory.UNDERWEIGHT -> "Maigre"
        WhtrCategory.HEALTHY -> "Sain"
        WhtrCategory.OVERWEIGHT -> "Surpoids abdominal"
        WhtrCategory.ABDOMINAL_OBESITY -> "Obésité abdominale"
        null -> "-"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("WHtR (taille / hauteur)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.2f".format(whtr), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.padding(horizontal = 8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LastVsCurrentHint(state: MeasurementsUiState) {
    val last = state.lastSession ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            "Dernière session : ${last.date}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
