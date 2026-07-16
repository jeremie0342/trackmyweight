package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.FoodEntity
import com.kps.trackmyweight.data.db.enums.FoodCategory
import com.kps.trackmyweight.data.db.enums.FoodRegion
import kotlinx.datetime.Instant

/**
 * Base alimentaire orientée Bénin + international courant.
 * Valeurs approximatives issues des tables FAO West Africa + CIQUAL,
 * pour 100g d'aliment prêt à consommer (sauf mention).
 */
object FoodSeed {

    fun items(now: Instant): List<FoodEntity> = listOf(
        // ── Céréales, féculents (Bénin) ──
        food("Pâte de maïs (akassa)", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 120f, prot = 3f, carb = 27f, fat = 0.5f, fiber = 1f, servingG = 200f, servingLabel = "1 boule", now = now),
        food("Riz blanc cuit", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 130f, prot = 2.7f, carb = 28f, fat = 0.3f, fiber = 0.4f, servingG = 150f, servingLabel = "1 louche", now = now),
        food("Igname bouillie", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 120f, prot = 1.5f, carb = 28f, fat = 0.2f, fiber = 4f, servingG = 200f, servingLabel = "1 morceau", now = now),
        food("Foutou igname", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 150f, prot = 2f, carb = 34f, fat = 0.3f, fiber = 2f, servingG = 250f, servingLabel = "1 boule", now = now),
        food("Attiéké", FoodCategory.GRAIN, FoodRegion.WEST_AFRICA,
            kcal = 120f, prot = 1.5f, carb = 27f, fat = 0.3f, fiber = 1.5f, servingG = 200f, servingLabel = "1 portion", now = now),
        food("Gari (sec)", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 360f, prot = 1.5f, carb = 84f, fat = 0.5f, fiber = 2f, servingG = 100f, servingLabel = "1 tasse", now = now),
        food("Amiwo", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 140f, prot = 3f, carb = 28f, fat = 1.5f, fiber = 1f, servingG = 200f, servingLabel = "1 portion", now = now),
        food("Foutou manioc", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 155f, prot = 1f, carb = 37f, fat = 0.3f, fiber = 1.5f, servingG = 250f, servingLabel = "1 boule", now = now),
        food("Bouillie de mil", FoodCategory.GRAIN, FoodRegion.BENIN,
            kcal = 80f, prot = 2.5f, carb = 16f, fat = 0.8f, fiber = 1.2f, servingG = 250f, servingLabel = "1 bol", now = now),
        food("Pain blanc", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 265f, prot = 9f, carb = 49f, fat = 3f, fiber = 2.7f, servingG = 30f, servingLabel = "1 tranche", now = now),
        food("Spaghettis cuits", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 158f, prot = 5.8f, carb = 31f, fat = 0.9f, fiber = 1.8f, servingG = 200f, servingLabel = "1 assiette", now = now),
        food("Couscous cuit", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 112f, prot = 3.8f, carb = 23f, fat = 0.2f, fiber = 1.4f, servingG = 200f, servingLabel = "1 assiette", now = now),
        food("Flocons d'avoine (sec)", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 379f, prot = 13f, carb = 68f, fat = 6.5f, fiber = 10f, servingG = 50f, servingLabel = "1 portion", now = now),
        food("Pomme de terre bouillie", FoodCategory.GRAIN, FoodRegion.INTERNATIONAL,
            kcal = 87f, prot = 1.9f, carb = 20f, fat = 0.1f, fiber = 1.8f, servingG = 200f, servingLabel = "1 assiette", now = now),

        // ── Protéines animales ──
        food("Poulet bicyclette grillé", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 165f, prot = 27f, carb = 0f, fat = 6f, fiber = 0f, servingG = 150f, servingLabel = "1 cuisse", now = now),
        food("Poulet fermier (blanc)", FoodCategory.PROTEIN_ANIMAL, FoodRegion.INTERNATIONAL,
            kcal = 165f, prot = 31f, carb = 0f, fat = 3.6f, fiber = 0f, servingG = 120f, servingLabel = "1 filet", now = now),
        food("Tilapia braisé", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 120f, prot = 22f, carb = 0f, fat = 3f, fiber = 0f, servingG = 200f, servingLabel = "1 poisson moyen", now = now),
        food("Poisson chat fumé", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 180f, prot = 28f, carb = 0f, fat = 7f, fiber = 0f, servingG = 100f, servingLabel = "1 portion", now = now),
        food("Maquereau (Sardine boîte)", FoodCategory.PROTEIN_ANIMAL, FoodRegion.INTERNATIONAL,
            kcal = 208f, prot = 24.6f, carb = 0f, fat = 12f, fiber = 0f, servingG = 90f, servingLabel = "1 boîte", now = now),
        food("Thon en boîte au naturel", FoodCategory.PROTEIN_ANIMAL, FoodRegion.INTERNATIONAL,
            kcal = 116f, prot = 26f, carb = 0f, fat = 1f, fiber = 0f, servingG = 140f, servingLabel = "1 boîte", now = now),
        food("Bœuf (viande maigre)", FoodCategory.PROTEIN_ANIMAL, FoodRegion.INTERNATIONAL,
            kcal = 250f, prot = 26f, carb = 0f, fat = 15f, fiber = 0f, servingG = 120f, servingLabel = "1 morceau", now = now),
        food("Chèvre grillée", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 143f, prot = 27f, carb = 0f, fat = 3f, fiber = 0f, servingG = 150f, servingLabel = "1 portion", now = now),
        food("Œuf de poule locale", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 143f, prot = 12.5f, carb = 0.7f, fat = 10f, fiber = 0f, servingG = 50f, servingLabel = "1 œuf", now = now),
        food("Wagashi frit", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 240f, prot = 15f, carb = 3f, fat = 19f, fiber = 0f, servingG = 80f, servingLabel = "1 tranche", now = now),
        food("Wagashi grillé", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 200f, prot = 17f, carb = 3f, fat = 14f, fiber = 0f, servingG = 80f, servingLabel = "1 tranche", now = now),
        food("Crevettes fraîches", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
            kcal = 99f, prot = 24f, carb = 0.2f, fat = 0.3f, fiber = 0f, servingG = 100f, servingLabel = "1 portion", now = now),

        // ── Protéines végétales / légumineuses ──
        food("Haricots niébé cuits", FoodCategory.PROTEIN_PLANT, FoodRegion.BENIN,
            kcal = 116f, prot = 8f, carb = 20f, fat = 0.5f, fiber = 6f, servingG = 200f, servingLabel = "1 assiette", now = now),
        food("Lentilles cuites", FoodCategory.PROTEIN_PLANT, FoodRegion.INTERNATIONAL,
            kcal = 116f, prot = 9f, carb = 20f, fat = 0.4f, fiber = 8f, servingG = 200f, servingLabel = "1 assiette", now = now),
        food("Pois chiches cuits", FoodCategory.PROTEIN_PLANT, FoodRegion.INTERNATIONAL,
            kcal = 164f, prot = 9f, carb = 27f, fat = 2.6f, fiber = 8f, servingG = 200f, servingLabel = "1 assiette", now = now),
        food("Soja (tofu)", FoodCategory.PROTEIN_PLANT, FoodRegion.INTERNATIONAL,
            kcal = 76f, prot = 8f, carb = 1.9f, fat = 4.8f, fiber = 0.3f, servingG = 120f, servingLabel = "1 portion", now = now),
        food("Beignet haricot (ata)", FoodCategory.PROTEIN_PLANT, FoodRegion.BENIN,
            kcal = 240f, prot = 8f, carb = 25f, fat = 12f, fiber = 3f, servingG = 40f, servingLabel = "1 beignet", now = now),
        food("Kluiklui", FoodCategory.PROTEIN_PLANT, FoodRegion.BENIN,
            kcal = 480f, prot = 22f, carb = 20f, fat = 35f, fiber = 5f, servingG = 30f, servingLabel = "1 poignée", now = now),

        // ── Sauces & plats composés ──
        food("Sauce arachide", FoodCategory.SAUCE, FoodRegion.BENIN,
            kcal = 300f, prot = 10f, carb = 10f, fat = 25f, fiber = 4f, servingG = 100f, servingLabel = "1 louche", now = now),
        food("Sauce gombo", FoodCategory.SAUCE, FoodRegion.BENIN,
            kcal = 60f, prot = 3f, carb = 8f, fat = 2f, fiber = 3f, servingG = 100f, servingLabel = "1 louche", now = now),
        food("Sauce feuille (crincrin)", FoodCategory.SAUCE, FoodRegion.BENIN,
            kcal = 80f, prot = 4f, carb = 8f, fat = 3f, fiber = 4f, servingG = 100f, servingLabel = "1 louche", now = now),
        food("Sauce ademe", FoodCategory.SAUCE, FoodRegion.BENIN,
            kcal = 75f, prot = 3.5f, carb = 8f, fat = 3f, fiber = 3.5f, servingG = 100f, servingLabel = "1 louche", now = now),
        food("Sauce tomate au poisson", FoodCategory.SAUCE, FoodRegion.BENIN,
            kcal = 90f, prot = 7f, carb = 7f, fat = 4f, fiber = 2f, servingG = 100f, servingLabel = "1 louche", now = now),
        food("Sauce d'arachide au boeuf", FoodCategory.COMPOSED_DISH, FoodRegion.BENIN,
            kcal = 320f, prot = 15f, carb = 10f, fat = 25f, fiber = 4f, servingG = 200f, servingLabel = "1 assiette", now = now),

        // ── Légumes ──
        food("Épinards / feuilles cuites", FoodCategory.VEGETABLE, FoodRegion.INTERNATIONAL,
            kcal = 23f, prot = 3f, carb = 3.6f, fat = 0.4f, fiber = 2.4f, servingG = 100f, servingLabel = "1 poignée", now = now),
        food("Tomates fraîches", FoodCategory.VEGETABLE, FoodRegion.INTERNATIONAL,
            kcal = 18f, prot = 0.9f, carb = 3.9f, fat = 0.2f, fiber = 1.2f, servingG = 100f, servingLabel = "1 tomate moyenne", now = now),
        food("Oignon", FoodCategory.VEGETABLE, FoodRegion.INTERNATIONAL,
            kcal = 40f, prot = 1.1f, carb = 9f, fat = 0.1f, fiber = 1.7f, servingG = 100f, servingLabel = "1 oignon", now = now),
        food("Piment frais", FoodCategory.VEGETABLE, FoodRegion.BENIN,
            kcal = 40f, prot = 1.9f, carb = 8.8f, fat = 0.4f, fiber = 1.5f, servingG = 15f, servingLabel = "1 piment", now = now),
        food("Concombre", FoodCategory.VEGETABLE, FoodRegion.INTERNATIONAL,
            kcal = 16f, prot = 0.7f, carb = 3.6f, fat = 0.1f, fiber = 0.5f, servingG = 100f, servingLabel = "1 tranche", now = now),
        food("Carotte", FoodCategory.VEGETABLE, FoodRegion.INTERNATIONAL,
            kcal = 41f, prot = 0.9f, carb = 10f, fat = 0.2f, fiber = 2.8f, servingG = 80f, servingLabel = "1 carotte", now = now),

        // ── Fruits ──
        food("Banane douce", FoodCategory.FRUIT, FoodRegion.BENIN,
            kcal = 89f, prot = 1.1f, carb = 23f, fat = 0.3f, fiber = 2.6f, servingG = 120f, servingLabel = "1 banane", now = now),
        food("Mangue", FoodCategory.FRUIT, FoodRegion.BENIN,
            kcal = 60f, prot = 0.8f, carb = 15f, fat = 0.4f, fiber = 1.6f, servingG = 200f, servingLabel = "1 mangue", now = now),
        food("Papaye", FoodCategory.FRUIT, FoodRegion.BENIN,
            kcal = 43f, prot = 0.5f, carb = 11f, fat = 0.3f, fiber = 1.7f, servingG = 200f, servingLabel = "1 morceau", now = now),
        food("Ananas frais", FoodCategory.FRUIT, FoodRegion.BENIN,
            kcal = 50f, prot = 0.5f, carb = 13f, fat = 0.1f, fiber = 1.4f, servingG = 150f, servingLabel = "1 tranche", now = now),
        food("Orange", FoodCategory.FRUIT, FoodRegion.INTERNATIONAL,
            kcal = 47f, prot = 0.9f, carb = 12f, fat = 0.1f, fiber = 2.4f, servingG = 150f, servingLabel = "1 orange", now = now),
        food("Avocat", FoodCategory.FRUIT, FoodRegion.BENIN,
            kcal = 160f, prot = 2f, carb = 9f, fat = 15f, fiber = 7f, servingG = 100f, servingLabel = "1 demi", now = now),
        food("Pastèque", FoodCategory.FRUIT, FoodRegion.INTERNATIONAL,
            kcal = 30f, prot = 0.6f, carb = 8f, fat = 0.2f, fiber = 0.4f, servingG = 200f, servingLabel = "1 tranche", now = now),

        // ── Produits laitiers ──
        food("Yaourt nature", FoodCategory.DAIRY, FoodRegion.INTERNATIONAL,
            kcal = 59f, prot = 3.5f, carb = 4.7f, fat = 3.3f, fiber = 0f, servingG = 125f, servingLabel = "1 pot", now = now),
        food("Fromage blanc 0%", FoodCategory.DAIRY, FoodRegion.INTERNATIONAL,
            kcal = 46f, prot = 8f, carb = 4f, fat = 0.2f, fiber = 0f, servingG = 150f, servingLabel = "1 pot", now = now),
        food("Lait entier", FoodCategory.DAIRY, FoodRegion.INTERNATIONAL,
            kcal = 61f, prot = 3.2f, carb = 4.8f, fat = 3.3f, fiber = 0f, servingG = 250f, servingLabel = "1 verre", now = now),
        food("Lait en poudre entier", FoodCategory.DAIRY, FoodRegion.INTERNATIONAL,
            kcal = 496f, prot = 27f, carb = 39f, fat = 27f, fiber = 0f, servingG = 30f, servingLabel = "3 c. à soupe", now = now),

        // ── Matières grasses ──
        food("Huile de palme rouge", FoodCategory.FAT, FoodRegion.BENIN,
            kcal = 899f, prot = 0f, carb = 0f, fat = 100f, fiber = 0f, servingG = 15f, servingLabel = "1 c. à soupe", now = now),
        food("Huile végétale", FoodCategory.FAT, FoodRegion.INTERNATIONAL,
            kcal = 884f, prot = 0f, carb = 0f, fat = 100f, fiber = 0f, servingG = 15f, servingLabel = "1 c. à soupe", now = now),
        food("Beurre de cacahuète", FoodCategory.FAT, FoodRegion.INTERNATIONAL,
            kcal = 588f, prot = 25f, carb = 20f, fat = 50f, fiber = 6f, servingG = 20f, servingLabel = "1 c. à soupe", now = now),
        food("Beurre", FoodCategory.FAT, FoodRegion.INTERNATIONAL,
            kcal = 717f, prot = 0.9f, carb = 0.1f, fat = 81f, fiber = 0f, servingG = 10f, servingLabel = "1 noix", now = now),

        // ── Boissons ──
        food("Eau", FoodCategory.BEVERAGE, FoodRegion.INTERNATIONAL,
            kcal = 0f, prot = 0f, carb = 0f, fat = 0f, fiber = 0f, servingG = 250f, servingLabel = "1 verre", now = now),
        food("Café noir", FoodCategory.BEVERAGE, FoodRegion.INTERNATIONAL,
            kcal = 2f, prot = 0.3f, carb = 0f, fat = 0f, fiber = 0f, servingG = 150f, servingLabel = "1 tasse", now = now),
        food("Bissap (jus)", FoodCategory.BEVERAGE, FoodRegion.BENIN,
            kcal = 45f, prot = 0f, carb = 11f, fat = 0f, fiber = 0.5f, servingG = 250f, servingLabel = "1 verre", now = now),
        food("Sodabi (2cl)", FoodCategory.BEVERAGE, FoodRegion.BENIN,
            kcal = 60f, prot = 0f, carb = 0f, fat = 0f, fiber = 0f, servingG = 20f, servingLabel = "1 shot", now = now),

        // ── Snacks ──
        food("Cacahuètes grillées", FoodCategory.SNACK, FoodRegion.BENIN,
            kcal = 585f, prot = 26f, carb = 21f, fat = 49f, fiber = 8f, servingG = 30f, servingLabel = "1 poignée", now = now),
        food("Chocolat noir 70%", FoodCategory.SNACK, FoodRegion.INTERNATIONAL,
            kcal = 598f, prot = 8f, carb = 46f, fat = 43f, fiber = 11f, servingG = 25f, servingLabel = "3 carrés", now = now),
    )

    private fun food(
        name: String,
        category: FoodCategory,
        region: FoodRegion,
        kcal: Float,
        prot: Float,
        carb: Float,
        fat: Float,
        fiber: Float,
        servingG: Float,
        servingLabel: String,
        now: Instant,
    ): FoodEntity = FoodEntity(
        name = name,
        region = region,
        category = category,
        kcalPer100g = kcal,
        proteinPer100g = prot,
        carbsPer100g = carb,
        fatsPer100g = fat,
        fiberPer100g = fiber,
        defaultServingG = servingG,
        servingLabel = servingLabel,
        isVerified = true,
        createdAt = now,
        updatedAt = now,
    )
}
