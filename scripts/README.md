# Scripts

Outillage hors-build du catalogue d'exercices. Rien ici n'est exécuté par Gradle
ni embarqué dans l'APK — ce sont des générateurs lancés à la main, dont le
résultat (le seed Kotlin et les visuels) est commité dans le dépôt.

Prérequis : Python 3.9+ et `pip install Pillow`.

## Chaîne

```
catalog.py                     source de vérité
   │                             E                = 200 exercices curés
   │                             EQUIPMENT_MEDIA  = 38 visuels d'équipement
   │
   ├─ fetch_exercise_media.py  → assets/exercises/<slug>_{0,1}.webp
   │                           → assets/equipment/<clé>.webp
   ├─ generate_exercise_seed.py→ data/seed/ExerciseSeed.kt
   │                           → data/seed/EquipmentMedia.kt
   └─ check_exercise_seed.py     contrôles statiques du résultat
```

## Ajouter ou modifier un exercice

1. Éditer `catalog.py`. Chaque ligne référence un `src` — l'identifiant de
   l'exercice dans [free-exercise-db](https://github.com/yuhonas/free-exercise-db),
   qui fournit les deux visuels (position de départ et position finale).
2. Régénérer :

   ```bash
   python scripts/fetch_exercise_media.py      # n'écrit que ce qui manque
   python scripts/generate_exercise_seed.py
   python scripts/check_exercise_seed.py
   ```

3. Commiter `catalog.py`, `ExerciseSeed.kt` et les `.webp` ajoutés.

## Règles

- **Ne jamais modifier un `slug` déjà publié.** C'est la clé unique en base ; les
  séances, records et templates de l'utilisateur y sont rattachés. Renommer un
  slug reviendrait à créer un exercice neuf et à orphaniser l'historique.
- **Ne pas éditer `ExerciseSeed.kt` à la main** : il est régénéré et tout
  changement direct serait écrasé.
- Le nom affiché, les muscles, la mécanique, les cues et le `mediaPath` sont en
  revanche resynchronisés à chaque lancement de l'app sur les exercices non
  personnalisés (voir `ExerciseRepository.syncCatalog`) : les corriger dans
  `catalog.py` suffit à les propager aux installs existantes.

## Réglages d'image

Par défaut : largeur 640 px, WebP qualité 80 — environ 26 Ko par image, soit
~11,3 Mo pour les 400 visuels d'exercice et les 38 visuels d'équipement.
Pour alléger l'APK :

```bash
python scripts/fetch_exercise_media.py --width 512 --quality 75 --force
```

## Licence de la source

free-exercise-db est publié sous **The Unlicense** (domaine public) : aucune
attribution n'est juridiquement exigée, aucune contrainte de partage à
l'identique. Le dépôt crédite en amont `exercises.json` d'Ollie Jennings sans
documenter intégralement la provenance des images ; par prudence, l'app affiche
malgré tout un crédit dans Réglages → À propos.
