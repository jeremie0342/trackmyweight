package com.kps.trackmyweight.domain.calc

/**
 * Calcule le coût par gramme de protéine, dans la devise donnée.
 * Retourne null si les données sont insuffisantes.
 */
object CostPerProtein {

    /**
     * @param pricePerPortion prix pour une portion (celle décrite par gramsPerPortion)
     * @param gramsPerPortion masse totale de la portion
     * @param proteinPer100g protéines pour 100g de l'aliment brut
     */
    fun compute(
        pricePerPortion: Float?,
        gramsPerPortion: Float?,
        proteinPer100g: Float,
    ): Float? {
        if (pricePerPortion == null || pricePerPortion <= 0f) return null
        if (gramsPerPortion == null || gramsPerPortion <= 0f) return null
        if (proteinPer100g <= 0f) return null
        val proteinInPortion = proteinPer100g * gramsPerPortion / 100f
        if (proteinInPortion <= 0f) return null
        return pricePerPortion / proteinInPortion
    }

    /**
     * Trie un ensemble d'aliments du meilleur (moins cher par g protéine) au pire.
     */
    fun <T> rankAscending(items: List<T>, costOf: (T) -> Float?): List<T> {
        return items
            .mapNotNull { item -> costOf(item)?.let { c -> item to c } }
            .sortedBy { it.second }
            .map { it.first }
    }
}
