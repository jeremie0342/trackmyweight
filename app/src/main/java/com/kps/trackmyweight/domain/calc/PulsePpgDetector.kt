package com.kps.trackmyweight.domain.calc

import kotlin.math.max
import kotlin.math.min

/**
 * Estimation du BPM à partir d'un signal PPG (variation de luminance sur le doigt
 * posé sur le capteur photo + flash allumé). Pure Kotlin, sans dépendance Android.
 *
 * Algorithme :
 *  1) Détrendage (soustraction d'une moyenne glissante ~1 s) pour retirer la dérive lente
 *  2) Détection des pics locaux positifs, avec distance minimale (max 200 BPM)
 *  3) BPM = 60 / intervalle moyen entre pics
 *
 * Renvoie null si signal trop court, trop bruité, ou hors plage physiologique (40–200 BPM).
 */
object PulsePpgDetector {

    data class Verdict(
        val bpm: Int,
        val quality: Float,     // 0..1, écart-type des intervalles (bas = fiable)
        val peaksDetected: Int,
    )

    fun estimate(samples: List<Float>, sampleRateHz: Float): Verdict? {
        if (samples.size < (sampleRateHz * 5f).toInt()) return null   // ≥ 5 s

        // 1) Détrendage : soustrait la moyenne glissante sur ~1 s
        val window = sampleRateHz.toInt().coerceAtLeast(3)
        val detrended = FloatArray(samples.size)
        for (i in samples.indices) {
            val from = max(0, i - window / 2)
            val to = min(samples.size, i + window / 2 + 1)
            var sum = 0.0
            for (j in from until to) sum += samples[j]
            detrended[i] = samples[i] - (sum / (to - from)).toFloat()
        }

        // 2) Pics : max local strictement positif, distance mini ~= 0.3 s (max 200 BPM)
        val minDistance = max((sampleRateHz * 0.3f).toInt(), 3)
        val peaks = mutableListOf<Int>()
        for (i in 1 until detrended.size - 1) {
            val v = detrended[i]
            if (v > 0f && v > detrended[i - 1] && v >= detrended[i + 1]) {
                if (peaks.isEmpty() || i - peaks.last() >= minDistance) {
                    peaks += i
                }
            }
        }
        if (peaks.size < 4) return null

        val intervals = IntArray(peaks.size - 1) { peaks[it + 1] - peaks[it] }
        val avgInterval = intervals.average()
        if (avgInterval <= 0.0) return null

        val bpm = (sampleRateHz * 60f / avgInterval.toFloat()).toInt()
        if (bpm !in 40..200) return null

        // 3) Qualité : écart-type normalisé
        val mean = avgInterval
        val variance = intervals.map { (it - mean) * (it - mean) }.average()
        val std = kotlin.math.sqrt(variance).toFloat()
        val quality = (1f - (std / mean.toFloat()).coerceIn(0f, 1f)).coerceIn(0f, 1f)

        return Verdict(bpm = bpm, quality = quality, peaksDetected = peaks.size)
    }
}
