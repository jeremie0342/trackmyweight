package com.kps.trackmyweight.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.kps.trackmyweight.data.db.entity.AlcoholEntryEntity
import com.kps.trackmyweight.data.db.entity.DietPhaseEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntity
import com.kps.trackmyweight.data.db.entity.FavoriteMealEntryEntity
import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.entity.FoodPortionAliasEntity
import com.kps.trackmyweight.data.db.entity.FoodPriceEntity
import com.kps.trackmyweight.data.db.entity.MealEntity
import com.kps.trackmyweight.data.db.entity.MealEntryEntity
import com.kps.trackmyweight.data.db.entity.RecipeEntity
import com.kps.trackmyweight.data.db.entity.RecipeIngredientEntity
import com.kps.trackmyweight.data.db.entity.WaterEntryEntity
import com.kps.trackmyweight.data.db.enums.FoodRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface NutritionDao {

    // ── Foods ─────────────────────────────────────────────
    @Upsert
    suspend fun upsertFood(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(foods: List<FoodEntity>): List<Long>

    @Query("SELECT * FROM food WHERE id = :id LIMIT 1")
    suspend fun getFood(id: Long): FoodEntity?

    @Query("SELECT * FROM food WHERE barcode = :code LIMIT 1")
    suspend fun getFoodByBarcode(code: String): FoodEntity?

    @Query("SELECT * FROM food WHERE region = :region ORDER BY name LIMIT :limit")
    fun observeFoodsByRegion(region: FoodRegion, limit: Int = 500): Flow<List<FoodEntity>>

    /** Liste par défaut à afficher quand la recherche est vide : régions locales d'abord. */
    @Query("""
        SELECT * FROM food
        ORDER BY
            CASE region
                WHEN 'BENIN' THEN 0
                WHEN 'WEST_AFRICA' THEN 1
                WHEN 'INTERNATIONAL' THEN 2
                ELSE 3
            END,
            name
        LIMIT :limit
    """)
    suspend fun browseFoods(limit: Int = 60): List<FoodEntity>

    /** Recherche FTS insensible aux accents. */
    @Query("""
        SELECT f.* FROM food f
        JOIN food_fts ON food_fts.rowid = f.id
        WHERE food_fts MATCH :query
        ORDER BY f.name
        LIMIT :limit
    """)
    suspend fun searchFoods(query: String, limit: Int = 30): List<FoodEntity>

    // ── Portion aliases ───────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPortionAliases(aliases: List<FoodPortionAliasEntity>)

    @Query("SELECT * FROM food_portion_alias WHERE foodId = :foodId")
    suspend fun getPortionAliases(foodId: Long): List<FoodPortionAliasEntity>

    // ── Prices ────────────────────────────────────────────
    @Insert
    suspend fun insertPrice(price: FoodPriceEntity): Long

    @Query("SELECT * FROM food_price WHERE foodId = :foodId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestPrice(foodId: Long): FoodPriceEntity?

    /** Classement des aliments par € (ou FCFA) par gramme de protéine — croissant. */
    @Query("""
        SELECT f.* FROM food f
        INNER JOIN (
            SELECT foodId, MIN(costPerGramProtein) AS minCost
            FROM food_price
            GROUP BY foodId
        ) fp ON fp.foodId = f.id
        ORDER BY fp.minCost ASC
        LIMIT :limit
    """)
    suspend fun getFoodsRankedByProteinCost(limit: Int = 20): List<FoodEntity>

    // ── Meals ─────────────────────────────────────────────
    @Insert
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("SELECT * FROM meal WHERE date = :date AND deletedAt IS NULL ORDER BY eatenAt")
    fun observeMealsOnDate(date: LocalDate): Flow<List<MealEntity>>

    @Query("UPDATE meal SET deletedAt = :now WHERE id = :id")
    suspend fun softDeleteMeal(id: Long, now: kotlinx.datetime.Instant)

    @Insert
    suspend fun insertMealEntry(entry: MealEntryEntity): Long

    @Query("SELECT * FROM meal_entry WHERE mealId = :mealId")
    suspend fun getMealEntries(mealId: Long): List<MealEntryEntity>

    @Query("DELETE FROM meal_entry WHERE id = :id")
    suspend fun deleteMealEntry(id: Long)

    /** Totaux macros pour une journée (rapide, agrégat SQL). */
    @Query("""
        SELECT
            IFNULL(SUM(me.snapKcal), 0) AS kcal,
            IFNULL(SUM(me.snapProteinG), 0) AS protein,
            IFNULL(SUM(me.snapCarbsG), 0) AS carbs,
            IFNULL(SUM(me.snapFatsG), 0) AS fats,
            IFNULL(SUM(me.snapFiberG), 0) AS fiber
        FROM meal_entry me
        INNER JOIN meal m ON m.id = me.mealId
        WHERE m.date = :date AND m.deletedAt IS NULL
    """)
    fun observeDailyMacros(date: LocalDate): Flow<DailyMacrosProjection>

    // ── Favorites ─────────────────────────────────────────
    @Insert
    suspend fun insertFavorite(fav: FavoriteMealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFavoriteEntries(entries: List<FavoriteMealEntryEntity>)

    @Query("SELECT * FROM favorite_meal ORDER BY usageCount DESC, name")
    fun observeFavorites(): Flow<List<FavoriteMealEntity>>

    @Query("SELECT * FROM favorite_meal_entry WHERE favoriteMealId = :favId")
    suspend fun getFavoriteEntries(favId: Long): List<FavoriteMealEntryEntity>

    @Query("UPDATE favorite_meal SET usageCount = usageCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun bumpFavoriteUsage(id: Long, now: kotlinx.datetime.Instant)

    // ── Recipes ───────────────────────────────────────────
    @Upsert
    suspend fun upsertRecipe(r: RecipeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRecipeIngredients(ings: List<RecipeIngredientEntity>)

    @Query("SELECT * FROM recipe ORDER BY name")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId")
    suspend fun getRecipeIngredients(recipeId: Long): List<RecipeIngredientEntity>

    // ── Water & alcohol ───────────────────────────────────
    @Insert
    suspend fun insertWater(w: WaterEntryEntity): Long

    @Query("SELECT IFNULL(SUM(volumeMl), 0) FROM water_entry WHERE date = :date")
    fun observeWaterMlForDate(date: LocalDate): Flow<Int>

    @Insert
    suspend fun insertAlcohol(a: AlcoholEntryEntity): Long

    @Query("SELECT * FROM alcohol_entry WHERE date >= :from AND date <= :to ORDER BY timestamp")
    suspend fun getAlcoholInRange(from: LocalDate, to: LocalDate): List<AlcoholEntryEntity>

    // ── Diet phases ───────────────────────────────────────
    @Insert
    suspend fun insertDietPhase(p: DietPhaseEntity): Long

    @Query("UPDATE diet_phase SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllPhases()

    @Query("SELECT * FROM diet_phase WHERE isActive = 1 LIMIT 1")
    fun observeActivePhase(): Flow<DietPhaseEntity?>

    @Query("SELECT * FROM diet_phase ORDER BY startDate DESC")
    fun observePhasesHistory(): Flow<List<DietPhaseEntity>>

    @Transaction
    suspend fun switchActivePhase(newPhase: DietPhaseEntity) {
        deactivateAllPhases()
        insertDietPhase(newPhase)
    }
}

/** Projection utilisée par [NutritionDao.observeDailyMacros]. */
data class DailyMacrosProjection(
    val kcal: Float,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float,
)
