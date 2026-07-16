package com.kps.trackmyweight.domain.calc

data class ReadinessInputs(
    val sleepQuality: Int?,   // 1-5
    val energy: Int?,         // 1-5
    val soreness: Int?,       // 1-5 (5 = pas courbaturé)
    val mood: Int?,           // 1-5
)

enum class ReadinessLevel { POOR, LOW, MODERATE, GOOD, EXCELLENT }

data class ReadinessVerdict(
    val score: Float,             // 0..5
    val level: ReadinessLevel,
    val filledDimensions: Int,    // 0..4
    val advice: String,
)

/**
 * Score de forme du jour à partir d'un check-in matinal de 4 dimensions.
 * Chaque dimension pèse 25%. Si une dimension manque, on redistribue au prorata.
 */
object ReadinessScore {

    fun compute(inputs: ReadinessInputs): ReadinessVerdict {
        val values = listOfNotNull(inputs.sleepQuality, inputs.energy, inputs.soreness, inputs.mood)
        if (values.isEmpty()) {
            return ReadinessVerdict(0f, ReadinessLevel.POOR, 0, "Fais ton check-in matinal pour obtenir un score.")
        }
        val avg = values.average().toFloat()
        val level = when {
            avg < 2f -> ReadinessLevel.POOR
            avg < 2.8f -> ReadinessLevel.LOW
            avg < 3.6f -> ReadinessLevel.MODERATE
            avg < 4.4f -> ReadinessLevel.GOOD
            else -> ReadinessLevel.EXCELLENT
        }
        val advice = when (level) {
            ReadinessLevel.POOR -> "Journée off. Repos actif seulement, dors."
            ReadinessLevel.LOW -> "Réduis l'intensité de 20-30 %. Cardio Z2 ou séance très allégée."
            ReadinessLevel.MODERATE -> "Journée standard, écoute-toi entre les séries."
            ReadinessLevel.GOOD -> "Bonne forme, respecte ton plan."
            ReadinessLevel.EXCELLENT -> "Push day. Vise des PRs sur tes lifts principaux."
        }
        return ReadinessVerdict(avg, level, values.size, advice)
    }
}
