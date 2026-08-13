package com.kps.trackmyweight.ui.nutrition.cost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.ProteinValueRow
import com.kps.trackmyweight.data.repository.NutritionRepository
import com.kps.trackmyweight.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Saisie d'un relevé de prix en cours. */
data class PriceDraft(
    val food: FoodEntity,
    val price: String = "",
    val quantityG: String = "",
) {
    val priceValue: Float? get() = price.toFloatOrNull()?.takeIf { it > 0f }
    val quantityValue: Float? get() = quantityG.toFloatOrNull()?.takeIf { it > 0f }
    val isValid: Boolean get() = priceValue != null && quantityValue != null
}

data class ProteinValueUiState(
    val ranking: List<ProteinValueRow> = emptyList(),
    val withoutPrice: List<FoodEntity> = emptyList(),
    val currency: String = "XOF",
    val draft: PriceDraft? = null,
    val message: String? = null,
) {
    val best: ProteinValueRow? get() = ranking.firstOrNull()
    val worst: ProteinValueRow? get() = ranking.lastOrNull()

    /** Le classement n'a de sens qu'à partir de deux aliments comparables. */
    val isComparable: Boolean get() = ranking.size >= 2
}

/**
 * Classement des aliments par coût protéique.
 *
 * Repond a une contrainte concrete : a budget donne, quelle source de proteine
 * rapporte le plus. Le calcul ([CostPerProtein]) et la table `food_price`
 * existaient sans qu'aucun ecran ne les alimente ni ne les lise.
 */
@HiltViewModel
class ProteinValueViewModel @Inject constructor(
    private val nutritionRepo: NutritionRepository,
    private val userRepo: UserProfileRepository,
) : ViewModel() {

    private val draft = MutableStateFlow<PriceDraft?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val currency = MutableStateFlow("XOF")
    private val withoutPrice = MutableStateFlow<List<FoodEntity>>(emptyList())

    val state: StateFlow<ProteinValueUiState> = combine(
        nutritionRepo.observeProteinValueRanking(limit = 30),
        withoutPrice,
        currency,
        draft,
        message,
    ) { ranking, unpriced, currentCurrency, currentDraft, currentMessage ->
        ProteinValueUiState(
            ranking = ranking,
            withoutPrice = unpriced,
            currency = currentCurrency,
            draft = currentDraft,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProteinValueUiState())

    init {
        viewModelScope.launch {
            nutritionRepo.seedIfEmpty()
            currency.value = userRepo.current()?.currency ?: "XOF"
            refreshUnpriced()
        }
    }

    private suspend fun refreshUnpriced() {
        withoutPrice.value = nutritionRepo.getFoodsWithoutPrice(limit = 40)
    }

    fun startPricing(food: FoodEntity) {
        draft.value = PriceDraft(food = food, quantityG = food.defaultServingG.toInt().toString())
    }

    /**
     * Rouvre la saisie pour un aliment déjà tarifé, pré-remplie avec le dernier
     * relevé. Les prix bougent — un classement figé au premier relevé se
     * périmerait sans qu'on puisse le corriger.
     */
    fun startRepricing(foodId: Long) {
        viewModelScope.launch {
            val food = nutritionRepo.getFood(foodId) ?: return@launch
            val last = nutritionRepo.getLatestPrice(foodId)
            val grams = last?.let { previous ->
                previous.pricePer100g
                    ?.takeIf { it > 0f }
                    ?.let { per100 -> previous.pricePerServing?.div(per100)?.times(100f) }
            } ?: food.defaultServingG
            draft.value = PriceDraft(
                food = food,
                price = last?.pricePerServing?.let { "%.0f".format(it) }.orEmpty(),
                quantityG = grams.toInt().toString(),
            )
        }
    }

    fun setPrice(value: String) = draft.update { it?.copy(price = value) }

    fun setQuantity(value: String) = draft.update { it?.copy(quantityG = value) }

    fun cancelPricing() {
        draft.value = null
    }

    fun savePrice() {
        val current = draft.value ?: return
        val price = current.priceValue ?: return
        val quantity = current.quantityValue ?: return
        viewModelScope.launch {
            runCatching {
                nutritionRepo.recordPrice(
                    foodId = current.food.id,
                    price = price,
                    quantityG = quantity,
                    currency = currency.value,
                )
            }
                .onSuccess { cost ->
                    draft.value = null
                    message.value = if (cost == null) {
                        // Un aliment sans proteines ne peut pas etre classe : le dire
                        // plutot que d'echouer en silence.
                        "${current.food.name} n'apporte pas de protéines, il ne peut pas être classé."
                    } else {
                        null
                    }
                    refreshUnpriced()
                }
                .onFailure { e -> message.value = e.message }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
