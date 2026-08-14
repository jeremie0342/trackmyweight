package com.kps.trackmyweight.ui.workout.pain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kps.trackmyweight.data.db.enums.PainArea
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SelectableChip
import com.kps.trackmyweight.ui.common.TextField
import com.kps.trackmyweight.ui.common.labelFr
import com.kps.trackmyweight.ui.common.painIntensityLabelFr
import kotlin.math.roundToInt

/** Ce que l'utilisateur signale. */
data class PainDraft(
    val area: PainArea,
    val intensity: Int,
    val notes: String,
)

/**
 * Signalement d'une douleur.
 *
 * Volontairement rapide à remplir : on le déclenche au milieu d'une série,
 * pas au calme. Zone, intensité, et c'est tout — la note est optionnelle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PainDialog(
    contextExerciseName: String? = null,
    initialArea: PainArea = PainArea.LOWER_BACK,
    onDismiss: () -> Unit,
    onConfirm: (PainDraft) -> Unit,
) {
    var area by remember { mutableStateOf(initialArea) }
    var intensity by remember { mutableFloatStateOf(4f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signaler une douleur") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                contextExerciseName?.let {
                    Text(
                        "Pendant : $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    "Zone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PainArea.entries.forEach { candidate ->
                        SelectableChip(
                            label = candidate.labelFr(),
                            selected = candidate == area,
                            onClick = { area = candidate },
                        )
                    }
                }

                Text(
                    "Intensité",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${intensity.roundToInt()}/10 · ${painIntensityLabelFr(intensity.roundToInt())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0f..10f,
                    steps = 9,
                )

                TextField(label = "Note (optionnel)", value = notes, onValueChange = { notes = it })
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Enregistrer",
                onClick = { onConfirm(PainDraft(area, intensity.roundToInt(), notes)) },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
