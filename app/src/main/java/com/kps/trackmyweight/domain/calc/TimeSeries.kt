package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.abs

/**
 * Point (date, valeur) utilisé par les analyses temporelles (poids, mensurations).
 */
data class DatedValue(val date: LocalDate, val value: Float)

/** Résultat de la moyenne mobile centrée/traînante avec fenêtre glissante. */
data class SmoothedPoint(val date: LocalDate, val raw: Float, val smoothed: Float)

/**
 * Régression linéaire simple, avec projection et détection de stagnation.
 * Pure Kotlin, aucune dépendance externe.
 */
object TimeSeries {

    /**
     * Moyenne mobile sur `windowDays` jours calendaires (pas nombre d'échantillons).
     * Utilise la fenêtre "trailing" : à la date D, moyenne des valeurs entre D-window+1 et D.
     */
    fun trailingMovingAverage(points: List<DatedValue>, windowDays: Int = 7): List<SmoothedPoint> {
        if (points.isEmpty()) return emptyList()
        val sorted = points.sortedBy { it.date }
        val out = mutableListOf<SmoothedPoint>()
        var leftIndex = 0
        var runningSum = 0.0
        var runningCount = 0
        sorted.forEachIndexed { rightIndex, current ->
            runningSum += current.value
            runningCount += 1
            // Shrink from the left while > window
            while (leftIndex < rightIndex &&
                sorted[leftIndex].date.daysUntil(current.date) >= windowDays
            ) {
                runningSum -= sorted[leftIndex].value
                runningCount -= 1
                leftIndex += 1
            }
            val avg = if (runningCount > 0) (runningSum / runningCount).toFloat() else current.value
            out += SmoothedPoint(date = current.date, raw = current.value, smoothed = avg)
        }
        return out
    }

    /**
     * Régression linéaire par moindres carrés, avec x = jours depuis le premier point.
     * Retourne (slopePerDay, interceptAtFirstDate) ou null si <2 points.
     */
    fun linearFit(points: List<DatedValue>): Pair<Float, Float>? {
        if (points.size < 2) return null
        val sorted = points.sortedBy { it.date }
        val origin = sorted.first().date
        val xs = sorted.map { origin.daysUntil(it.date).toDouble() }
        val ys = sorted.map { it.value.toDouble() }
        val n = xs.size
        val meanX = xs.average()
        val meanY = ys.average()
        val cov = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) }
        val varX = xs.sumOf { (it - meanX) * (it - meanX) }
        if (varX == 0.0) return null
        val slope = cov / varX
        val intercept = meanY - slope * meanX
        return slope.toFloat() to intercept.toFloat()
    }

    /**
     * Projette la valeur à une date future en supposant la tendance linéaire actuelle.
     * Renvoie null si insuffisamment de données.
     */
    fun projectAt(points: List<DatedValue>, targetDate: LocalDate): Float? {
        val fit = linearFit(points) ?: return null
        val (slope, intercept) = fit
        val origin = points.minOf { it.date }
        val daysFromOrigin = origin.daysUntil(targetDate).toFloat()
        return intercept + slope * daysFromOrigin
    }

    /**
     * Estime la date à laquelle une valeur cible sera atteinte selon la tendance actuelle.
     * Renvoie null si pas de tendance, ou si la cible ne peut pas être atteinte (mauvaise direction).
     */
    fun etaFor(points: List<DatedValue>, targetValue: Float): LocalDate? {
        val fit = linearFit(points) ?: return null
        val (slope, intercept) = fit
        if (abs(slope) < 1e-6f) return null
        val daysNeeded = ((targetValue - intercept) / slope).toDouble()
        if (daysNeeded.isNaN() || daysNeeded.isInfinite() || daysNeeded < 0.0) return null
        val origin = points.minOf { it.date }
        return origin.plusDays(kotlin.math.round(daysNeeded).toInt())
    }

    private fun LocalDate.plusDays(days: Int): LocalDate =
        kotlinx.datetime.LocalDate.fromEpochDays(this.toEpochDays() + days)
}

/**
 * Détecte une stagnation : pas de progression sensible sur `windowDays` jours.
 * Le seuil est défini en unités absolues (kg, cm...).
 */
object StagnationDetector {
    data class Verdict(
        val isStagnating: Boolean,
        val netChange: Float,
        val daysObserved: Int,
    )

    fun detect(
        points: List<DatedValue>,
        windowDays: Int = 14,
        thresholdAbs: Float = 0.3f,
        today: LocalDate,
    ): Verdict {
        val sorted = points.sortedBy { it.date }
        val cutoff = LocalDate.fromEpochDays(today.toEpochDays() - windowDays)
        val recent = sorted.filter { it.date >= cutoff }
        if (recent.size < 2) {
            return Verdict(isStagnating = false, netChange = 0f, daysObserved = recent.size)
        }
        val netChange = recent.last().value - recent.first().value
        val daysObserved = recent.first().date.daysUntil(recent.last().date)
        return Verdict(
            isStagnating = abs(netChange) < thresholdAbs && daysObserved >= windowDays / 2,
            netChange = netChange,
            daysObserved = daysObserved,
        )
    }
}
