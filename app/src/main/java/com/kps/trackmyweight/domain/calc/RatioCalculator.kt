package com.kps.trackmyweight.domain.calc

/**
 * WHtR (Waist-to-Height Ratio) et WHR (Waist-to-Hip Ratio).
 *
 * WHtR est plus prédictif de la santé cardiovasculaire que l'IMC.
 * Seuils de risque (adulte homme) :
 *   < 0.40 : maigre / attention
 *   0.40 - 0.49 : sain
 *   0.50 - 0.54 : surpoids
 *   ≥ 0.55 : obésité abdominale
 */
enum class WhtrCategory { UNDERWEIGHT, HEALTHY, OVERWEIGHT, ABDOMINAL_OBESITY }

object RatioCalculator {

    fun whtr(waistCm: Float?, heightCm: Float): Float? {
        if (waistCm == null || waistCm <= 0f || heightCm <= 0f) return null
        return waistCm / heightCm
    }

    fun categorizeWhtr(ratio: Float): WhtrCategory = when {
        ratio < 0.40f -> WhtrCategory.UNDERWEIGHT
        ratio < 0.50f -> WhtrCategory.HEALTHY
        ratio < 0.55f -> WhtrCategory.OVERWEIGHT
        else -> WhtrCategory.ABDOMINAL_OBESITY
    }

    fun whr(waistCm: Float?, hipCm: Float?): Float? {
        if (waistCm == null || hipCm == null || hipCm <= 0f) return null
        return waistCm / hipCm
    }
}
