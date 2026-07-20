package com.kps.trackmyweight.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.repository.GoalRepository
import com.kps.trackmyweight.data.repository.MealWithEntries
import com.kps.trackmyweight.data.repository.NutritionRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import com.kps.trackmyweight.data.repository.WeightRepository
import com.kps.trackmyweight.domain.calc.DistributionQuality
import com.kps.trackmyweight.domain.calc.DistributionVerdict
import com.kps.trackmyweight.domain.calc.MealProtein
import com.kps.trackmyweight.domain.calc.NutritionCalculator
import com.kps.trackmyweight.domain.calc.ProteinDistribution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class NutritionUiState(
    val date: LocalDate = LocalDate(2000, 1, 1),
    val meals: List<MealWithEntries> = emptyList(),
    val phase: DietPhaseEntity? = null,
    /** TDEE estimé pour comparer la cible actuelle à la maintenance. */
    val tdeeEstimate: Int? = null,
    val kcalConsumed: Float = 0f,
    val proteinConsumed: Float = 0f,
    val carbsConsumed: Float = 0f,
    val fatsConsumed: Float = 0f,
    val fiberConsumed: Float = 0f,
    val distribution: DistributionVerdict? = null,
    val favorites: List<FavoriteMealEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepo: NutritionRepository,
    private val userRepo: UserProfileRepository,
    private val goalRepo: GoalRepository,
    private val weightRepo: WeightRepository,
) : ViewModel() {

    private val _date = MutableStateFlow(todayLocal())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    val state: StateFlow<NutritionUiState> = combine(
        _date,
        nutritionRepo.observeActivePhase(),
        nutritionRepo.observeFavorites(),
        _refreshTrigger,
    ) { date, phase, favs, _ ->
        val meals = nutritionRepo.getMealsWithEntries(date)
        val kcal = meals.sumOf { it.totalKcal.toDouble() }.toFloat()
        val protein = meals.sumOf { it.totalProteinG.toDouble() }.toFloat()
        val carbs = meals.flatMap { it.entries }.sumOf { it.entry.snapCarbsG.toDouble() }.toFloat()
        val fats = meals.flatMap { it.entries }.sumOf { it.entry.snapFatsG.toDouble() }.toFloat()
        val fiber = meals.flatMap { it.entries }.sumOf { it.entry.snapFiberG.toDouble() }.toFloat()
        val dist = phase?.let {
            ProteinDistribution.analyze(
                meals = meals.map { m -> MealProtein(m.meal.mealType.name, m.totalProteinG) },
                dailyTargetG = it.targetProteinG,
            )
        }
        val tdee = runCatching {
            val profile = userRepo.current()
            val weight = weightRepo.observeLast().first()?.weightKg
            if (profile != null && weight != null) {
                val age = date.year - profile.birthDate.year
                val bmr = NutritionCalculator.bmr(profile.sex, weight, profile.heightCm, age)
                NutritionCalculator.tdee(bmr, profile.activityLevel)
            } else null
        }.getOrNull()
        NutritionUiState(
            date = date,
            meals = meals,
            phase = phase,
            tdeeEstimate = tdee,
            kcalConsumed = kcal,
            proteinConsumed = protein,
            carbsConsumed = carbs,
            fatsConsumed = fats,
            fiberConsumed = fiber,
            distribution = dist,
            favorites = favs,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionUiState())

    init {
        viewModelScope.launch { nutritionRepo.seedIfEmpty() }
    }

    fun setDate(d: LocalDate) { _date.value = d }

    suspend fun searchFoods(query: String): List<FoodEntity> = nutritionRepo.searchFoods(query, 40)

    fun addEntry(
        mealType: MealType,
        foodId: Long,
        mode: PortionMode,
        quantity: Float,
    ) {
        viewModelScope.launch {
            runCatching {
                nutritionRepo.addEntry(_date.value, mealType, foodId, mode, quantity)
            }
            _refreshTrigger.update { it + 1 }
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            nutritionRepo.deleteEntry(entryId)
            _refreshTrigger.update { it + 1 }
        }
    }

    fun saveMealAsFavorite(name: String, meal: MealWithEntries) {
        viewModelScope.launch {
            nutritionRepo.createFavoriteFromMeal(name, meal.meal.mealType, meal)
        }
    }

    fun applyFavorite(mealType: MealType, favId: Long) {
        viewModelScope.launch {
            nutritionRepo.applyFavorite(_date.value, mealType, favId)
            _refreshTrigger.update { it + 1 }
        }
    }

    private fun todayLocal(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
