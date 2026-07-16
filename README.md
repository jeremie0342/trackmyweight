# TrackMyWeight

Application Android native (Kotlin + Jetpack Compose + Room) de suivi de transformation
physique (poids, mensurations, photos, entraînements, nutrition adaptée au Bénin,
habitudes, récupération).

## Workflow de build sans PC puissant

Ce projet est configuré pour être **compilé dans le cloud** via GitHub Actions et publié
sur GitHub Releases. Tu édites le code en local (VS Code, IntelliJ Community, ou n'importe
quel éditeur), tu pushes, GitHub construit l'APK et le publie automatiquement.

### Setup une seule fois

**1. Créer un repo GitHub privé** avec ce projet.

**2. Générer un keystore debug stable** (une seule fois, garde-le précieusement).
Sur un ordi qui a Java installé (n'importe quel PC, ami, cybercafé, Codespaces...) :

```bash
keytool -genkeypair -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10950 \
  -dname "CN=TrackMyWeight Debug,O=Personal,C=BJ"
```

Cela produit `debug.keystore` (~2 Ko).

**3. Encoder en base64** pour le mettre dans un secret GitHub :

```bash
# Linux / macOS
base64 -w0 debug.keystore > debug.keystore.b64
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("debug.keystore")) | Set-Content debug.keystore.b64
```

**4. Ajouter 4 secrets GitHub** dans _Settings → Secrets and variables → Actions_ :

| Secret | Valeur |
|---|---|
| `DEBUG_KEYSTORE_BASE64` | contenu de `debug.keystore.b64` |
| `DEBUG_KEYSTORE_PASSWORD` | `android` (celui utilisé au step 2) |
| `DEBUG_KEY_ALIAS` | `androiddebugkey` |
| `DEBUG_KEY_PASSWORD` | `android` |

### Utilisation quotidienne

1. Édite le code sur ton PC (VS Code + extension Kotlin conseillé).
2. `git commit && git push`.
3. GitHub Actions build l'APK (~4-6 min).
4. Va sur l'onglet _Releases_ du repo → télécharge le dernier APK.
5. Installe sur ton tel (autoriser sources inconnues la première fois).
6. Les builds suivants remplacent l'app sans désinstallation (signature identique).

Tu peux aussi récupérer l'APK depuis l'onglet _Actions → run → artifacts_ si tu ne
veux pas de release publique.

## Structure

```
app/src/main/java/com/kps/trackmyweight/
├── TrackMyWeightApp.kt           Application Hilt + WorkManager
├── MainActivity.kt               Entry Compose
├── ui/theme/                     Palette dark-first, typo tabulaire, Material You
├── data/db/
│   ├── enums/                    Enums typés (Sex, MuscleGroup, PortionMode, ...)
│   ├── entity/                   45 entités Room (User, Body, Workout, Nutrition, Habit, ...)
│   ├── dao/                      7 DAOs (User, Body, Exercise, Workout, Nutrition, Habit, AnalyticsMeta)
│   ├── converters/               TypeConverters (Instant, LocalDate, enums, JSON)
│   └── TrackMyWeightDatabase.kt  Room database, version 1
└── di/                           Modules Hilt
```

## Stack technique

- Kotlin 2.1, Jetpack Compose (BOM 2024.12), Material 3
- Room 2.6 (FTS4, migrations testées)
- Hilt 2.53 (DI)
- WorkManager (rappels, backups)
- Health Connect 1.1
- CameraX 1.4 (photos progression)
- Vico (graphiques Compose-natifs)
- Coil (image loading)
- SplashScreen API, Biometric, Security Crypto
- Min SDK 26, Target SDK 35

## Local dev (si tu as la RAM)

Ouvre le dossier dans Android Studio, laisse-le générer le wrapper Gradle et sync.
Sinon, [Zed](https://zed.dev) ou VS Code + extension Kotlin marchent pour l'édition,
build via `./gradlew :app:assembleDebug`.
