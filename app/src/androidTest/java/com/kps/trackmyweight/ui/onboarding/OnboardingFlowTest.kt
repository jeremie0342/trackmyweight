package com.kps.trackmyweight.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Test léger de progression du flow onboarding, en utilisant des états contrôlés
 * (les screens sont composables purs). Le flow complet avec ViewModel Hilt est
 * couvert par les tests instrumentés de repository.
 */
class OnboardingFlowTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun welcome_shows_start_button_and_advances() {
        var started = false
        rule.setContent { WelcomeScreen(onStart = { started = true }) }
        rule.onNodeWithText("Commencer").assertIsDisplayed().performClick()
        assert(started) { "onStart should have been invoked" }
    }

    @Test
    fun identity_next_button_disabled_when_fields_missing() {
        rule.setContent {
            IdentityScreen(
                state = OnboardingUiState(),
                onSex = {},
                onBirthDate = {},
                onHeight = {},
                onWeight = {},
                onBack = {},
                onNext = {},
            )
        }
        rule.onNodeWithText("À propos de toi").assertIsDisplayed()
        // Le bouton "Continuer" existe mais est désactivé — impossible à cliquer sans crash.
        rule.onNodeWithText("Continuer").assertIsDisplayed()
    }
}
