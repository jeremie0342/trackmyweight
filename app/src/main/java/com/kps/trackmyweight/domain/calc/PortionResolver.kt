package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.PortionMode

/**
 * Convertit une portion (mode + quantité) en grammes réels.
 *
 * Peut s'appuyer :
 *  - Sur un alias spécifique à l'aliment (ex : "1 louche de sauce arachide = 90g")
 *  - Sinon sur des valeurs par défaut selon la catégorie
 *  - Pour PRECISE_G : la quantité EST la valeur en grammes
 */
object PortionResolver {

    fun resolveGrams(
        mode: PortionMode,
        quantity: Float,
        aliasGramsForMode: Float? = null,
        defaultServingG: Float? = null,
        category: FoodCategory? = null,
    ): Float {
        if (mode == PortionMode.PRECISE_G) return quantity
        val perUnit = aliasGramsForMode
            ?: defaultGramsFor(mode, category)
            ?: defaultServingG
            ?: 100f
        return quantity * perUnit
    }

    /** Valeurs indicatives par catégorie (approximations "main humaine" universelles). */
    private fun defaultGramsFor(mode: PortionMode, category: FoodCategory?): Float? = when (mode) {
        PortionMode.PALM -> when (category) {
            FoodCategory.PROTEIN_ANIMAL -> 120f
            FoodCategory.PROTEIN_PLANT -> 100f
            else -> 100f
        }
        PortionMode.FIST -> when (category) {
            FoodCategory.GRAIN -> 150f
            FoodCategory.VEGETABLE -> 120f
            FoodCategory.FRUIT -> 150f
            else -> 130f
        }
        PortionMode.THUMB -> 12f            // matière grasse
        PortionMode.CUPPED_HAND -> 30f      // fruits secs, snacks
        PortionMode.HANDFUL -> 30f
        PortionMode.LADLE_SMALL -> 80f
        PortionMode.LADLE_LARGE -> 150f
        PortionMode.SPOON_TEA -> 5f
        PortionMode.SPOON_TABLE -> 15f
        PortionMode.CUP -> 200f              // tasse de café/thé standard
        PortionMode.GLASS -> 250f            // verre d'eau/jus
        PortionMode.BOWL -> 250f             // bol de soupe/bouillie
        PortionMode.PLATE -> when (category) {
            FoodCategory.GRAIN -> 250f
            FoodCategory.VEGETABLE -> 200f
            else -> 300f
        }
        PortionMode.SLICE -> when (category) {
            FoodCategory.GRAIN -> 30f        // tranche de pain
            FoodCategory.FRUIT -> 100f       // tranche de fruit
            else -> 50f
        }
        PortionMode.PIECE -> null           // toujours dépendant de l'aliment (via alias)
        PortionMode.UNIT -> null            // dépend totalement de l'aliment
        PortionMode.SERVING -> null
        PortionMode.PRECISE_G -> null
    }
}
