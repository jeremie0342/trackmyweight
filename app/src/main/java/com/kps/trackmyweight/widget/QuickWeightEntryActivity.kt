package com.kps.trackmyweight.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.theme.TrackMyWeightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activité transparente déclenchée depuis le widget pour un log poids 1-tap.
 * Affiche juste un dialog, log, met à jour le widget, se ferme.
 */
@AndroidEntryPoint
class QuickWeightEntryActivity : ComponentActivity() {

    @Inject lateinit var weightRepo: WeightRepository
    @Inject lateinit var widgetUpdater: WeightWidgetUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackMyWeightTheme {
                Dialog()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Dialog() {
        var text by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { finish() },
            title = { Text("Pesée rapide") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumericField(label = "Poids", valueText = text, suffix = "kg", onValueChange = { text = it })
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = if (isSaving) "Enregistrement..." else "Enregistrer",
                    enabled = !isSaving && text.toFloatOrNull() != null,
                    onClick = {
                        val kg = text.toFloatOrNull() ?: return@PrimaryButton
                        isSaving = true
                        scope.launch {
                            val today = kotlinx.datetime.Clock.System.now()
                                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                            weightRepo.log(today, kg)
                            widgetUpdater.refresh()
                            finish()
                        }
                    },
                )
            },
            dismissButton = { TextButton(onClick = { finish() }) { Text("Annuler") } },
        )
    }
}
