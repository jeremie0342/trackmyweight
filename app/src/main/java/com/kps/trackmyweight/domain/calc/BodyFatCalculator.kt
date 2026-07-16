package com.kps.trackmyweight.domain.calc

import com.kps.trackmyweight.data.db.enums.Sex
import kotlin.math.log10

data class BodyComposition(
    val bodyFatPct: Float,
    val leanMassKg: Float,
    val fatMassKg: Float,
)

/**
 * Méthode Navy pour estimer le % de masse grasse.
 * Précision ±3% vs DEXA, sans équipement (juste un mètre-ruban).
 *
 * Homme :  495 / (1.0324 − 0.19077 × log10(taille − cou) + 0.15456 × log10(hauteur)) − 450
 * Femme :  495 / (1.29579 − 0.35004 × log10(taille + hanches − cou) + 0.22100 × log10(hauteur)) − 450
 */
object BodyFatCalculator {

    /** Renvoie le % de masse grasse ou `null` si les mesures nécessaires sont manquantes/invalides. */
    fun navyBodyFatPct(
        sex: Sex,
        heightCm: Float,
        neckCm: Float?,
        waistCm: Float?,
        hipCm: Float?,
    ): Float? {
        if (neckCm == null || waistCm == null || heightCm <= 0f) return null
        return when (sex) {
            Sex.MALE -> {
                val delta = waistCm - neckCm
                if (delta <= 0f) return null
                val denom = 1.0324f - 0.19077f * log10(delta) + 0.15456f * log10(heightCm)
                (495f / denom) - 450f
            }
            Sex.FEMALE -> {
                if (hipCm == null) return null
                val delta = waistCm + hipCm - neckCm
                if (delta <= 0f) return null
                val denom = 1.29579f - 0.35004f * log10(delta) + 0.22100f * log10(heightCm)
                (495f / denom) - 450f
            }
        }.let { pct -> if (pct.isFinite() && pct in 2f..70f) pct else null }
    }

    fun compose(
        sex: Sex,
        heightCm: Float,
        weightKg: Float,
        neckCm: Float?,
        waistCm: Float?,
        hipCm: Float?,
    ): BodyComposition? {
        val pct = navyBodyFatPct(sex, heightCm, neckCm, waistCm, hipCm) ?: return null
        val fatMass = weightKg * (pct / 100f)
        val leanMass = weightKg - fatMass
        return BodyComposition(bodyFatPct = pct, leanMassKg = leanMass, fatMassKg = fatMass)
    }
}
