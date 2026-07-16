package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.entity.FoodEntity

data class MacroSnapshot(
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val fiberG: Float,
    val sodiumMg: Float?,
)

/**
 * Calcul des macros à partir d'un aliment et de grammes résolus.
 * Renvoie un snapshot figé qu'on stocke dans MealEntry (immutable même si Food évolue).
 */
object MacroCalculator {

    fun snapshot(food: FoodEntity, resolvedGrams: Float): MacroSnapshot {
        val factor = resolvedGrams / 100f
        return MacroSnapshot(
            kcal = food.kcalPer100g * factor,
            proteinG = food.proteinPer100g * factor,
            carbsG = food.carbsPer100g * factor,
            fatsG = food.fatsPer100g * factor,
            fiberG = food.fiberPer100g * factor,
            sodiumMg = food.sodiumMgPer100g?.let { it * factor },
        )
    }

    fun sum(snapshots: List<MacroSnapshot>): MacroSnapshot = MacroSnapshot(
        kcal = snapshots.sumOf { it.kcal.toDouble() }.toFloat(),
        proteinG = snapshots.sumOf { it.proteinG.toDouble() }.toFloat(),
        carbsG = snapshots.sumOf { it.carbsG.toDouble() }.toFloat(),
        fatsG = snapshots.sumOf { it.fatsG.toDouble() }.toFloat(),
        fiberG = snapshots.sumOf { it.fiberG.toDouble() }.toFloat(),
        sodiumMg = snapshots.mapNotNull { it.sodiumMg }.takeIf { it.isNotEmpty() }?.sum()?.toFloat(),
    )
}
