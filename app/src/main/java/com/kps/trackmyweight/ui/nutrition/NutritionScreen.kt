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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.FoodPortionAliasEntity
import com.kps.trackmyweight.data.db.enums.CookingMethod
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.FoodCategory
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
    var showFavPicker by remember { mutableStateOf<MealType?>(null) }

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
                    hasFavorites = state.favorites.isNotEmpty(),
                    onAdd = { showAddDialog = type },
                    onOpenFavorites = { showFavPicker = type },
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
            loadAliases = { id -> vm.aliasesFor(id) },
            onDismiss = { showAddDialog = null },
            onConfirm = { foodId, mode, qty, cook ->
                vm.addEntry(currentDialog, foodId, mode, qty, cook)
            },
        )
    }

    val currentFav = showFavPicker
    if (currentFav != null) {
        FavoritePickerDialog(
            favorites = state.favorites,
            onDismiss = { showFavPicker = null },
            onPick = { favId ->
                vm.applyFavorite(currentFav, favId)
                showFavPicker = null
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aujourd'hui",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                phase?.let { PhaseBadge(it.phase, it.targetKcal, state.tdeeEstimate) }
            }
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
private fun PhaseBadge(phase: DietPhaseKind, targetKcal: Int, tdee: Int?) {
    val delta = tdee?.let { targetKcal - it }
    val label = phase.labelFr()
    val subtitle = when {
        delta == null -> null
        delta <= -50 -> "-${-delta} kcal / jour"
        delta >= 50 -> "+$delta kcal / jour"
        else -> "maintenance"
    }
    val bg = when (phase) {
        DietPhaseKind.CUT_MODERATE, DietPhaseKind.CUT_AGGRESSIVE -> MaterialTheme.colorScheme.tertiary
        DietPhaseKind.BULK_LEAN, DietPhaseKind.BULK_STANDARD -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = bg, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun DietPhaseKind.labelFr() = when (this) {
    DietPhaseKind.CUT_MODERATE -> "Déficit modéré"
    DietPhaseKind.CUT_AGGRESSIVE -> "Déficit agressif"
    DietPhaseKind.RECOMP -> "Recomposition"
    DietPhaseKind.MAINTENANCE -> "Maintenance"
    DietPhaseKind.BULK_LEAN -> "Prise sèche"
    DietPhaseKind.BULK_STANDARD -> "Prise de masse"
    DietPhaseKind.REFEED -> "Refeed"
    DietPhaseKind.DIET_BREAK -> "Diet break"
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
    hasFavorites: Boolean,
    onAdd: () -> Unit,
    onOpenFavorites: () -> Unit,
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
                if (hasFavorites) {
                    TextButton(onClick = onOpenFavorites) { Text("★") }
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text("Ajouter")
                }
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
    loadAliases: suspend (Long) -> List<FoodPortionAliasEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, PortionMode, Float, CookingMethod?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodEntity>>(emptyList()) }
    var picked by remember { mutableStateOf<FoodEntity?>(null) }
    var aliases by remember { mutableStateOf<List<FoodPortionAliasEntity>>(emptyList()) }
    var mode by remember { mutableStateOf(PortionMode.SERVING) }
    var quantityText by remember { mutableStateOf("1") }
    var cooking by remember { mutableStateOf<CookingMethod?>(null) }
    var addedCount by remember { mutableStateOf(0) }

    LaunchedEffect(query) { results = search(query) }
    LaunchedEffect(picked?.id) {
        val f = picked
        aliases = if (f != null) loadAliases(f.id) else emptyList()
        mode = PortionMode.SERVING
        quantityText = "1"
        cooking = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (addedCount == 0) "Ajouter à ${mealType.label()}"
                else "Ajouter à ${mealType.label()} ($addedCount déjà ajouté${if (addedCount > 1) "s" else ""})"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(560.dp)) {
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
                    val food = picked!!
                    Text(food.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("Comment mesurer ta portion ?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        buildPortionOptions(food, aliases).forEach { (m, label) ->
                            ChoiceTile(title = label, selected = mode == m, onClick = { mode = m })
                        }
                    }
                    NumericField(
                        label = if (mode == PortionMode.PRECISE_G) "Grammes" else "Quantité",
                        valueText = quantityText,
                        onValueChange = { quantityText = it },
                    )

                    Text("Cuisson", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Ajoute uniquement si tu as ajouté de l'huile (sauté = +5% huile, frit = +10%).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CookingChip("Nature", cooking == null) { cooking = null }
                        CookingChip("Sauté", cooking == CookingMethod.SAUTEED) { cooking = CookingMethod.SAUTEED }
                        CookingChip("Frit", cooking == CookingMethod.FRIED) { cooking = CookingMethod.FRIED }
                    }
                }
            }
        },
        confirmButton = {
            if (picked != null) {
                PrimaryButton(
                    text = "Ajouter",
                    onClick = {
                        val q = quantityText.toFloatOrNull() ?: return@PrimaryButton
                        onConfirm(picked!!.id, mode, q, cooking)
                        // Reste dans le dialogue pour empiler d'autres aliments (café + sucre + lait...)
                        addedCount += 1
                        picked = null
                        query = ""
                    },
                    enabled = quantityText.toFloatOrNull() != null,
                )
            } else {
                PrimaryButton(text = if (addedCount > 0) "Terminer" else "Fermer", onClick = onDismiss)
            }
        },
        dismissButton = {
            if (picked != null) TextButton(onClick = { picked = null }) { Text("← Retour") }
            else if (addedCount == 0) TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/**
 * Construit la liste ordonnée des modes de portion à proposer pour cet aliment :
 * 1) SERVING avec le label naturel de l'aliment (ex "1 boule")
 * 2) Aliases spécifiques déclarés dans FoodSeed (ex "1 morceau = 4g" pour le sucre)
 * 3) PRECISE_G (grammes exacts)
 * 4) Repli générique adapté à la catégorie (louche, cuillère, poing, poignée, ...)
 */
private fun buildPortionOptions(
    food: FoodEntity,
    aliases: List<FoodPortionAliasEntity>,
): List<Pair<PortionMode, String>> {
    val out = mutableListOf<Pair<PortionMode, String>>()
    out += PortionMode.SERVING to
        "Portion standard (${food.servingLabel ?: "${food.defaultServingG.toInt()} g"})"

    val aliasByMode = aliases.associateBy { it.mode }
    // Ordre naturel : PIECE, UNIT, SLICE, CUP, GLASS, BOWL, PLATE, SPOON_*, LADLE_*, HANDFUL
    val preferred = listOf(
        PortionMode.PIECE, PortionMode.UNIT, PortionMode.SLICE,
        PortionMode.CUP, PortionMode.GLASS, PortionMode.BOWL, PortionMode.PLATE,
        PortionMode.SPOON_TABLE, PortionMode.SPOON_TEA,
        PortionMode.LADLE_LARGE, PortionMode.LADLE_SMALL,
        PortionMode.HANDFUL, PortionMode.CUPPED_HAND,
        PortionMode.PALM, PortionMode.FIST, PortionMode.THUMB,
    )
    preferred.forEach { m ->
        val alias = aliasByMode[m]
        if (alias != null) out += m to "${m.labelFr()} (~${alias.equivalentG.toInt()} g)"
    }
    out += PortionMode.PRECISE_G to "Grammes précis"

    // Modes génériques utiles selon la catégorie (proposés seulement si pas déjà déclarés en alias)
    val fallback = when (food.category) {
        FoodCategory.PROTEIN_ANIMAL, FoodCategory.PROTEIN_PLANT -> listOf(PortionMode.PALM)
        FoodCategory.GRAIN -> listOf(PortionMode.FIST, PortionMode.LADLE_LARGE)
        FoodCategory.FAT -> listOf(PortionMode.THUMB, PortionMode.SPOON_TEA, PortionMode.SPOON_TABLE)
        FoodCategory.VEGETABLE, FoodCategory.FRUIT -> listOf(PortionMode.HANDFUL)
        FoodCategory.SAUCE -> listOf(PortionMode.LADLE_LARGE, PortionMode.LADLE_SMALL, PortionMode.SPOON_TABLE)
        FoodCategory.BEVERAGE -> listOf(PortionMode.GLASS, PortionMode.CUP)
        FoodCategory.SNACK -> listOf(PortionMode.HANDFUL, PortionMode.CUPPED_HAND)
        else -> emptyList()
    }
    fallback.forEach { m ->
        if (aliasByMode[m] == null && out.none { it.first == m }) {
            out += m to m.labelFr()
        }
    }
    return out
}

@Composable
private fun FavoritePickerDialog(
    favorites: List<com.kps.trackmyweight.data.db.entity.FavoriteMealEntity>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mes repas favoris") },
        text = {
            if (favorites.isEmpty()) {
                Text(
                    "Aucun favori pour l'instant. Enregistre un repas comme favori pour le réutiliser en un tap.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    modifier = Modifier.height(400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    favorites.forEach { fav ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(fav.id) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Utilisé ${fav.usageCount}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun CookingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

internal fun PortionMode.labelFr() = when (this) {
    PortionMode.PRECISE_G -> "Grammes précis"
    PortionMode.SERVING -> "Portion standard"
    PortionMode.PALM -> "Paume (protéine)"
    PortionMode.FIST -> "Poing (féculent)"
    PortionMode.THUMB -> "Pouce (matière grasse)"
    PortionMode.CUPPED_HAND -> "Main en coupe"
    PortionMode.HANDFUL -> "Poignée"
    PortionMode.LADLE_SMALL -> "Petite louche"
    PortionMode.LADLE_LARGE -> "Grande louche"
    PortionMode.SPOON_TEA -> "Cuillère à café"
    PortionMode.SPOON_TABLE -> "Cuillère à soupe"
    PortionMode.UNIT -> "Unité"
    PortionMode.CUP -> "Tasse"
    PortionMode.GLASS -> "Verre"
    PortionMode.BOWL -> "Bol"
    PortionMode.PIECE -> "Morceau"
    PortionMode.PLATE -> "Assiette"
    PortionMode.SLICE -> "Tranche"
}

private fun MealType.label() = when (this) {
    MealType.BREAKFAST -> "Petit-déj"
    MealType.LUNCH -> "Déjeuner"
    MealType.DINNER -> "Dîner"
    MealType.SNACK -> "Collation"
    MealType.PRE_WORKOUT -> "Pré-workout"
    MealType.POST_WORKOUT -> "Post-workout"
}
