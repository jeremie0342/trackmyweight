# Contributing to TrackMyWeight

Thanks for your interest! This page describes how to contribute effectively.

## Welcome contributions

- 🐛 **Bug reports** with precise reproduction steps
- 💡 **Feature suggestions** (check the roadmap in the README first)
- 📖 **Documentation improvements** (README, code comments, examples)
- 🥘 **Food additions** to the database (Benin, West Africa, other regions)
- 🏋️ **New exercises** in the library
- 🌍 **Translations** (the app is currently French-only)
- ✅ **Additional tests** (JVM or instrumented)
- 🎨 **UI/UX improvements**
- 🔧 **Focused refactors** (avoid large refactors without prior discussion)

## Development setup

### Prerequisites

- **JDK 17** (Temurin recommended)
- **Android SDK** (via Android Studio or command-line tools)
- **Git**

### Without Android Studio (low-spec PC)

All builds can happen in the cloud via GitHub Actions:

1. Fork the repo on GitHub
2. Clone locally: `git clone git@github.com:<you>/trackmyweight.git`
3. Edit with your favorite lightweight IDE (VS Code + Kotlin extension, IntelliJ Community, Zed)
4. Configure the 4 GitHub Actions secrets on your fork (see `docs/CI_SETUP.md` — coming soon; for now generate a debug keystore and set `DEBUG_KEYSTORE_BASE64`, `DEBUG_KEYSTORE_PASSWORD=android`, `DEBUG_KEY_ALIAS=androiddebugkey`, `DEBUG_KEY_PASSWORD=android`)
5. Push → APK is built automatically and published to Releases

### With Android Studio

```bash
git clone git@github.com:<you>/trackmyweight.git
cd trackmyweight
# Open the folder in Android Studio and let it sync
./gradlew assembleDebug
```

## PR workflow

1. **Discuss first** for features > 100 lines of code — open a "Proposal" issue before coding
2. **Branch naming**: `feat/short-name`, `fix/bug-description`, `docs/what`, `test/scope`
3. **Atomic commits** with descriptive messages (imperative mood, English preferred)
4. **Tests required**:
   - New business logic → JVM tests in `src/test/`
   - New Room query → instrumented test in `src/androidTest/`
5. **Green CI**: JVM tests + APK build must pass
6. **PR description**: problem context, solution, screenshots if UI

## Code style

- Idiomatic Kotlin — avoid manual getters, unnecessary defensive null checks
- Compose: private composables for sub-parts, no business logic in screens
- Naming: English for code, French acceptable for UI strings and domain comments
- Formatting: Android Studio default (Kotlin style)
- **No emoji** in code or UI strings
- **No obvious comments** — the code should speak for itself

## Adding a food to the database

Open `app/src/main/java/com/kps/trackmyweight/data/seed/FoodSeed.kt` and add a line:

```kotlin
food("Local name", FoodCategory.PROTEIN_ANIMAL, FoodRegion.BENIN,
    kcal = 165f, prot = 27f, carb = 0f, fat = 6f, fiber = 0f,
    servingG = 150f, servingLabel = "1 portion", now = now),
```

Cite your sources in the PR description (FAO tables, CIQUAL, INRAN, etc.).

## Adding an exercise

Same in `ExerciseSeed.kt`. Format:

```kotlin
ex("unique_slug", "Display name", MuscleGroup.PRIMARY, listOf(MuscleGroup.SECONDARY),
    ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("equipment_slug"), now),
```

Equipment slugs must exist in `EquipmentSeed.kt`.

## Reporting a bug

Open an issue using the "Bug report" template. Include:
- App version (visible in Settings)
- Phone model + Android version
- Reproducible steps
- Expected vs observed behavior
- Logs (`adb logcat -s TrackMyWeight` if you have a PC)

## Reporting a security vulnerability

**Do not open a public issue.** See `SECURITY.md`.

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Be respectful, constructive, patient.

## License

By contributing, you agree that your contribution is released under the project's MIT license.
