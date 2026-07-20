package com.kps.trackmyweight.domain.calc

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Estimation du BPM à partir d'un signal PPG (variation de luminance sur le doigt
 * posé sur le capteur photo + flash allumé). Pure Kotlin, sans dépendance Android.
 *
 * Pipeline :
 *  1) Détrendage : moyenne glissante ~1.5 s soustraite (retire dérive lente)
 *  2) Lissage : moyenne mobile courte (~0.2 s) pour tuer le bruit haute fréquence
 *  3) Détection des pics via seuil dynamique + distance mini
 *  4) Filtrage des intervalles aberrants (rejet des outliers > 40% de la médiane)
 *  5) BPM = 60 / médiane des intervalles retenus (plus robuste que la moyenne)
 *
 * Renvoie null si signal trop court, ou hors plage physiologique (40–200 BPM).
 */
object PulsePpgDetector {

    data class Verdict(
        val bpm: Int,
        val quality: Float,     // 0..1, cohérence entre intervalles
        val peaksDetected: Int,
    )

    fun estimate(samples: List<Float>, sampleRateHz: Float): Verdict? {
        val n = samples.size
        if (n < (sampleRateHz * 5f).toInt() || sampleRateHz <= 0f) return null

        // 1) Détrendage : soustrait la moyenne glissante sur ~1.5 s
        val detrendWin = (sampleRateHz * 1.5f).toInt().coerceAtLeast(5)
        val detrended = FloatArray(n)
        var runSum = 0.0
        val q: ArrayDeque<Float> = ArrayDeque(detrendWin)
        for (i in 0 until n) {
            q.addLast(samples[i]); runSum += samples[i]
            if (q.size > detrendWin) runSum -= q.removeFirst()
            detrended[i] = samples[i] - (runSum / q.size).toFloat()
        }

        // 2) Lissage passe-bas courte fenêtre (~0.2 s) pour retirer le bruit
        val smoothWin = max((sampleRateHz * 0.2f).toInt(), 3)
        val smooth = FloatArray(n)
        var sSum = 0f
        val sq: ArrayDeque<Float> = ArrayDeque(smoothWin)
        for (i in 0 until n) {
            sq.addLast(detrended[i]); sSum += detrended[i]
            if (sq.size > smoothWin) sSum -= sq.removeFirst()
            smooth[i] = sSum / sq.size
        }

        // Seuil dynamique : 30 % de l'amplitude positive
        val posMax = smooth.max()
        val posMean = smooth.filter { it > 0f }.average().toFloat().let { if (it.isNaN()) 0f else it }
        val threshold = max(posMean * 0.5f, posMax * 0.3f)

        // 3) Pics : max local strictement > seuil, avec distance mini adaptative
        val minDistance = max((sampleRateHz * 0.35f).toInt(), 3)  // max ~170 BPM
        val peaks = mutableListOf<Int>()
        for (i in 1 until n - 1) {
            val v = smooth[i]
            if (v > threshold && v > smooth[i - 1] && v >= smooth[i + 1]) {
                if (peaks.isEmpty() || i - peaks.last() >= minDistance) {
                    peaks += i
                } else if (v > smooth[peaks.last()]) {
                    // Remplace le pic précédent si celui-ci est plus haut et trop proche
                    peaks[peaks.lastIndex] = i
                }
            }
        }
        if (peaks.size < 4) return null

        var workingPeaks = peaks.toList()

        // 3.5) Correction "harmonique dicrotique" — deux signes possibles :
        //   a) intervalles qui alternent (petit/grand)
        //   b) hauteurs de pics qui alternent (systolique grand / dicrotique petit)
        // Dans les deux cas, on ne garde qu'un pic sur deux (le plus haut du couple).
        fun applyFusion(subset: List<Int>) {
            workingPeaks = subset
        }

        if (workingPeaks.size >= 6) {
            val heights = workingPeaks.map { smooth[it] }
            val oddH = heights.filterIndexed { i, _ -> i % 2 == 0 }.average().toFloat()
            val evenH = heights.filterIndexed { i, _ -> i % 2 == 1 }.average().toFloat()
            val heightRatio = if (min(oddH, evenH) > 0f) max(oddH, evenH) / min(oddH, evenH) else 1f

            val intervalsRaw = IntArray(workingPeaks.size - 1) { workingPeaks[it + 1] - workingPeaks[it] }.toList()
            val oddI = intervalsRaw.filterIndexed { i, _ -> i % 2 == 0 }.average()
            val evenI = intervalsRaw.filterIndexed { i, _ -> i % 2 == 1 }.average()
            val intervalRatio = if (min(oddI, evenI) > 0.0) max(oddI, evenI) / min(oddI, evenI) else 1.0

            val provisionalBpm = sampleRateHz * 60f / intervalsRaw.average().toFloat()

            if (heightRatio > 1.35f || intervalRatio > 1.5) {
                val keepOdd = oddH >= evenH
                applyFusion(workingPeaks.filterIndexed { i, _ -> (i % 2 == 0) == keepOdd })
            } else if (provisionalBpm > 110f) {
                // BPM très élevé pour un repos : on tente la fusion "au cas où" et
                // on vérifie si le résultat reste physiologiquement plausible (40-110).
                val trial = workingPeaks.filterIndexed { i, _ -> i % 2 == 0 }
                if (trial.size >= 4) {
                    val trialInt = IntArray(trial.size - 1) { trial[it + 1] - trial[it] }.average()
                    val trialBpm = sampleRateHz * 60f / trialInt.toFloat()
                    if (trialBpm in 40f..110f) applyFusion(trial)
                }
            }
        }

        var intervals = IntArray(workingPeaks.size - 1) { workingPeaks[it + 1] - workingPeaks[it] }.toList()
        if (intervals.size < 3) return null

        // 4) Rejet des outliers : garde uniquement les intervalles dans ±40 % de la médiane
        val median = intervals.sorted().let { it[it.size / 2] }.toFloat()
        val kept = intervals.filter { abs(it - median) / median < 0.4f }
        if (kept.size < 3) return null

        val avg = kept.average().toFloat()
        val bpm = (sampleRateHz * 60f / avg).toInt()
        if (bpm !in 40..200) return null

        // 5) Qualité = 1 - CV (coefficient de variation), avec floor plus généreux
        val mean = avg.toDouble()
        val variance = kept.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance).toFloat()
        val cv = (std / avg).coerceIn(0f, 1f)
        val quality = (1f - cv * 2f).coerceIn(0f, 1f)  // amplifie légèrement

        return Verdict(bpm = bpm, quality = quality, peaksDetected = workingPeaks.size)
    }
}
