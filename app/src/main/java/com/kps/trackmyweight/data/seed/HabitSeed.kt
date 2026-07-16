package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity

object HabitSeed {
    val items: List<HabitDefinitionEntity> = listOf(
        HabitDefinitionEntity(key = "morning_weigh_in", displayName = "Pesée matinale", iconKey = "scale", targetPerWeek = 7, orderIndex = 0),
        HabitDefinitionEntity(key = "creatine", displayName = "Créatine", iconKey = "pill", targetPerWeek = 7, orderIndex = 1),
        HabitDefinitionEntity(key = "steps_10k", displayName = "10 000 pas", iconKey = "walk", targetPerWeek = 5, orderIndex = 2),
        HabitDefinitionEntity(key = "water_2L", displayName = "2 L d'eau", iconKey = "water", targetPerWeek = 7, orderIndex = 3),
        HabitDefinitionEntity(key = "sleep_7h", displayName = "Sommeil ≥ 7h", iconKey = "sleep", targetPerWeek = 6, orderIndex = 4),
        HabitDefinitionEntity(key = "stretch", displayName = "Étirements", iconKey = "stretch", targetPerWeek = 4, orderIndex = 5),
        HabitDefinitionEntity(key = "no_alcohol", displayName = "Sans alcool", iconKey = "no_alcohol", targetPerWeek = 6, orderIndex = 6),
    )
}
