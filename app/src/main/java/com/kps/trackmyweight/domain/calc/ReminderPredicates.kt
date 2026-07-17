package com.kps.trackmyweight.domain.calc

/**
 * Prédicats testables pour décider si une notification doit être envoyée.
 * Extraits des workers pour permettre les tests JVM.
 */
object ReminderPredicates {

    /**
     * Décide s'il faut rappeler que la séance n'a pas été loguée.
     */
    fun shouldRemindSessionNotLogged(sessionCountToday: Int): Boolean = sessionCountToday == 0

    /**
     * Décide s'il faut rappeler de boire.
     * Seuil : sous 60% de la cible.
     */
    fun shouldRemindHydration(mlToday: Int, targetMl: Int): Boolean {
        if (targetMl <= 0) return false
        return mlToday.toFloat() / targetMl < 0.6f
    }
}
