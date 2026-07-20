package com.kps.trackmyweight.data.repository

import androidx.room.withTransaction
import com.kps.trackmyweight.data.db.TrackMyWeightDatabase
import com.kps.trackmyweight.data.db.dao.DailyMacrosProjection
import com.kps.trackmyweight.data.db.dao.NutritionDao
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntryEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.FoodPortionAliasEntity
import com.kps.trackmyweight.data.db.entity.MealEntity
import com.kps.trackmyweight.data.db.entity.MealEntryEntity
import com.kps.trackmyweight.data.db.enums.CookingMethod
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.seed.FoodSeed
import com.kps.trackmyweight.domain.calc.CookingImpact
import com.kps.trackmyweight.domain.calc.MacroCalculator
import com.kps.trackmyweight.domain.calc.PortionResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class MealWithEntries(
    val meal: MealEntity,
    val entries: List<MealEntryWithFood>,
) {
    val totalKcal: Float get() = entries.sumOf { it.entry.snapKcal.toDouble() }.toFloat()
    val totalProteinG: Float get() = entries.sumOf { it.entry.snapProteinG.toDouble() }.toFloat()
}

data class MealEntryWithFood(
    val entry: MealEntryEntity,
    val food: FoodEntity?,
)

@Singleton
class NutritionRepository @Inject constructor(
    private val db: TrackMyWeightDatabase,
    private val nutritionDao: NutritionDao,
) {
    // ── Seed ─────────────────────────────────────────
    suspend fun seedIfEmpty() {
        val currentFoods = nutritionDao.observeFoodsByRegion(
            com.kps.trackmyweight.data.db.enums.FoodRegion.BENIN, limit = 1
        ).first()
        if (currentFoods.isNotEmpty()) return
        val now = Clock.System.now()
        val seeded = FoodSeed.seeded(now)
        db.withTransaction {
            seeded.forEach { entry ->
                val foodId = nutritionDao.upsertFood(entry.food)
                if (entry.aliases.isNotEmpty()) {
                    nutritionDao.setPortionAliases(
                        entry.aliases.map { spec ->
                            FoodPortionAliasEntity(foodId = foodId, mode = spec.mode, equivalentG = spec.grams)
                        }
                    )
                }
            }
        }
    }

    suspend fun getAliases(foodId: Long): List<FoodPortionAliasEntity> =
        nutritionDao.getPortionAliases(foodId)

    // ── Foods ────────────────────────────────────────
    suspend fun searchFoods(query: String, limit: Int = 30): List<FoodEntity> {
        // Sanitize FTS query: MATCH doesn't accept apostrophes etc. On split en tokens.
        val safe = query.trim().replace("'", " ").replace("\"", " ")
            .split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        // Query vide → on affiche une liste par défaut browsable (Bénin d'abord).
        if (safe.isEmpty()) return runCatching { nutritionDao.browseFoods(limit) }.getOrDefault(emptyList())
        return runCatching { nutritionDao.searchFoods(safe, limit) }.getOrDefault(emptyList())
    }

    suspend fun getFood(id: Long): FoodEntity? = nutritionDao.getFood(id)
    suspend fun getFoodByBarcode(code: String): FoodEntity? = nutritionDao.getFoodByBarcode(code)

    // ── Meals ────────────────────────────────────────
    fun observeMealsOnDate(date: LocalDate): Flow<List<MealEntity>> = nutritionDao.observeMealsOnDate(date)

    /** Renvoie tous les repas d'une date + leurs entrées (avec info aliment). */
    suspend fun getMealsWithEntries(date: LocalDate): List<MealWithEntries> {
        val meals = nutritionDao.observeMealsOnDate(date).first()
        return meals.map { m ->
            val entries = nutritionDao.getMealEntries(m.id).map { e ->
                MealEntryWithFood(e, nutritionDao.getFood(e.foodId))
            }
            MealWithEntries(m, entries)
        }
    }

    fun observeDailyMacros(date: LocalDate): Flow<DailyMacrosProjection> = nutritionDao.observeDailyMacros(date)

    /** Ajoute une entrée à un repas (crée le repas s'il n'existe pas pour cette date + type). */
    suspend fun addEntry(
        date: LocalDate,
        mealType: MealType,
        foodId: Long,
        portionMode: PortionMode,
        portionQuantity: Float,
        cookingMethod: CookingMethod? = null,
        aliasGramsForMode: Float? = null,
    ): Long = db.withTransaction {
        val now = Clock.System.now()
        val food = nutritionDao.getFood(foodId) ?: error("Food not found")
        // Si l'appelant ne fournit pas d'alias explicite, on cherche automatiquement
        // dans la table d'aliases spécifiques à l'aliment.
        val effectiveAlias = aliasGramsForMode
            ?: nutritionDao.getPortionAliases(foodId).firstOrNull { it.mode == portionMode }?.equivalentG
        val grams = PortionResolver.resolveGrams(
            mode = portionMode,
            quantity = portionQuantity,
            aliasGramsForMode = effectiveAlias,
            defaultServingG = food.defaultServingG,
            category = food.category,
        )
        val macros = CookingImpact.apply(MacroCalculator.snapshot(food, grams), cookingMethod, grams)

        val existingMeal = nutritionDao.observeMealsOnDate(date).first()
            .firstOrNull { it.mealType == mealType }
        val mealId = existingMeal?.id ?: nutritionDao.insertMeal(
            MealEntity(
                date = date,
                mealType = mealType,
                eatenAt = now,
                createdAt = now,
            )
        )

        nutritionDao.insertMealEntry(
            MealEntryEntity(
                mealId = mealId,
                foodId = foodId,
                portionMode = portionMode,
                portionQuantity = portionQuantity,
                resolvedGrams = grams,
                cookingMethod = cookingMethod,
                snapKcal = macros.kcal,
                snapProteinG = macros.proteinG,
                snapCarbsG = macros.carbsG,
                snapFatsG = macros.fatsG,
                snapFiberG = macros.fiberG,
                snapSodiumMg = macros.sodiumMg,
            )
        )
    }

    suspend fun deleteEntry(entryId: Long) {
        nutritionDao.deleteMealEntry(entryId)
    }

    // ── Favorites ────────────────────────────────────
    fun observeFavorites(): Flow<List<FavoriteMealEntity>> = nutritionDao.observeFavorites()

    suspend fun createFavoriteFromMeal(name: String, mealTypeHint: MealType?, meal: MealWithEntries): Long = db.withTransaction {
        val now = Clock.System.now()
        val favId = nutritionDao.insertFavorite(
            FavoriteMealEntity(name = name, mealTypeHint = mealTypeHint, createdAt = now)
        )
        val entries = meal.entries.map { e ->
            FavoriteMealEntryEntity(
                favoriteMealId = favId,
                foodId = e.entry.foodId,
                portionMode = e.entry.portionMode,
                portionQuantity = e.entry.portionQuantity,
            )
        }
        if (entries.isNotEmpty()) nutritionDao.setFavoriteEntries(entries)
        favId
    }

    suspend fun applyFavorite(date: LocalDate, mealType: MealType, favoriteId: Long) = db.withTransaction {
        val entries = nutritionDao.getFavoriteEntries(favoriteId)
        entries.forEach { e ->
            addEntry(date, mealType, e.foodId, e.portionMode, e.portionQuantity)
        }
        nutritionDao.bumpFavoriteUsage(favoriteId, Clock.System.now())
    }

    // ── Diet phase (adaptation calories) ─────────────
    fun observeActivePhase(): Flow<DietPhaseEntity?> = nutritionDao.observeActivePhase()

    suspend fun updateActivePhase(newTargetKcal: Int, newTargetProteinG: Int? = null) {
        val current = nutritionDao.observeActivePhase().first() ?: return
        val updated = current.copy(
            targetKcal = newTargetKcal,
            targetProteinG = newTargetProteinG ?: current.targetProteinG,
        )
        nutritionDao.switchActivePhase(updated.copy(id = 0, isActive = true, startDate = today()))
    }

    /** Remplace la phase active complète (kind + toutes les cibles macros). */
    suspend fun setActivePhase(phase: DietPhaseEntity) {
        nutritionDao.switchActivePhase(phase.copy(id = 0, isActive = true, startDate = today()))
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
