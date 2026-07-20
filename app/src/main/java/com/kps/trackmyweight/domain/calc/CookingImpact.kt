package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.CookingMethod

/**
 * Ajuste un [MacroSnapshot] en fonction de la méthode de cuisson.
 *
 * Le seul mode qui modifie significativement les macros par rapport à l'aliment
 * "tel quel" en base est la friture (huile absorbée). Les autres méthodes (bouilli,
 * vapeur, grillé, four) sont considérées comme neutres — les aliments en base
 * sont supposés déjà décrits dans un mode "prêt à consommer" standard.
 *
 * Valeurs indicatives issues de la littérature (variables selon huile, temps, aliment) :
 *  - SAUTEED : ~5% du poids en huile absorbée
 *  - FRIED : ~10% du poids en huile absorbée
 *
 * 1g d'huile ≈ 9 kcal, 1g de lipides.
 */
object CookingImpact {

    fun apply(snap: MacroSnapshot, method: CookingMethod?, grams: Float): MacroSnapshot {
        val oilFactor = when (method) {
            CookingMethod.SAUTEED -> 0.05f
            CookingMethod.FRIED -> 0.10f
            else -> 0f
        }
        if (oilFactor == 0f) return snap
        val absorbedOilG = grams * oilFactor
        return snap.copy(
            kcal = snap.kcal + absorbedOilG * 9f,
            fatsG = snap.fatsG + absorbedOilG,
        )
    }
}
