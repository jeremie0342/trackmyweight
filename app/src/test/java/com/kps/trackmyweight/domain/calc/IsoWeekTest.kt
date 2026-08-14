package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les cas attendus sont ceux d'ISO 8601, vérifiés contre `date.isocalendar()`
 * de Python — pas contre ma propre lecture de la norme.
 *
 * Presque tout l'intérêt est aux bascules d'année : le reste de l'année, à peu
 * près n'importe quelle implémentation tombe juste.
 */
class IsoWeekTest {

    private fun week(y: Int, m: Int, d: Int) = IsoWeek.of(LocalDate(y, m, d))

    @Test fun `milieu d'annee, cas sans piege`() {
        assertEquals("2026-W33", week(2026, 8, 13))
    }

    @Test fun `la semaine 1 est celle du 4 janvier`() {
        // 2026-01-04 est un dimanche : il ferme la semaine 1, il ne l'ouvre pas.
        assertEquals("2026-W01", week(2026, 1, 4))
        assertEquals("2026-W02", week(2026, 1, 5))
    }

    @Test fun `fin decembre peut appartenir a l'annee suivante`() {
        assertEquals("2025-W52", week(2025, 12, 28))
        // Lundi 29 : sa semaine a son jeudi en janvier, elle bascule en 2026.
        assertEquals("2026-W01", week(2025, 12, 29))
        assertEquals("2026-W01", week(2025, 12, 31))
        assertEquals("2026-W01", week(2026, 1, 1))
    }

    @Test fun `debut janvier peut appartenir a l'annee precedente`() {
        assertEquals("2026-W53", week(2027, 1, 1))
        assertEquals("2026-W53", week(2027, 1, 3))
        assertEquals("2027-W01", week(2027, 1, 4))
    }

    @Test fun `les annees a 53 semaines en ont bien 53`() {
        // 2026 et 2020 en comptent 53, 2025 seulement 52.
        assertEquals("2026-W53", week(2026, 12, 31))
        assertEquals("2020-W53", week(2020, 12, 31))
        assertEquals("2020-W53", week(2021, 1, 1))
        assertEquals(52, IsoWeek.numberOf(LocalDate(2025, 12, 28)))
    }

    @Test fun `l'annee ISO se distingue de l'annee civile`() {
        assertEquals(2026, IsoWeek.yearOf(LocalDate(2025, 12, 31)))
        assertEquals(2026, IsoWeek.yearOf(LocalDate(2027, 1, 1)))
        assertEquals(2025, IsoWeek.yearOf(LocalDate(2025, 12, 28)))
    }

    @Test fun `tous les jours d'une meme semaine donnent la meme cle`() {
        // Lundi 10 au dimanche 16 aout 2026.
        val keys = (10..16).map { week(2026, 8, it) }.distinct()
        assertEquals(listOf("2026-W33"), keys)
        // Et le lundi suivant bascule.
        assertEquals("2026-W34", week(2026, 8, 17))
    }

    @Test fun `l'ordre lexicographique est l'ordre chronologique`() {
        // C'est ce sur quoi repose `observeWeeklyVolumeSince`, qui compare les
        // cles en SQL sans les parser. Le zero de remplissage n'est donc pas
        // cosmetique : "2026-W9" se trierait apres "2026-W10".
        val chronologique = listOf(
            week(2025, 12, 20), week(2025, 12, 28), week(2025, 12, 29),
            week(2026, 2, 25), week(2026, 3, 4), week(2026, 12, 31), week(2027, 1, 4),
        )
        assertEquals(chronologique.sorted(), chronologique)
        assertTrue(chronologique.all { it.length == 8 })
    }

    @Test fun `une annee entiere ne produit ni semaine 0 ni saut`() {
        // Balayage jour par jour : chaque numero est plausible, et la suite des
        // cles distinctes n'a pas de trou.
        var date = LocalDate(2026, 1, 1)
        val seen = mutableListOf<String>()
        while (date < LocalDate(2027, 1, 1)) {
            val n = IsoWeek.numberOf(date)
            assertTrue("semaine $n hors bornes pour $date", n in 1..53)
            IsoWeek.of(date).let { if (seen.lastOrNull() != it) seen += it }
            date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
        }
        // 2026 commence en W01 et compte 53 semaines, avant de basculer en 2027.
        assertEquals("2026-W01", seen.first())
        assertEquals("2026-W53", seen.last())
        assertEquals(53, seen.size)
        assertEquals(seen.distinct(), seen)
    }
}
