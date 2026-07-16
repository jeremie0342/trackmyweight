package com.kps.trackmyweight.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.repository.UserProfileRepository
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
) : ViewModel() {

    private val _destination = MutableStateFlow(RootDestination.LOADING)
    val destination: StateFlow<RootDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            _destination.value = if (userRepo.isOnboarded()) RootDestination.HOME else RootDestination.ONBOARDING
        }
    }

    fun markOnboardingDone() {
        _destination.value = RootDestination.HOME
    }
}
