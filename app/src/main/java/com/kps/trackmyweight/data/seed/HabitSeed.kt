package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.HabitDefinitionEntity

object HabitSeed {
    val items: List<HabitDefinitionEntity> = listOf(
        HabitDefinitionEntity(key = "morning_weigh_in", displayName = "Pesée matinale", iconKey = "scale", targetPerWeek = 7, orderIndex = 0),
        HabitDefinitionEntity(key = "steps_10k", displayName = "Pas", iconKey = "walk", targetPerWeek = 5, dailyTarget = 10000f, unit = "pas", orderIndex = 1),
        HabitDefinitionEntity(key = "water_2L", displayName = "Eau", iconKey = "water", targetPerWeek = 7, dailyTarget = 2.5f, unit = "L", orderIndex = 2),
        HabitDefinitionEntity(key = "sleep_7h", displayName = "Sommeil", iconKey = "sleep", targetPerWeek = 6, dailyTarget = 7f, unit = "h", orderIndex = 3),
        HabitDefinitionEntity(key = "stretch", displayName = "Étirements", iconKey = "stretch", targetPerWeek = 4, orderIndex = 4),
        HabitDefinitionEntity(key = "no_alcohol", displayName = "Sans alcool", iconKey = "no_alcohol", targetPerWeek = 6, orderIndex = 5),
        HabitDefinitionEntity(key = "creatine", displayName = "Créatine", iconKey = "pill", targetPerWeek = 7, dailyTarget = 5f, unit = "g", isActive = false, orderIndex = 6),
    )
}
