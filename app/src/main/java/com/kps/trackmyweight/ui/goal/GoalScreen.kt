package com.kps.trackmyweight.ui.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.GoalEntity
import com.kps.trackmyweight.data.db.entity.UserProfileEntity
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.GoalPhase
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.NutritionRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.NutritionCalculator
import com.kps.trackmyweight.ui.common.BackHeader
import com.kps.trackmyweight.ui.common.ChoiceTile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class GoalUiState(
    val profile: UserProfileEntity? = null,
    val goal: GoalEntity? = null,
    val phase: DietPhaseEntity? = null,
    val currentWeightKg: Float? = null,
    val bmr: Int? = null,
    val tdee: Int? = null,
    val message: String? = null,
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val userRepo: UserProfileRepository,
    private val goalRepo: GoalRepository,
    private val weightRepo: WeightRepository,
    private val nutritionRepo: NutritionRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val state: StateFlow<GoalUiState> = kotlinx.coroutines.flow.combine(
        userRepo.observe(),
        goalRepo.observeActive(),
        nutritionRepo.observeActivePhase(),
        weightRepo.observeLast().map { it?.weightKg },
        _message,
    ) { profile, goal, phase, weight, msg ->
        val bmr = if (profile != null && weight != null) {
            val age = today().year - profile.birthDate.year
            NutritionCalculator.bmr(profile.sex, weight, profile.heightCm, age)
        } else null
        val tdee = if (bmr != null && profile != null) NutritionCalculator.tdee(bmr, profile.activityLevel) else null
        GoalUiState(
            profile = profile,
            goal = goal,
            phase = phase,
            currentWeightKg = weight,
            bmr = bmr,
            tdee = tdee,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalUiState())

    fun saveProfile(updated: UserProfileEntity) {
        viewModelScope.launch {
            userRepo.save(updated)
            _message.value = "Profil mis à jour."
        }
    }

    fun changePhase(newPhase: GoalPhase) {
        viewModelScope.launch {
            val profile = userRepo.current() ?: run {
                _message.value = "Profil manquant."
                return@launch
            }
            val goal = goalRepo.observeActive().first() ?: run {
                _message.value = "Aucun objectif actif."
                return@launch
            }
            val weight = weightRepo.observeLast().first()?.weightKg ?: run {
                _message.value = "Aucune pesée enregistrée."
                return@launch
            }
            val age = today().year - profile.birthDate.year
            val targets = NutritionCalculator.compute(
                sex = profile.sex,
                weightKg = weight,
                heightCm = profile.heightCm,
                ageYears = age,
                activityLevel = profile.activityLevel,
                targetWeightKg = goal.targetWeightKg,
                today = today(),
                targetDate = goal.targetDate,
                overridePhase = newPhase,
            )
            val now = Clock.System.now()
            nutritionRepo.setActivePhase(
                DietPhaseEntity(
                    startDate = today(),
                    phase = newPhase.toDietPhaseKind(),
                    targetKcal = targets.targetKcal,
                    targetProteinG = targets.targetProteinG,
                    targetCarbsG = targets.targetCarbsG,
                    targetFatsG = targets.targetFatsG,
                    isActive = true,
                    createdAt = now,
                ),
            )
            _message.value = "Cibles recalculées : ${targets.targetKcal} kcal / jour."
        }
    }

    fun clearMessage() { _message.value = null }
}

private fun GoalPhase.toDietPhaseKind() = when (this) {
    GoalPhase.CUT -> DietPhaseKind.CUT_MODERATE
    GoalPhase.RECOMP -> DietPhaseKind.RECOMP
    GoalPhase.BULK -> DietPhaseKind.BULK_LEAN
    GoalPhase.MAINTENANCE -> DietPhaseKind.MAINTENANCE
}

private fun DietPhaseKind.toGoalPhase() = when (this) {
    DietPhaseKind.CUT_MODERATE, DietPhaseKind.CUT_AGGRESSIVE -> GoalPhase.CUT
    DietPhaseKind.RECOMP -> GoalPhase.RECOMP
    DietPhaseKind.BULK_LEAN, DietPhaseKind.BULK_STANDARD -> GoalPhase.BULK
    DietPhaseKind.MAINTENANCE, DietPhaseKind.REFEED, DietPhaseKind.DIET_BREAK -> GoalPhase.MAINTENANCE
}

private fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

@Composable
fun GoalScreen(
    onBack: () -> Unit,
    vm: GoalViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showEditProfile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackHeader(title = "Mon objectif", onBack = onBack)

            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            ProfileCard(state, onEdit = { showEditProfile = true })
            GoalCard(state)
            PhaseCard(state, onPickPhase = vm::changePhase)
            TargetsCard(state)

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showEditProfile && state.profile != null) {
        EditProfileDialog(
            initial = state.profile!!,
            onDismiss = { showEditProfile = false },
            onSave = { updated ->
                vm.saveProfile(updated)
                showEditProfile = false
            },
        )
    }
}

@Composable
private fun ProfileCard(state: GoalUiState, onEdit: () -> Unit) {
    val p = state.profile
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Profil", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onEdit) { Text("Modifier") }
            }
            if (p == null) {
                Text("Profil non renseigné (relance l'onboarding).", style = MaterialTheme.typography.bodyMedium)
            } else {
                val age = today().year - p.birthDate.year
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sexe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(p.sex.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Âge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$age ans", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Taille", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${p.heightCm.toInt()} cm", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Poids actuel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.currentWeightKg?.let { "%.1f kg".format(it) } ?: "—", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activité", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(p.activityLevel.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(state: GoalUiState) {
    val g = state.goal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Objectif", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (g == null) {
                Text("Aucun objectif actif.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val diff = state.currentWeightKg?.let { g.targetWeightKg - it }
                Text(
                    "Cible : %.1f kg d'ici le %s".format(g.targetWeightKg, g.targetDate),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                diff?.let {
                    val sign = if (it < 0) "" else "+"
                    Text(
                        "Écart : $sign%.1f kg".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseCard(state: GoalUiState, onPickPhase: (GoalPhase) -> Unit) {
    val currentGoalPhase = state.phase?.phase?.toGoalPhase()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Phase", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Choisir une phase recalcule instantanément tes cibles caloriques et macros.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                GoalPhase.CUT to ("Déficit — perdre du gras" to "-350 à -700 kcal / j selon l'écart"),
                GoalPhase.RECOMP to ("Recomposition — perdre du gras + prendre du muscle" to "-100 à -300 kcal / j"),
                GoalPhase.MAINTENANCE to ("Maintenance — stabiliser" to "TDEE tel quel"),
                GoalPhase.BULK to ("Prise de masse" to "+250 à +500 kcal / j"),
            ).forEach { (p, meta) ->
                ChoiceTile(
                    title = meta.first,
                    subtitle = meta.second,
                    selected = currentGoalPhase == p,
                    onClick = { onPickPhase(p) },
                )
            }
        }
    }
}

@Composable
private fun TargetsCard(state: GoalUiState) {
    val p = state.phase
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cibles quotidiennes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p == null) {
                Text("Choisis une phase ci-dessus pour générer tes cibles.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BMR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.bmr?.toString() ?: "—", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TDEE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.tdee?.toString() ?: "—", style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cible kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(p.targetKcal.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    MacroCell("Prot", p.targetProteinG, "g")
                    MacroCell("Gluc", p.targetCarbsG, "g")
                    MacroCell("Lip", p.targetFatsG, "g")
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    initial: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit,
) {
    var sex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.sex) }
    var height by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.heightCm.toInt().toString()) }
    var birthYear by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.birthDate.year.toString()) }
    var birthMonth by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.birthDate.monthNumber.toString()) }
    var birthDay by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.birthDate.dayOfMonth.toString()) }
    var activity by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial.activityLevel) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le profil") },
        text = {
            Column(
                modifier = Modifier.height(520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Sexe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.kps.trackmyweight.data.db.enums.Sex.entries.forEach { s ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                            ChoiceTile(title = if (s.name == "MALE") "Homme" else "Femme", selected = sex == s, onClick = { sex = s })
                        }
                    }
                }

                com.kps.trackmyweight.ui.common.NumericField(label = "Taille (cm)", valueText = height, onValueChange = { height = it })

                Text("Date de naissance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        com.kps.trackmyweight.ui.common.NumericField(label = "Jour", valueText = birthDay, onValueChange = { birthDay = it })
                    }
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        com.kps.trackmyweight.ui.common.NumericField(label = "Mois", valueText = birthMonth, onValueChange = { birthMonth = it })
                    }
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1.3f)) {
                        com.kps.trackmyweight.ui.common.NumericField(label = "Année", valueText = birthYear, onValueChange = { birthYear = it })
                    }
                }

                Text("Niveau d'activité", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                com.kps.trackmyweight.data.db.enums.ActivityLevel.entries.forEach { lvl ->
                    ChoiceTile(
                        title = lvl.labelFr(),
                        subtitle = lvl.hintFr(),
                        selected = activity == lvl,
                        onClick = { activity = lvl },
                    )
                }
            }
        },
        confirmButton = {
            com.kps.trackmyweight.ui.common.PrimaryButton(
                text = "Enregistrer",
                enabled = height.toIntOrNull() != null
                    && birthYear.toIntOrNull() != null
                    && birthMonth.toIntOrNull() in 1..12
                    && birthDay.toIntOrNull() in 1..31,
                onClick = {
                    val date = runCatching {
                        kotlinx.datetime.LocalDate(birthYear.toInt(), birthMonth.toInt(), birthDay.toInt())
                    }.getOrNull() ?: return@PrimaryButton
                    onSave(
                        initial.copy(
                            sex = sex,
                            heightCm = height.toInt().toFloat(),
                            birthDate = date,
                            activityLevel = activity,
                            updatedAt = kotlinx.datetime.Clock.System.now(),
                        ),
                    )
                },
            )
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun com.kps.trackmyweight.data.db.enums.ActivityLevel.labelFr() = when (this) {
    com.kps.trackmyweight.data.db.enums.ActivityLevel.SEDENTARY -> "Sédentaire"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.LIGHTLY_ACTIVE -> "Légèrement actif"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.MODERATELY_ACTIVE -> "Modérément actif"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.VERY_ACTIVE -> "Très actif"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.EXTRA_ACTIVE -> "Extrêmement actif"
}

private fun com.kps.trackmyweight.data.db.enums.ActivityLevel.hintFr() = when (this) {
    com.kps.trackmyweight.data.db.enums.ActivityLevel.SEDENTARY -> "Bureau, très peu de sport"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.LIGHTLY_ACTIVE -> "1-3 séances / semaine"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.MODERATELY_ACTIVE -> "3-5 séances / semaine"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.VERY_ACTIVE -> "6-7 séances / semaine"
    com.kps.trackmyweight.data.db.enums.ActivityLevel.EXTRA_ACTIVE -> "Sport intensif + boulot physique"
}

@Composable
private fun RowScope.MacroCell(label: String, value: Int, unit: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value $unit", style = MaterialTheme.typography.bodyLarge)
    }
}
