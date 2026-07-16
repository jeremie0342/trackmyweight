package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.enums.EquipmentCategory

/**
 * Référentiel d'équipement de salle. Seeded au premier lancement.
 * Regroupé par catégorie pour l'affichage en cases à cocher pendant l'onboarding.
 */
object EquipmentSeed {

    val items: List<EquipmentEntity> = listOf(
        // Barres
        eq("barbell_olympic", "Barre olympique 20 kg", EquipmentCategory.BAR),
        eq("barbell_ez", "Barre EZ / curl", EquipmentCategory.BAR),
        eq("barbell_short", "Barres droites courtes", EquipmentCategory.BAR),
        eq("plates_olympic", "Disques olympiques", EquipmentCategory.BAR),
        eq("plates_micro", "Micro-poids (0.5-1.25 kg)", EquipmentCategory.BAR),

        // Haltères
        eq("dumbbells_fixed", "Haltères fixes", EquipmentCategory.DUMBBELL),
        eq("dumbbells_adjustable", "Haltères réglables", EquipmentCategory.DUMBBELL),

        // Kettlebells
        eq("kettlebells", "Kettlebells", EquipmentCategory.KETTLEBELL),

        // Bancs & racks
        eq("bench_flat", "Banc plat", EquipmentCategory.MACHINE),
        eq("bench_incline", "Banc inclinable", EquipmentCategory.MACHINE),
        eq("bench_decline", "Banc décliné", EquipmentCategory.MACHINE),
        eq("squat_rack", "Squat rack / cage", EquipmentCategory.MACHINE),
        eq("smith_machine", "Smith machine", EquipmentCategory.MACHINE),
        eq("preacher_bench", "Banc Larry Scott", EquipmentCategory.MACHINE),

        // Machines
        eq("leg_press", "Leg press", EquipmentCategory.MACHINE),
        eq("hack_squat", "Hack squat", EquipmentCategory.MACHINE),
        eq("leg_curl", "Leg curl", EquipmentCategory.MACHINE),
        eq("leg_extension", "Leg extension", EquipmentCategory.MACHINE),
        eq("calf_raise_machine", "Machine mollets", EquipmentCategory.MACHINE),
        eq("pec_deck", "Pec deck", EquipmentCategory.MACHINE),
        eq("chest_press_machine", "Machine développé pec", EquipmentCategory.MACHINE),
        eq("shoulder_press_machine", "Machine développé épaules", EquipmentCategory.MACHINE),
        eq("lat_pulldown", "Tirage vertical", EquipmentCategory.CABLE),
        eq("seated_row", "Tirage horizontal", EquipmentCategory.CABLE),
        eq("cable_crossover", "Vis-à-vis / poulies croisées", EquipmentCategory.CABLE),
        eq("cable_column_single", "Poulie simple", EquipmentCategory.CABLE),
        eq("assisted_pullup", "Traction assistée", EquipmentCategory.MACHINE),
        eq("ab_crunch_machine", "Machine abdos", EquipmentCategory.MACHINE),
        eq("back_extension", "Banc lombaires (Roman chair)", EquipmentCategory.MACHINE),

        // Poids du corps
        eq("pullup_bar", "Barre de traction", EquipmentCategory.BODYWEIGHT),
        eq("dip_bars", "Barres parallèles (dips)", EquipmentCategory.BODYWEIGHT),
        eq("trx", "TRX / sangles suspension", EquipmentCategory.BODYWEIGHT),
        eq("gymnastic_rings", "Anneaux", EquipmentCategory.BODYWEIGHT),
        eq("resistance_bands", "Élastiques", EquipmentCategory.BODYWEIGHT),

        // Cardio
        eq("treadmill", "Tapis de course", EquipmentCategory.CARDIO),
        eq("stationary_bike", "Vélo d'appartement", EquipmentCategory.CARDIO),
        eq("rowing_machine", "Rameur", EquipmentCategory.CARDIO),
        eq("elliptical", "Elliptique", EquipmentCategory.CARDIO),
        eq("jump_rope", "Corde à sauter", EquipmentCategory.CARDIO),
        eq("stair_master", "Stair master", EquipmentCategory.CARDIO),

        // Accessoires
        eq("weight_belt", "Ceinture de force", EquipmentCategory.ACCESSORY),
        eq("wrist_straps", "Sangles de tirage", EquipmentCategory.ACCESSORY),
        eq("ab_wheel", "Roue abdominale", EquipmentCategory.ACCESSORY),
        eq("foam_roller", "Foam roller", EquipmentCategory.ACCESSORY),
    )

    private fun eq(key: String, name: String, category: EquipmentCategory) =
        EquipmentEntity(key = key, displayName = name, category = category)
}
