package com.kps.trackmyweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kps.trackmyweight.ui.home.HomeScreen
import com.kps.trackmyweight.ui.onboarding.OnboardingHost
import com.kps.trackmyweight.ui.root.RootDestination
import com.kps.trackmyweight.ui.root.RootViewModel
import com.kps.trackmyweight.ui.theme.TrackMyWeightTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TrackMyWeightTheme {
                val destination by rootViewModel.destination.collectAsState()
                if (destination != RootDestination.LOADING) keepSplash = false
                RootRouter(
                    destination = destination,
                    onOnboardingDone = rootViewModel::markOnboardingDone,
                )
            }
        }
    }
}

@Composable
private fun RootRouter(destination: RootDestination, onOnboardingDone: () -> Unit) {
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (destination) {
                RootDestination.LOADING -> CircularProgressIndicator()
                RootDestination.ONBOARDING -> OnboardingHost(onCompleted = onOnboardingDone)
                RootDestination.HOME -> HomeScreen()
            }
        }
    }
}
