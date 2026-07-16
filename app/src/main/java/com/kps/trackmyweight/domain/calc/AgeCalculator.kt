package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.LocalDate
import kotlinx.datetime.until
import kotlinx.datetime.DateTimeUnit

/** Âge en années complètes entre une date de naissance et aujourd'hui. */
object AgeCalculator {
    fun yearsBetween(birthDate: LocalDate, today: LocalDate): Int {
        require(!birthDate.compareTo(today).let { it > 0 }) { "birthDate must be in the past" }
        return birthDate.until(today, DateTimeUnit.YEAR)
    }
}
