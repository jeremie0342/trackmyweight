package com.kps.trackmyweight.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kps.trackmyweight.data.db.enums.CookingMethod
import com.kps.trackmyweight.data.db.enums.DietPhaseKind
import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.FoodRegion
import com.kps.trackmyweight.data.db.enums.MealType
import com.kps.trackmyweight.data.db.enums.PortionMode
import com.kps.trackmyweight.data.db.enums.WaterSource
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "food",
    indices = [
        Index(value = ["name"]),
        Index(value = ["barcode"]),
        Index(value = ["region"]),
        Index(value = ["category"]),
    ],
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val region: FoodRegion,
    val category: FoodCategory,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatsPer100g: Float,
    val fiberPer100g: Float = 0f,
    val sugarPer100g: Float? = null,
    val sodiumMgPer100g: Float? = null,
    val barcode: String? = null,
    val defaultServingG: Float = 100f,
    val servingLabel: String? = null,
    val isCustom: Boolean = false,
    val isVerified: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Table virtuelle FTS4 pour recherche insensible aux accents. */
@Fts4(contentEntity = FoodEntity::class)
@Entity(tableName = "food_fts")
data class FoodFtsEntity(
    val name: String,
    val brand: String?,
)

@Entity(
    tableName = "food_portion_alias",
    foreignKeys = [
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["foodId", "mode"], unique = true)],
)
data class FoodPortionAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val mode: PortionMode,
    val equivalentG: Float,
)

@Entity(
    tableName = "food_price",
    foreignKeys = [
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["foodId", "updatedAt"])],
)
data class FoodPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val currency: String,
    val pricePerServing: Float? = null,
    val pricePer100g: Float? = null,
    /** Coût pour obtenir 1g de protéine (calculé, snapshot). */
    val costPerGramProtein: Float,
    val updatedAt: Instant,
)

@Entity(
    tableName = "meal",
    indices = [Index(value = ["date", "mealType"])],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val eatenAt: Instant,
    val notes: String? = null,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)

@Entity(
    tableName = "meal_entry",
    foreignKeys = [
        ForeignKey(entity = MealEntity::class, parentColumns = ["id"], childColumns = ["mealId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("mealId"), Index("foodId")],
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long,
    val portionMode: PortionMode,
    val portionQuantity: Float,
    val resolvedGrams: Float,
    /** Méthode de cuisson choisie. NULL = pas d'ajustement (aliment déjà décrit tel quel). */
    val cookingMethod: CookingMethod? = null,
    // Snapshots figés au moment de la saisie (incluent déjà l'impact de la cuisson)
    val snapKcal: Float,
    val snapProteinG: Float,
    val snapCarbsG: Float,
    val snapFatsG: Float,
    val snapFiberG: Float,
    val snapSodiumMg: Float? = null,
    val snapCost: Float? = null,
)

@Entity(
    tableName = "favorite_meal",
    indices = [Index("lastUsedAt")],
)
data class FavoriteMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mealTypeHint: MealType? = null,
    val usageCount: Int = 0,
    val lastUsedAt: Instant? = null,
    val createdAt: Instant,
)

@Entity(
    tableName = "favorite_meal_entry",
    foreignKeys = [
        ForeignKey(entity = FavoriteMealEntity::class, parentColumns = ["id"], childColumns = ["favoriteMealId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("favoriteMealId"), Index("foodId")],
)
data class FavoriteMealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val favoriteMealId: Long,
    val foodId: Long,
    val portionMode: PortionMode,
    val portionQuantity: Float,
)

@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Int = 1,
    val notes: String? = null,
    val totalKcal: Float = 0f,
    val totalProteinG: Float = 0f,
    val totalCarbsG: Float = 0f,
    val totalFatsG: Float = 0f,
    val totalFiberG: Float = 0f,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "recipe_ingredient",
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["foodId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("recipeId"), Index("foodId")],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long,
    val grams: Float,
)

@Entity(
    tableName = "water_entry",
    indices = [Index(value = ["date"])],
)
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val timestamp: Instant,
    val volumeMl: Int,
    val source: WaterSource = WaterSource.MANUAL,
)

@Entity(
    tableName = "alcohol_entry",
    indices = [Index(value = ["date"])],
)
data class AlcoholEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val timestamp: Instant,
    val drink: String,
    val volumeMl: Int,
    val abvPct: Float,
    val alcoholGrams: Float,
    val kcal: Float,
)

@Entity(
    tableName = "diet_phase",
    indices = [Index(value = ["isActive"]), Index(value = ["startDate"])],
)
data class DietPhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val phase: DietPhaseKind,
    val targetKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatsG: Int,
    val notes: String? = null,
    val isActive: Boolean,
    val createdAt: Instant,
)
