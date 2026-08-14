package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/**
 * Semaine ISO 8601, au format `2026-W33`.
 *
 * C'est la clé de `muscle_group_volume_weekly`, et l'historique du volume se
 * lit en comparant ces chaînes. Le format y est pour quelque chose : l'année
 * sur 4 chiffres devant, la semaine sur 2, donc l'ordre lexicographique est
 * l'ordre chronologique. `"2025-W52" < "2026-W01"` se compare correctement
 * sans rien parser.
 *
 * Extrait de [StrengthRepository][com.kps.trackmyweight.data.repository.StrengthRepository],
 * où il était privé : un calendrier a beau paraître trivial, l'ISO ne l'est pas,
 * et une clé fausse une semaine par an corromprait l'agrégat sans rien casser
 * de visible.
 *
 * Les deux règles qui font tout :
 *
 * 1. Une semaine appartient à l'année de son **jeudi**. Le 31 décembre 2025 est
 *    donc en `2026-W01`, et le 1er janvier 2027 en `2026-W53`.
 * 2. La semaine 1 est celle qui contient le **4 janvier** — autrement dit le
 *    premier jeudi de l'année.
 */
object IsoWeek {

    /** Semaine ISO de [date], au format `2026-W33`. */
    fun of(date: LocalDate): String = "%04d-W%02d".format(yearOf(date), numberOf(date))

    /** Numéro de semaine ISO, de 1 à 52 ou 53 selon l'année. */
    fun numberOf(date: LocalDate): Int {
        val thursday = thursdayOf(date)
        return (thursday.toEpochDays() - firstThursdayOf(thursday.year).toEpochDays()) / 7 + 1
    }

    /**
     * Année ISO, qui n'est pas toujours l'année civile : fin décembre peut
     * appartenir à l'année suivante, et début janvier à la précédente.
     */
    fun yearOf(date: LocalDate): Int = thursdayOf(date).year

    /** Le jeudi de la semaine contenant [date]. */
    private fun thursdayOf(date: LocalDate): LocalDate =
        date.plus(DatePeriod(days = 4 - date.dayOfWeek.isoDayNumber))

    /** Le jeudi de la semaine 1 de [year], c'est-à-dire celui de la semaine du 4 janvier. */
    private fun firstThursdayOf(year: Int): LocalDate = thursdayOf(LocalDate(year, 1, 4))
}
