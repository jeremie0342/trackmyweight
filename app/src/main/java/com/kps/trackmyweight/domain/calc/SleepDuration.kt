package com.kps.trackmyweight.domain.calc

import kotlinx.datetime.Instant

object SleepDuration {

    /** Durée de sommeil en minutes entre coucher et réveil (positive). */
    fun minutesBetween(bedtime: Instant, wakeTime: Instant): Int {
        if (wakeTime <= bedtime) return 0
        return ((wakeTime.toEpochMilliseconds() - bedtime.toEpochMilliseconds()) / 60_000L).toInt()
    }

    /** Format compact "7h30". */
    fun format(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "${h}h${m.toString().padStart(2, '0')}"
    }
}
