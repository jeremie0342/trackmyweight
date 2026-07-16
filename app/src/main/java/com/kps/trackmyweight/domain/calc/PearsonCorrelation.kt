package com.kps.trackmyweight.domain.calc

import kotlin.math.sqrt

data class CorrelationResult(
    val r: Float,
    val sampleSize: Int,
    val strength: CorrelationStrength,
) {
    val isSignificantHint: Boolean get() = sampleSize >= 5 && kotlin.math.abs(r) >= 0.4f
}

enum class CorrelationStrength { NEGLIGIBLE, WEAK, MODERATE, STRONG }

/**
 * Corrélation de Pearson entre deux séries de valeurs appariées.
 * Renvoie r ∈ [-1, 1] + qualification qualitative.
 */
object PearsonCorrelation {

    fun compute(xs: List<Float>, ys: List<Float>): CorrelationResult {
        require(xs.size == ys.size) { "series must be same length" }
        val n = xs.size
        if (n < 2) return CorrelationResult(0f, n, CorrelationStrength.NEGLIGIBLE)

        val meanX = xs.average()
        val meanY = ys.average()
        var num = 0.0
        var denomX = 0.0
        var denomY = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            num += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }
        if (denomX == 0.0 || denomY == 0.0) return CorrelationResult(0f, n, CorrelationStrength.NEGLIGIBLE)
        val r = (num / sqrt(denomX * denomY)).toFloat()
        return CorrelationResult(r, n, classify(r))
    }

    private fun classify(r: Float): CorrelationStrength = when {
        kotlin.math.abs(r) < 0.2f -> CorrelationStrength.NEGLIGIBLE
        kotlin.math.abs(r) < 0.4f -> CorrelationStrength.WEAK
        kotlin.math.abs(r) < 0.7f -> CorrelationStrength.MODERATE
        else -> CorrelationStrength.STRONG
    }
}
