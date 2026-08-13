package com.kps.trackmyweight.data.seed

/**
 * FICHIER GÉNÉRÉ — ne pas éditer à la main.
 * Source de vérité : `scripts/catalog.py` (EQUIPMENT_MEDIA).
 * Régénérer avec : `python scripts/generate_exercise_seed.py`
 *
 * Équipements disposant d'un visuel dans `assets/equipment/<clé>.webp`.
 *
 * Les accessoires sans visuel distinctif (ceinture, sangles, foam roller,
 * micro-poids, barres courtes, haltères réglables) en sont volontairement
 * absents : l'UI se contente alors d'afficher leur nom.
 */
object EquipmentMedia {

    /** Racine des visuels d'équipement dans les assets. */
    const val DIR: String = "equipment"

    private val COVERED: Set<String> = setOf(
        "ab_crunch_machine",
        "ab_wheel",
        "assisted_pullup",
        "back_extension",
        "barbell_ez",
        "barbell_olympic",
        "bench_decline",
        "bench_flat",
        "bench_incline",
        "cable_column_single",
        "cable_crossover",
        "calf_raise_machine",
        "chest_press_machine",
        "dip_bars",
        "dumbbells_fixed",
        "elliptical",
        "gymnastic_rings",
        "hack_squat",
        "jump_rope",
        "kettlebells",
        "lat_pulldown",
        "leg_curl",
        "leg_extension",
        "leg_press",
        "pec_deck",
        "plates_olympic",
        "preacher_bench",
        "pullup_bar",
        "resistance_bands",
        "rowing_machine",
        "seated_row",
        "shoulder_press_machine",
        "smith_machine",
        "squat_rack",
        "stair_master",
        "stationary_bike",
        "treadmill",
        "trx",
    )

    /**
     * Chemin du visuel dans les assets, ou `null` si cet équipement n'en a pas.
     * Les appelants doivent gérer le `null` plutôt que de supposer une image.
     */
    fun pathFor(equipmentKey: String): String? =
        if (equipmentKey in COVERED) "$DIR/$equipmentKey.webp" else null
}
