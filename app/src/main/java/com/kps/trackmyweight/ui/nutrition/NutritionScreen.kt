package com.kps.trackmyweight.ui.nutrition

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.repository.MealWithEntries
import com.kps.trackmyweight.domain.calc.DistributionQuality
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.TextField
import kotlinx.coroutines.launch

@Composable
fun NutritionScreen(vm: NutritionViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var showAddDialog by remember { mutableStateOf<MealType?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = MealType.LUNCH },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Ajouter") },
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
            Spacer(Modifier.height(16.dp))
            Text(
                "Nutrition",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )

            MacrosCard(state)
            state.distribution?.let { DistributionCard(it) }

            listOf(
                MealType.BREAKFAST to "Petit-déjeuner",
                MealType.LUNCH to "Déjeuner",
                MealType.DINNER to "Dîner",
                MealType.SNACK to "Collation",
            ).forEach { (type, label) ->
                MealSection(
                    label = label,
                    meal = state.meals.firstOrNull { it.meal.mealType == type },
                    onAdd = { showAddDialog = type },
                    onDeleteEntry = vm::deleteEntry,
                )
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    val currentDialog = showAddDialog
    if (currentDialog != null) {
        AddEntryDialog(
            mealType = currentDialog,
            search = { q -> vm.searchFoods(q) },
            onDismiss = { showAddDialog = null },
            onConfirm = { foodId, mode, qty ->
                vm.addEntry(currentDialog, foodId, mode, qty)
                showAddDialog = null
            },
        )
    }
}

@Composable
private fun MacrosCard(state: NutritionUiState) {
    val phase = state.phase
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Aujourd'hui", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.0f".format(state.kcalConsumed), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    "/ ${phase?.targetKcal?.toString() ?: "-"} kcal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (phase != null) {
                val kcalProgress = (state.kcalConsumed / phase.targetKcal.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { kcalProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                MacroLine("Protéines", state.proteinConsumed, phase.targetProteinG.toFloat(), "g")
                MacroLine("Glucides", state.carbsConsumed, phase.targetCarbsG.toFloat(), "g")
                MacroLine("Lipides", state.fatsConsumed, phase.targetFatsG.toFloat(), "g")
                MacroLine("Fibres", state.fiberConsumed, 30f, "g")
            }
        }
    }
}

@Composable
private fun MacroLine(label: String, current: Float, target: Float, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%.0f / %.0f $unit".format(current, target), style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { (current / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun DistributionCard(v: com.kps.trackmyweight.domain.calc.DistributionVerdict) {
    val color = when (v.quality) {
        DistributionQuality.EXCELLENT -> MaterialTheme.colorScheme.primary
        DistributionQuality.GOOD -> MaterialTheme.colorScheme.primary
        DistributionQuality.UNBALANCED -> MaterialTheme.colorScheme.tertiary
        DistributionQuality.INSUFFICIENT -> MaterialTheme.colorScheme.error
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Distribution protéines", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(v.quality.name, style = MaterialTheme.typography.titleMedium, color = color)
            Text(v.advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MealSection(
    label: String,
    meal: MealWithEntries?,
    onAdd: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    meal?.let { "%.0f kcal · %.0fg prot".format(it.totalKcal, it.totalProteinG) } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Text(" +") }
            }
            meal?.entries?.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.food?.name ?: "(supprimé)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "%.0f g · %.0f kcal · %.0fg prot".format(e.entry.resolvedGrams, e.entry.snapKcal, e.entry.snapProteinG),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDeleteEntry(e.entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    mealType: MealType,
    search: suspend (String) -> List<FoodEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, PortionMode, Float) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodEntity>>(emptyList()) }
    var picked by remember { mutableStateOf<FoodEntity?>(null) }
    var mode by remember { mutableStateOf(PortionMode.SERVING) }
    var quantityText by remember { mutableStateOf("1") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        results = search(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter à ${mealType.label()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(500.dp)) {
                if (picked == null) {
                    TextField(label = "Rechercher un aliment", value = query, onValueChange = { query = it })
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        results.take(50).forEach { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { picked = f }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(f.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "%.0f kcal · %.1fg prot / 100g".format(f.kcalPer100g, f.proteinPer100g),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (results.isEmpty() && query.isNotBlank()) {
                            Text("Aucun résultat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text(picked!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("Mode de portion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            PortionMode.SERVING to "Portion standard (${picked!!.servingLabel ?: "${picked!!.defaultServingG.toInt()} g"})",
                            PortionMode.PRECISE_G to "Grammes précis",
                            PortionMode.PALM to "Paume (protéine)",
                            PortionMode.FIST to "Poing (féculent)",
                            PortionMode.THUMB to "Pouce (matière grasse)",
                            PortionMode.LADLE_LARGE to "Grande louche",
                            PortionMode.SPOON_TABLE to "Cuillère à soupe",
                        ).forEach { (m, label) ->
                            ChoiceTile(
                                title = label,
                                selected = mode == m,
                                onClick = { mode = m },
                            )
                        }
                    }
                    NumericField(
                        label = if (mode == PortionMode.PRECISE_G) "Grammes" else "Quantité",
                        valueText = quantityText,
                        onValueChange = { quantityText = it },
                    )
                }
            }
        },
        confirmButton = {
            if (picked != null) {
                PrimaryButton(
                    text = "Ajouter",
                    onClick = {
                        val q = quantityText.toFloatOrNull() ?: return@PrimaryButton
                        onConfirm(picked!!.id, mode, q)
                    },
                    enabled = quantityText.toFloatOrNull() != null,
                )
            } else {
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
        dismissButton = {
            if (picked != null) TextButton(onClick = { picked = null }) { Text("← Retour") }
        },
    )
}

private fun MealType.label() = when (this) {
    MealType.BREAKFAST -> "Petit-déj"
    MealType.LUNCH -> "Déjeuner"
    MealType.DINNER -> "Dîner"
    MealType.SNACK -> "Collation"
    MealType.PRE_WORKOUT -> "Pré-workout"
    MealType.POST_WORKOUT -> "Post-workout"
}
