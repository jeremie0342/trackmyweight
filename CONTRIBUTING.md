# Contributing to TrackMyWeight

Merci de ton intérêt ! Cette page décrit comment contribuer efficacement.

## Types de contributions bienvenues

- 🐛 **Bug reports** avec étapes de reproduction précises
- 💡 **Suggestions de features** (voir la roadmap dans le README avant)
- 📖 **Améliorations de documentation** (README, commentaires, exemples)
- 🥘 **Ajout d'aliments** à la base (Bénin, Afrique de l'Ouest, autres régions francophones)
- 🏋️ **Ajout d'exercices** à la bibliothèque
- 🌍 **Traductions** (l'app est actuellement en français uniquement)
- ✅ **Tests** supplémentaires (JVM ou instrumentés)
- 🎨 **Améliorations UI/UX**
- 🔧 **Refactorings** ciblés (attention aux gros PRs sans discussion préalable)

## Setup développement

### Prérequis

- **JDK 17** (Temurin recommandé)
- **Android SDK** (via Android Studio ou command-line tools)
- **Git**

### Sans Android Studio (PC peu puissant)

Tout le build peut se faire dans le cloud via GitHub Actions :

1. Fork le repo sur GitHub
2. Clone en local : `git clone git@github.com:<toi>/trackmyweight.git`
3. Édite avec ton IDE léger préféré (VS Code + extension Kotlin, IntelliJ Community, Zed)
4. Configure les 4 secrets GitHub Actions du fork (voir README section _Setup une seule fois_)
5. Push → APK compilé automatiquement dans les Releases

### Avec Android Studio

```bash
git clone git@github.com:<toi>/trackmyweight.git
cd trackmyweight
# Ouvre le dossier dans Android Studio, laisse-le sync
./gradlew assembleDebug
```

## Workflow PR

1. **Discussion d'abord** pour les features > 100 lignes de code — ouvre une issue "Proposal" avant de coder
2. **Branche** : `feat/short-name`, `fix/bug-description`, `docs/what`, `test/scope`
3. **Commits atomiques** avec messages descriptifs (impératif, en anglais ou français)
4. **Tests obligatoires** :
   - Nouveau code métier → tests JVM dans `src/test/`
   - Nouvelle query Room → test instrumenté dans `src/androidTest/`
5. **CI verte** : les tests JVM + build APK doivent passer
6. **PR description** : contexte du problème, solution, screenshots si UI

## Style de code

- Kotlin idiomatique — évite les getters manuels, les null checks défensifs superflus
- Compose : composables privés pour les sous-parties, pas de logique métier dans les screens
- Nommage : anglais pour le code, français OK pour les UI strings et commentaires métier
- Formatage : Android Studio default (Kotlin style)
- **Pas d'emoji** dans le code ni les UI strings
- **Pas de commentaires évidents** — le code doit se suffire

## Ajouter un aliment à la base

Ouvre `app/src/main/java/com/kps/trackmyweight/data/seed/FoodSeed.kt` et ajoute une ligne :

```kotlin
food("Nom local", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
    kcal = 165f, prot = 27f, carb = 0f, fat = 6f, fiber = 0f,
    servingG = 150f, servingLabel = "1 portion", now = now),
```

Cite tes sources dans la description du PR (tables FAO, CIQUAL, INRAN, etc.).

## Ajouter un exercice

Idem dans `ExerciseSeed.kt`. Format :

```kotlin
ex("slug_unique", "Nom affiché", MuscleGroup.PRIMARY, listOf(MuscleGroup.SECONDARY),
    ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("equipment_slug"), now),
```

Les slugs d'équipement doivent exister dans `EquipmentSeed.kt`.

## Reporting a bug

Ouvre une issue avec le template "Bug report". Inclus :
- Version de l'app (visible dans Settings)
- Modèle de téléphone + version Android
- Étapes reproductibles
- Comportement attendu vs observé
- Logs (`adb logcat -s TrackMyWeight` si tu as un PC)

## Reporting a security vulnerability

**Ne pas ouvrir d'issue publique.** Voir `SECURITY.md`.

## Code of Conduct

Ce projet suit le [Contributor Covenant](CODE_OF_CONDUCT.md). Sois respectueux, constructif, patient.

## Licence

En contribuant, tu acceptes que ta contribution soit publiée sous la licence MIT du projet.
