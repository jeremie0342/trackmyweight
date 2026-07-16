package com.kps.trackmyweight.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RootDestination { LOADING, ONBOARDING, HOME }

@HiltViewModel
class RootViewModel @Inject constructor(
    private val userRepo: UserProfileRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _destination = MutableStateFlow(RootDestination.LOADING)
    val destination: StateFlow<RootDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = userRepo.isOnboarded()
            _destination.value = if (onboarded) RootDestination.HOME else RootDestination.ONBOARDING
            if (onboarded) scheduleReminders()
        }
    }

    fun markOnboardingDone() {
        _destination.value = RootDestination.HOME
        scheduleReminders()
    }

    private fun scheduleReminders() {
        reminderScheduler.scheduleMorningWeighIn()
        reminderScheduler.scheduleMonthlyMeasurement()
    }
}
