package com.kps.trackmyweight.ui.common

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formatage des dates pour l'affichage.
 *
 * Aucune donnée temporelle ne doit être montrée via `toString()` : un
 * `Instant` s'affiche alors en ISO brut (`2026-08-13T09:12:44Z`), ce qui n'a
 * rien à faire sous les yeux de l'utilisateur.
 */

private val MONTHS_SHORT_FR = listOf(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc.",
)

private val MONTHS_LONG_FR = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** Ex. `13 août 2026`. */
fun LocalDate.formatFr(): String = "$dayOfMonth ${MONTHS_SHORT_FR[monthNumber - 1]} $year"

fun Instant.formatDateFr(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.formatFr()

/** Ex. `18:42`. */
fun Instant.formatTimeFr(): String {
    val t = toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d".format(t.hour, t.minute)
}

/**
 * Convertit une clé de mois SQL (`YYYY-MM`) en libellé lisible (`août 2026`).
 * Renvoie la valeur brute si le format n'est pas celui attendu, plutôt que de
 * lever une exception dans une couche d'affichage.
 */
fun String.toMonthLabelFr(): String {
    val parts = split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return this
    if (month !in 1..12) return this
    return "${MONTHS_LONG_FR[month - 1]} ${parts[0]}"
}
