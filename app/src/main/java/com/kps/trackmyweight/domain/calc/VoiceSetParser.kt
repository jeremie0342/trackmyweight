package com.kps.trackmyweight.domain.calc

data class ParsedVoiceSet(val reps: Int, val weightKg: Float)

/**
 * Parse une transcription voix française en (reps, poids kg).
 * Formats acceptés (insensible à la casse) :
 *  "12 reps a 80 kilos", "12 reps à 80 kg", "12 à 80", "12 fois 80"
 *  "80 kilos 12 fois", "80 kg 12"
 *  Les décimales avec virgule ou point (82,5 = 82.5)
 */
object VoiceSetParser {

    private val patterns: List<Regex> = listOf(
        // 12 (reps)? (à|a|fois) 80 (kg|kilo|kilos)?
        Regex("""(\d+)\s*(?:reps?)?\s*(?:à|a|fois|x)\s*(\d+(?:[.,]\d+)?)\s*(?:k(?:g|ilo(?:s)?))?""", RegexOption.IGNORE_CASE),
        // 80 (kg)? 12 (reps|fois)?
        Regex("""(\d+(?:[.,]\d+)?)\s*(?:k(?:g|ilo(?:s)?))\s*(\d+)\s*(?:reps?|fois)?""", RegexOption.IGNORE_CASE),
    )

    fun parse(input: String): ParsedVoiceSet? {
        val cleaned = input.trim().replace(",", ".")
        patterns.forEachIndexed { i, regex ->
            val m = regex.find(cleaned) ?: return@forEachIndexed
            val (a, b) = m.destructured
            return when (i) {
                0 -> ParsedVoiceSet(reps = a.toInt(), weightKg = b.toFloat())
                1 -> ParsedVoiceSet(reps = b.toInt(), weightKg = a.toFloat())
                else -> null
            }
        }
        return null
    }
}
