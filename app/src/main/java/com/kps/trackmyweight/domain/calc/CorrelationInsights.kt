package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Une métrique quotidienne comparable, identifiée par une clé stable.
 *
 * La clé est persistée dans `correlation_insight` : la renommer casserait la
 * lecture des corrélations déjà calculées.
 */
enum class DailyMetric(val key: String, val labelFr: String, val unit: String) {
    SLEEP_HOURS("sleep_h", "Sommeil", "h"),
    STEPS("steps", "Pas", "pas"),
    READINESS("readiness", "Forme du jour", "/5"),
    RESTING_HR("resting_hr", "FC au repos", "bpm"),
    SESSION_VOLUME("session_volume", "Volume soulevé", "kg"),
    BODY_WEIGHT("body_weight", "Poids", "kg"),
}

/** Deux métriques dont le croisement a un sens physiologique. */
data class MetricPair(val x: DailyMetric, val y: DailyMetric)

/**
 * Croisements retenus.
 *
 * Volontairement restreint : corréler toutes les paires possibles produirait
 * surtout du bruit, et une corrélation forte trouvée par hasard parmi trente
 * essais n'apprend rien. Chaque paire ci-dessous a une hypothèse derrière elle.
 */
val CORRELATION_PAIRS: List<MetricPair> = listOf(
    // Dormir mieux devrait se sentir le lendemain.
    MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.READINESS),
    // Et se voir à l'entraînement.
    MetricPair(DailyMetric.SLEEP_HOURS, DailyMetric.SESSION_VOLUME),
    // La forme ressentie prédit-elle vraiment la performance ?
    MetricPair(DailyMetric.READINESS, DailyMetric.SESSION_VOLUME),
    // La FC au repos est un marqueur classique de récupération.
    MetricPair(DailyMetric.RESTING_HR, DailyMetric.READINESS),
    // L'activité quotidienne pèse-t-elle sur la balance ?
    MetricPair(DailyMetric.STEPS, DailyMetric.BODY_WEIGHT),
)

data class CorrelationInsight(
    val pair: MetricPair,
    val result: CorrelationResult,
    val narrative: String,
)

/**
 * Met en évidence les liens entre habitudes et résultats.
 *
 * [PearsonCorrelation] et la table `correlation_insight` existaient sans que
 * rien ne les alimente : la fonctionnalité n'était calculée nulle part.
 */
object CorrelationInsights {

    /**
     * En dessous, le coefficient n'est pas interprétable.
     *
     * Pearson sur quatre points donne facilement un r de 0,9 par pur hasard.
     * Mieux vaut ne rien afficher qu'un lien inventé.
     */
    const val MIN_SAMPLE_SIZE = 10

    /**
     * Calcule les corrélations pour les paires retenues.
     *
     * @param series valeurs par métrique et par jour. Une paire n'est évaluée
     *   que sur les jours où les DEUX métriques sont renseignées.
     */
    fun compute(
        series: Map<DailyMetric, Map<LocalDate, Float>>,
        pairs: List<MetricPair> = CORRELATION_PAIRS,
        minSampleSize: Int = MIN_SAMPLE_SIZE,
    ): List<CorrelationInsight> = pairs.mapNotNull { pair ->
        val xs = series[pair.x] ?: return@mapNotNull null
        val ys = series[pair.y] ?: return@mapNotNull null
        val sharedDates = xs.keys.intersect(ys.keys).sorted()
        if (sharedDates.size < minSampleSize) return@mapNotNull null

        val result = PearsonCorrelation.compute(
            xs = sharedDates.map { xs.getValue(it) },
            ys = sharedDates.map { ys.getValue(it) },
        )
        CorrelationInsight(pair, result, narrate(pair, result))
    }.sortedByDescending { abs(it.result.r) }

    /**
     * Formule le lien en français, sans jamais affirmer de causalité : une
     * corrélation dit que deux choses varient ensemble, pas que l'une cause
     * l'autre.
     */
    fun narrate(pair: MetricPair, result: CorrelationResult): String {
        val x = pair.x.labelFr
        val y = pair.y.labelFr
        val days = "${result.sampleSize} jours"

        if (result.strength == CorrelationStrength.NEGLIGIBLE) {
            return "Aucun lien visible entre $x et $y sur $days."
        }

        val intensity = when (result.strength) {
            CorrelationStrength.STRONG -> "très nettement"
            CorrelationStrength.MODERATE -> "assez nettement"
            else -> "légèrement"
        }
        val movement = if (result.r > 0f) "augmente aussi" else "baisse"
        return "Quand $x augmente, $y $movement, $intensity (r = %.2f sur $days). "
            .format(result.r) + "Lien observé, pas forcément une cause."
    }
}
