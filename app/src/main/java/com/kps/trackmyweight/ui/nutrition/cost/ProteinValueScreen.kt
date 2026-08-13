package com.kps.trackmyweight.ui.nutrition.cost

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.ProteinValueRow
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.theme.tabular

/**
 * Quelle source de protéine rapporte le plus, à budget donné.
 */
@Composable
fun ProteinValueScreen(
    onBack: () -> Unit,
    vm: ProteinValueViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackHeader(title = "Coût protéique", onBack = onBack)

            Text(
                "Combien te coûte 1 g de protéine, aliment par aliment. " +
                    "Renseigne le prix de ce que tu achètes : le classement se construit " +
                    "à partir de tes vrais prix, pas d'une moyenne.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.message?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { vm.clearMessage() },
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            if (state.ranking.isEmpty()) {
                Text(
                    "Aucun prix renseigné pour l'instant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (state.isComparable) {
                    ComparisonHeadline(
                        best = state.best!!,
                        worst = state.worst!!,
                        currency = state.currency,
                    )
                }
                Section("Classement") {
                    Text(
                        "Touche une ligne pour mettre son prix à jour.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val maxCost = state.ranking.maxOf { it.costPerGramProtein }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.ranking.forEachIndexed { index, row ->
                            RankingRow(
                                position = index + 1,
                                row = row,
                                maxCost = maxCost,
                                currency = state.currency,
                                onClick = { vm.startRepricing(row.foodId) },
                            )
                        }
                    }
                }
            }

            if (state.withoutPrice.isNotEmpty()) {
                Section("Sans prix renseigné") {
                    Text(
                        "Touche un aliment pour saisir ce que tu l'as payé.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.withoutPrice.forEach { food ->
                            UnpricedRow(food = food, onClick = { vm.startPricing(food) })
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    state.draft?.let { draft ->
        PriceDialog(
            draft = draft,
            currency = state.currency,
            onPrice = vm::setPrice,
            onQuantity = vm::setQuantity,
            onDismiss = vm::cancelPricing,
            onSave = vm::savePrice,
        )
    }
}

/**
 * L'écart entre le meilleur et le pire est l'information la plus actionnable :
 * c'est ce qui dit s'il y a un arbitrage à faire.
 */
@Composable
private fun ComparisonHeadline(best: ProteinValueRow, worst: ProteinValueRow, currency: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Meilleur rapport",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(best.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "%.1f %s par gramme de protéine".format(best.costPerGramProtein, currency),
                style = MaterialTheme.typography.bodyMedium.tabular(),
            )
            if (best.costPerGramProtein > 0f) {
                val ratio = worst.costPerGramProtein / best.costPerGramProtein
                Text(
                    "%.1f fois moins cher que %s, le moins avantageux de ta liste."
                        .format(ratio, worst.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RankingRow(
    position: Int,
    row: ProteinValueRow,
    maxCost: Float,
    currency: String,
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
                "$position.",
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "%.1f g de protéines / 100 g".format(row.proteinPer100g),
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "%.1f %s/g".format(row.costPerGramProtein, currency),
                style = MaterialTheme.typography.bodyMedium.tabular(),
                fontWeight = FontWeight.Medium,
            )
        }
        LinearProgressIndicator(
            progress = { if (maxCost > 0f) row.costPerGramProtein / maxCost else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun UnpricedRow(food: FoodEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(food.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "%.0f g/100 g".format(food.proteinPer100g),
            style = MaterialTheme.typography.labelSmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PriceDialog(
    draft: PriceDraft,
    currency: String,
    onPrice: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    // Apercu immediat : l'utilisateur voit le resultat avant d'enregistrer.
    val preview = draft.priceValue?.let { price ->
        draft.quantityValue?.let { grams ->
            val proteinInPortion = draft.food.proteinPer100g * grams / 100f
            if (proteinInPortion > 0f) price / proteinInPortion else null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(draft.food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Combien as-tu payé, et pour quelle quantité ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumericField(
                            label = "Prix",
                            valueText = draft.price,
                            suffix = currency,
                            onValueChange = onPrice,
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        NumericField(
                            label = "Quantité",
                            valueText = draft.quantityG,
                            suffix = "g",
                            onValueChange = onQuantity,
                        )
                    }
                }
                Text(
                    "%.1f g de protéines pour 100 g".format(draft.food.proteinPer100g),
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                preview?.let {
                    Text(
                        "Soit %.1f %s par gramme de protéine.".format(it, currency),
                        style = MaterialTheme.typography.bodyLarge.tabular(),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(text = "Enregistrer", enabled = draft.isValid, onClick = onSave)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
