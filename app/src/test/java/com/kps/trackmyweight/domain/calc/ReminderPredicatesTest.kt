package com.kps.trackmyweight.domain.calc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPredicatesTest {
    @Test fun `no session today → remind`() {
        assertTrue(ReminderPredicates.shouldRemindSessionNotLogged(0))
    }

    @Test fun `session already done → no remind`() {
        assertFalse(ReminderPredicates.shouldRemindSessionNotLogged(1))
        assertFalse(ReminderPredicates.shouldRemindSessionNotLogged(3))
    }

    @Test fun `hydration below 60% → remind`() {
        assertTrue(ReminderPredicates.shouldRemindHydration(1000, 2500))
    }

    @Test fun `hydration at or above 60% → no remind`() {
        assertFalse(ReminderPredicates.shouldRemindHydration(1500, 2500))
        assertFalse(ReminderPredicates.shouldRemindHydration(2500, 2500))
    }

    @Test fun `hydration with zero target returns false`() {
        assertFalse(ReminderPredicates.shouldRemindHydration(500, 0))
    }
}
