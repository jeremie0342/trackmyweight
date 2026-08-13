package com.kps.trackmyweight.ui.workout.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.SelectableChip
import com.kps.trackmyweight.ui.common.TextField
import com.kps.trackmyweight.ui.common.labelFr

/** Ce que l'utilisateur a saisi pour créer un exercice. */
data class CustomExerciseDraft(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val mechanics: ExerciseMechanics,
    val force: ExerciseForce,
    /**
     * Équipements requis. Sans eux, l'exercice passe toujours le filtre « ma
     * salle » — il serait proposé même dans une salle qui n'a pas la machine.
     */
    val equipmentIds: List<Long> = emptyList(),
)

/**
 * Création d'un exercice absent du catalogue.
 *
 * Le catalogue couvre 200 mouvements, mais il ne couvrira jamais les variantes
 * maison ni les machines exotiques d'une salle donnée. Sans cette porte de
 * sortie, un exercice manquant est un cul-de-sac.
 *
 * Le matériel requis est optionnel mais proposé : sans lui, l'exercice
 * échappe au filtre par salle et serait suggéré partout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateExerciseDialog(
    initialName: String,
    availableEquipment: List<EquipmentEntity>,
    onDismiss: () -> Unit,
    onCreate: (CustomExerciseDraft) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var mechanics by remember { mutableStateOf(ExerciseMechanics.ISOLATION) }
    var force by remember { mutableStateOf(ExerciseForce.PUSH) }
    val selectedEquipment = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel exercice") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            ) {
                TextField(label = "Nom", value = name, onValueChange = { name = it })

                FieldLabel("Muscle principal")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MuscleGroup.entries.forEach { candidate ->
                        SelectableChip(
                            label = candidate.labelFr(),
                            selected = candidate == muscle,
                            onClick = { muscle = candidate },
                        )
                    }
                }

                FieldLabel("Mécanique")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseMechanics.entries.forEach { candidate ->
                        SelectableChip(
                            label = candidate.labelFr(),
                            selected = candidate == mechanics,
                            onClick = { mechanics = candidate },
                        )
                    }
                }

                FieldLabel("Type de mouvement")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExerciseForce.entries.forEach { candidate ->
                        SelectableChip(
                            label = candidate.labelFr(),
                            selected = candidate == force,
                            onClick = { force = candidate },
                        )
                    }
                }

                if (availableEquipment.isNotEmpty()) {
                    FieldLabel("Matériel requis (optionnel)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableEquipment.forEach { equipment ->
                            SelectableChip(
                                label = equipment.displayName,
                                selected = equipment.id in selectedEquipment,
                                onClick = {
                                    if (equipment.id in selectedEquipment) {
                                        selectedEquipment.remove(equipment.id)
                                    } else {
                                        selectedEquipment.add(equipment.id)
                                    }
                                },
                            )
                        }
                    }
                    Text(
                        "Sans matériel renseigné, l'exercice apparaîtra dans toutes tes salles.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Créer",
                enabled = name.isNotBlank(),
                onClick = {
                    onCreate(
                        CustomExerciseDraft(
                            name = name.trim(),
                            primaryMuscle = muscle,
                            mechanics = mechanics,
                            force = force,
                            equipmentIds = selectedEquipment.toList(),
                        )
                    )
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
