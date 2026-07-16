package com.kps.trackmyweight.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingHost(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val equipment by viewModel.equipment.collectAsState()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onCompleted()
    }

    LaunchedEffect(state.step) {
        if (state.step == OnboardingStep.RECAP) viewModel.computeTargets()
    }

    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            val direction = if (targetState.order > initialState.order) 1 else -1
            (slideInHorizontally(tween(220)) { it * direction } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(180)) { -it * direction } + fadeOut(tween(180)))
        },
        modifier = Modifier.fillMaxSize(),
        label = "onboarding-step",
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> WelcomeScreen(onStart = viewModel::next)
            OnboardingStep.IDENTITY -> IdentityScreen(
                state = state,
                onSex = viewModel::setSex,
                onBirthDate = viewModel::setBirthDate,
                onHeight = viewModel::setHeightCm,
                onWeight = viewModel::setCurrentWeightKg,
                onBack = viewModel::back,
                onNext = viewModel::next,
            )
            OnboardingStep.GOAL -> GoalScreen(
                state = state,
                onTargetWeight = viewModel::setTargetWeightKg,
                onTargetDate = viewModel::setTargetDate,
                onPhaseOverride = viewModel::setPhaseOverride,
                onBack = viewModel::back,
                onNext = viewModel::next,
            )
            OnboardingStep.ACTIVITY -> ActivityScreen(
                state = state,
                onSelect = viewModel::setActivityLevel,
                onBack = viewModel::back,
                onNext = viewModel::next,
            )
            OnboardingStep.GYM -> GymScreen(
                state = state,
                equipment = equipment,
                onGymName = viewModel::setGymName,
                onToggleEquipment = viewModel::toggleEquipment,
                onSkip = viewModel::setSkipGym,
                onBack = viewModel::back,
                onNext = viewModel::next,
            )
            OnboardingStep.COACH_MODE -> CoachModeScreen(
                state = state,
                onToggle = viewModel::setCoachMode,
                onBack = viewModel::back,
                onNext = viewModel::next,
            )
            OnboardingStep.RECAP -> RecapScreen(
                state = state,
                onBack = viewModel::back,
                onFinish = viewModel::finish,
            )
        }
    }
}
