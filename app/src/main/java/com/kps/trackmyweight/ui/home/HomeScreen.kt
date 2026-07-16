package com.kps.trackmyweight.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class HomeViewModel @Inject constructor(
    userRepo: UserProfileRepository,
    goalRepo: GoalRepository,
) : ViewModel() {
    val profile: StateFlow<UserProfileEntity?> = userRepo.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val goal: StateFlow<GoalEntity?> = goalRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    val goal by vm.goal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Aujourd'hui",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Onboarding terminé. Les prochains écrans arrivent dans les phases suivantes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        profile?.let { p ->
            InfoCard(
                title = "Profil",
                lines = listOf(
                    "Taille : ${p.heightCm.toInt()} cm",
                    "Sexe : ${if (p.sex.name == "MALE") "Homme" else "Femme"}",
                    "Mode coach : ${if (p.coachModeEnabled) "activé" else "désactivé"}",
                ),
            )
        }

        goal?.let { g ->
            InfoCard(
                title = "Objectif actif",
                lines = listOf(
                    "Poids cible : ${"%.1f".format(g.targetWeightKg)} kg",
                    "Date cible : ${g.targetDate}",
                    "Phase : ${g.phase.name}",
                ),
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
