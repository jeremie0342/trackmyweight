package com.kps.trackmyweight.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kps.trackmyweight.data.db.entity.EquipmentEntity
import com.kps.trackmyweight.data.db.enums.ActivityLevel
import com.kps.trackmyweight.data.db.enums.EquipmentCategory
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.db.enums.Sex
import com.kps.trackmyweight.domain.calc.NutritionTargets
import com.kps.trackmyweight.ui.common.ChoiceTile
import com.kps.trackmyweight.ui.common.DateField
import com.kps.trackmyweight.ui.common.NavRow
import com.kps.trackmyweight.ui.common.NumericField
import com.kps.trackmyweight.ui.common.PrimaryButton
import com.kps.trackmyweight.ui.common.StepHeader
import com.kps.trackmyweight.ui.common.TextField
import kotlinx.datetime.LocalDate

// ─────────────────────────────────────────────────────────────
// Progress bar en-tête (commune)
// ─────────────────────────────────────────────────────────────

@Composable
fun OnboardingProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth().height(2.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun StepScaffold(
    progress: Float,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingProgress(progress)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) { content() }
    }
}

// ─────────────────────────────────────────────────────────────
// Welcome
// ─────────────────────────────────────────────────────────────

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    StepScaffold(progress = 0f) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(64.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TrackMyWeight",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Ton assistant complet pour la transformation physique.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "On va commencer par quelques infos pour personnaliser tes cibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Commencer", onStart)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Identity
// ─────────────────────────────────────────────────────────────

@Composable
fun IdentityScreen(
    state: OnboardingUiState,
    onSex: (Sex) -> Unit,
    onBirthDate: (LocalDate) -> Unit,
    onHeight: (Float?) -> Unit,
    onWeight: (Float?) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.IDENTITY.progress) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader("À propos de toi", "Ces infos calculent ton métabolisme de base et tes cibles.")

            Text("Sexe biologique", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Sex.entries.forEach { sex ->
                    Box(modifier = Modifier.weight(1f)) {
                        ChoiceTile(
                            title = if (sex == Sex.MALE) "Homme" else "Femme",
                            selected = state.sex == sex,
                            onClick = { onSex(sex) },
                        )
                    }
                }
            }

            DateField(
                label = "Date de naissance",
                value = state.birthDate,
                onValueChange = onBirthDate,
                yearRange = 1930..2020,
            )
            NumericField(
                label = "Taille",
                valueText = state.heightCm?.toIntStr().orEmpty(),
                suffix = "cm",
                onValueChange = { onHeight(it.toFloatOrNull()) },
            )
            NumericField(
                label = "Poids actuel",
                valueText = state.currentWeightKg?.toStr1().orEmpty(),
                suffix = "kg",
                onValueChange = { onWeight(it.toFloatOrNull()) },
            )

            Spacer(Modifier.height(16.dp))
            NavRow(
                onBack = onBack,
                onNext = onNext,
                nextEnabled = state.canProceedFromIdentity,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Goal
// ─────────────────────────────────────────────────────────────

@Composable
fun GoalScreen(
    state: OnboardingUiState,
    onTargetWeight: (Float?) -> Unit,
    onTargetDate: (LocalDate) -> Unit,
    onPhaseOverride: (GoalPhase?) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.GOAL.progress) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader("Ton objectif", "Quel poids veux-tu atteindre et pour quand ?")

            NumericField(
                label = "Poids cible",
                valueText = state.targetWeightKg?.toStr1().orEmpty(),
                suffix = "kg",
                onValueChange = { onTargetWeight(it.toFloatOrNull()) },
            )
            DateField(
                label = "Date cible",
                value = state.targetDate,
                onValueChange = onTargetDate,
                yearRange = 2026..2030,
            )

            Spacer(Modifier.height(8.dp))
            Text("Phase (facultatif — sinon détectée automatiquement)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceTile(
                    title = "Auto",
                    subtitle = "Laisse l'app choisir selon l'écart poids/cible",
                    selected = state.phaseOverride == null,
                    onClick = { onPhaseOverride(null) },
                )
                GoalPhase.entries.forEach { p ->
                    ChoiceTile(
                        title = p.displayName(),
                        subtitle = p.subtitle(),
                        selected = state.phaseOverride == p,
                        onClick = { onPhaseOverride(p) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            NavRow(onBack = onBack, onNext = onNext, nextEnabled = state.canProceedFromGoal)
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun GoalPhase.displayName() = when (this) {
    GoalPhase.CUT -> "Sèche"
    GoalPhase.RECOMP -> "Recomposition"
    GoalPhase.BULK -> "Prise de masse"
    GoalPhase.MAINTENANCE -> "Maintenance"
}
private fun GoalPhase.subtitle() = when (this) {
    GoalPhase.CUT -> "Perte de gras avec préservation du muscle"
    GoalPhase.RECOMP -> "Perdre du gras + gagner du muscle en même temps"
    GoalPhase.BULK -> "Gagner en masse musculaire, léger surplus"
    GoalPhase.MAINTENANCE -> "Stabiliser le poids"
}

// ─────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────

@Composable
fun ActivityScreen(
    state: OnboardingUiState,
    onSelect: (ActivityLevel) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.ACTIVITY.progress) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                "Niveau d'activité",
                "Incluant travail, déplacements et entraînement. Sert au calcul du TDEE.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    ChoiceTile(
                        title = level.displayName(),
                        subtitle = level.subtitle(),
                        selected = state.activityLevel == level,
                        onClick = { onSelect(level) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            NavRow(onBack = onBack, onNext = onNext)
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun ActivityLevel.displayName() = when (this) {
    ActivityLevel.SEDENTARY -> "Sédentaire"
    ActivityLevel.LIGHTLY_ACTIVE -> "Légèrement actif"
    ActivityLevel.MODERATELY_ACTIVE -> "Modérément actif"
    ActivityLevel.VERY_ACTIVE -> "Très actif"
    ActivityLevel.EXTRA_ACTIVE -> "Extrêmement actif"
}
private fun ActivityLevel.subtitle() = when (this) {
    ActivityLevel.SEDENTARY -> "Bureau, peu ou pas de sport"
    ActivityLevel.LIGHTLY_ACTIVE -> "Sport léger 1-3× / semaine"
    ActivityLevel.MODERATELY_ACTIVE -> "Sport modéré 3-5× / semaine"
    ActivityLevel.VERY_ACTIVE -> "Sport intense 6-7× / semaine"
    ActivityLevel.EXTRA_ACTIVE -> "Sport très intense + travail physique"
}

// ─────────────────────────────────────────────────────────────
// Gym setup
// ─────────────────────────────────────────────────────────────

@Composable
fun GymScreen(
    state: OnboardingUiState,
    equipment: List<EquipmentEntity>,
    onGymName: (String) -> Unit,
    onToggleEquipment: (Long) -> Unit,
    onSkip: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.GYM.progress) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                "Ta salle de sport",
                "Coche ce qui est dispo. L'app filtrera les exercices en conséquence.",
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Configurer une salle",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = !state.skipGym,
                    onCheckedChange = { onSkip(!it) },
                )
            }

            if (!state.skipGym) {
                TextField(
                    label = "Nom de la salle",
                    value = state.gymName,
                    onValueChange = onGymName,
                )

                val byCategory = equipment.groupBy { it.category }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    byCategory.forEach { (cat, list) ->
                        item(key = "cat-${cat.name}") {
                            Text(
                                cat.displayName(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(list, key = { it.id }) { eq ->
                            ChoiceTile(
                                title = eq.displayName,
                                selected = eq.id in state.selectedEquipmentIds,
                                onClick = { onToggleEquipment(eq.id) },
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Aucune salle configurée. Tu pourras l'ajouter plus tard depuis les paramètres.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            NavRow(onBack = onBack, onNext = onNext)
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun EquipmentCategory.displayName() = when (this) {
    EquipmentCategory.BAR -> "Barres & disques"
    EquipmentCategory.DUMBBELL -> "Haltères"
    EquipmentCategory.KETTLEBELL -> "Kettlebells"
    EquipmentCategory.MACHINE -> "Machines"
    EquipmentCategory.CABLE -> "Poulies / câbles"
    EquipmentCategory.BODYWEIGHT -> "Poids du corps"
    EquipmentCategory.CARDIO -> "Cardio"
    EquipmentCategory.ACCESSORY -> "Accessoires"
}

// ─────────────────────────────────────────────────────────────
// Coach mode
// ─────────────────────────────────────────────────────────────

@Composable
fun CoachModeScreen(
    state: OnboardingUiState,
    onToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.COACH_MODE.progress) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                "As-tu un coach ?",
                "En mode coach, l'app respecte ton programme sans proposer d'ajustement automatique.",
            )
            ChoiceTile(
                title = "Non — mode auto",
                subtitle = "L'app propose progression, deload, ajustements caloriques",
                selected = !state.coachModeEnabled,
                onClick = { onToggle(false) },
            )
            ChoiceTile(
                title = "Oui — mode coach",
                subtitle = "L'app mesure et logue sans contredire ton programme",
                selected = state.coachModeEnabled,
                onClick = { onToggle(true) },
            )
            Spacer(Modifier.weight(1f))
            NavRow(onBack = onBack, onNext = onNext, nextLabel = "Voir le récap")
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Recap
// ─────────────────────────────────────────────────────────────

@Composable
fun RecapScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    StepScaffold(progress = OnboardingStep.RECAP.progress) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(
                "Tes cibles calculées",
                "Tu pourras les ajuster à tout moment.",
            )

            val t: NutritionTargets? = state.computedTargets
            if (t == null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                TargetCard("Métabolisme de base (BMR)", "${t.bmr} kcal / jour")
                TargetCard("Dépense totale (TDEE)", "${t.tdee} kcal / jour")
                TargetCard(
                    "Cible calorique",
                    "${t.targetKcal} kcal / jour",
                    subtitle = if (t.deficitKcal > 0) "Déficit ${t.deficitKcal} kcal" else if (t.deficitKcal < 0) "Surplus ${-t.deficitKcal} kcal" else "Maintenance",
                )
                TargetCard("Protéines", "${t.targetProteinG} g / jour")
                TargetCard("Glucides", "${t.targetCarbsG} g / jour")
                TargetCard("Lipides", "${t.targetFatsG} g / jour")
                TargetCard(
                    "Rythme visé",
                    "${"%.2f".format(t.weeklyRateKg)} kg / semaine",
                    subtitle = "Phase : ${t.recommendedPhase.displayName()}",
                )

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))
            NavRow(
                onBack = onBack,
                onNext = onFinish,
                nextEnabled = t != null && !state.isSaving,
                nextLabel = if (state.isSaving) "Enregistrement..." else "Terminer",
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TargetCard(label: String, value: String, subtitle: String? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers d'affichage
// ─────────────────────────────────────────────────────────────

private fun Float.toStr1(): String = if (this % 1f == 0f) toInt().toString() else "%.1f".format(this)
private fun Float.toIntStr(): String = toInt().toString()
