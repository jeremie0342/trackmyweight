# TrackMyWeight

**Local-first fitness & nutrition tracker for Android — optimized for Benin / West African cuisine.**

An all-in-one app to drive a physical transformation: weight, measurements, photos, workout log, nutrition adapted to local foods, habits, recovery, weekly report with algorithmic coaching.

No data is ever sent to a server. Everything stays on your phone, with optional Health Connect sync and manual JSON backup to your own cloud.

[![Build & Release](https://github.com/jeremie0342/trackmyweight/actions/workflows/android-build.yml/badge.svg)](https://github.com/jeremie0342/trackmyweight/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/minSdk-26-green.svg)](https://developer.android.com/about/versions/oreo)
[![Release](https://img.shields.io/github/v/release/jeremie0342/trackmyweight?include_prereleases)](https://github.com/jeremie0342/trackmyweight/releases/latest)

> **Note on language:** the app's UI is currently in French. English translation is planned (see roadmap). Documentation, code, and contribution process are in English to welcome global contributors.

---

## Why this app

Most fitness trackers assume a Western user with a smart scale, a high-end watch, and standardized food. This app is built for:

- 📱 An Android phone with no mandatory accessories
- 🥘 A local food database (pâte, foutou, attiéké, wagashi, peanut/gombo/leaf sauces, free-range chicken, tilapia, kluiklui, bissap, etc.)
- 💰 A budget-conscious user (auto cost per gram of protein in FCFA)
- 🏋️ Daily gym use with variable equipment
- 👨‍🏫 Coexistence with a human coach (passive mode that logs without contradicting)
- ⚡ Pure local-first — no account, no telemetry, no ads

## Screenshots

Official captures will be added under `docs/screenshots/`. See [docs/README.md](docs/README.md) to contribute.

| Today | Workout | Nutrition |
|-------|---------|-----------|
| _tbd_ | _tbd_ | _tbd_ |

| Body | Weekly report | Widget |
|------|---------------|--------|
| _tbd_ | _tbd_ | _tbd_ |

## Features

### Body tracking
- Daily weigh-in with **chart + 7-day moving average + linear projection**
- Home screen widget with **+ Add weigh-in** 1-tap button
- 14-field measurements + auto **Navy method body fat %** (no impedance scale required)
- Progress photos with **silhouette overlay** of your last shot for alignment
- **MP4 timelapse** generated on demand, shareable
- Health ratios (WHtR, WHR)
- Auto **stagnation detection** (14 days without progress)

### Workout journal
- Library of **~60 exercises** with primary/secondary muscles + equipment
- Auto-filtering by **your gym's equipment** (multi-gym supported)
- Session templates with **rotation** (e.g. Monday arms/legs alternating)
- Active session: **auto-fill** last performance, adaptive rest timer (compound 3min / isolation 90s)
- **Automatic PR detection** (max weight, estimated 1RM Epley/Brzycki, max reps at a given weight)
- **Weekly volume per muscle group** vs MEV/MAV/MRV landmarks (Renaissance Periodization)
- **French voice input**: "12 reps à 80 kilos" → fields auto-filled
- Cardio: 9 activities with **MET-based kcal estimation** modulated by RPE
- **Text export** to send to your coach

### Nutrition
- **Benin food database** (~60 items) + **international staples**
- **3 stackable logging modes** per meal:
  - Visual portions (fist, palm, thumb, ladle, spoon)
  - Precise grams (for the curious)
  - Favorite meals 1-tap
- Daily **protein + calorie counter** vs personalized target
- **Protein distribution** across meals with qualitative advice
- Fiber, sodium, alcohol tracked separately
- **Cost per gram of protein in FCFA** — auto-ranks your foods by value
- Diet phases: **cut / recomp / bulk / maintenance / refeed / diet break**
- **Automatic calorie adaptation** based on your actual weight trend

### Habits & recovery
- "Today" dashboard with cards for readiness, weight, macros, sleep, water, habits, resting pulse
- **7 default habits** (morning weigh-in, creatine, 10k steps, 2L water, sleep ≥7h, stretching, no alcohol)
- **Streaks** in consecutive days (recursive SQL CTE)
- **Morning readiness check-in** — 15 seconds (sleep/energy/soreness/mood → 0-5 score with advice)
- Water counter with 250/500/750/1000 ml presets

### Analytics & coaching
- **Weekly report**: adherence weighted across 6 signals, auto narrative, non-linear projection toward goal with ETA
- **Coach Advisor** with 10 prioritized rules:
  - Auto-suggested refeed week after 8 weeks in cut
  - Deload if average readiness < 2.5/5
  - Calorie adjustment on stagnation > 14d or loss > 0.9kg/week
  - Volume above MRV per muscle group
  - Insufficient sleep
- **One-tap application** of a 7-day refeed

### System
- **Health Connect**: auto read of weight / steps / sleep every 12h (opt-in)
- **Full JSON backup / restore** — export to Drive/iCloud/USB
- **Context-aware notifications** (morning weigh-in, hydration, session not logged)
- Multi-gym with active switch
- **Material You** dynamic theme + dark-first
- **AES-256 encrypted photos** via Android Keystore
- **Zero account, zero telemetry, zero cloud**

## Installation

### End user

1. Go to the [Releases page](https://github.com/jeremie0342/trackmyweight/releases/latest)
2. Download the APK `trackmyweight-vX.X.X.apk`
3. Allow unknown sources in your browser's Android settings
4. Open the APK from Downloads and install

Future updates will replace the app without wiping your data (stable signature).

### Developer

Prerequisites: **JDK 17** + **Android SDK** (via Android Studio or command-line tools).

```bash
git clone https://github.com/jeremie0342/trackmyweight.git
cd trackmyweight
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/
```

Install on phone via ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Building without a powerful PC (cloud CI)

The project is configured to build entirely in GitHub Actions.
See [CONTRIBUTING.md](CONTRIBUTING.md#development-setup) for the full setup.

## Tech stack

- **Kotlin** 2.1 + **Jetpack Compose** (BOM 2024.12) + **Material 3**
- **Room** 2.6 (FTS4 for accent-insensitive food search, ~45 entities, tested migrations)
- **Hilt** 2.53 (DI)
- **WorkManager** (reminders and periodic sync)
- **Health Connect** 1.1
- **CameraX** 1.4 (custom capture with Compose overlay)
- **Coil** (image loading)
- **kotlinx-serialization** (JSON backup)
- **MediaCodec + MediaMuxer** (H.264 timelapse encoding)
- **Android Keystore** (photo encryption)

Architecture: lightweight MVI with ViewModels + StateFlow, domain-scoped repositories, use cases for business logic (under `domain/calc`), all offline.

## Roadmap

Short term:
- 🌍 English UI translation
- 📸 Official README screenshots
- 🎨 Custom iconography
- 📊 Widgets for daily protein + next session

Mid term:
- 📥 Import coach programs from PDF/photo (OCR)
- 🔄 Multi-device sync via Turso/PowerSync (opt-in)
- 🏆 Achievements & visual progression
- 📈 Monthly / annual views
- 💬 Local AI coach via small on-device LLM (Gemma 2B)

What will **never** be added:
- Mandatory account / cloud
- Advertising, telemetry, analytics
- In-app purchases
- Social features

## Contributing

Contributions welcome, especially:

- 🥘 Local food additions (Benin, Senegal, Ivory Coast, Togo, others)
- 🏋️ New exercises or variants
- 🐛 Detailed bug reports
- 🌍 Translations

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full process. Please respect the [Code of Conduct](CODE_OF_CONDUCT.md).

To report a security vulnerability, see [SECURITY.md](SECURITY.md).

## Acknowledgements

This project stands on the shoulders of giants:

- **Google Jetpack** (Compose, Room, Hilt, Health Connect, CameraX, WorkManager, Glance)
- **JetBrains** (Kotlin, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization)
- **Coil** for image loading
- **FAO West Africa food composition tables** for local nutritional values
- **Renaissance Periodization / Dr. Mike Israetel** for MEV/MAV/MRV landmarks
- **Contributor Covenant** for the Code of Conduct

Special thanks to anyone who tests, reports bugs, suggests local foods, or contributes a line of code.

## License

Distributed under the [MIT license](LICENSE) — free to use, modify, and redistribute, including commercially. No warranty.

---

**TrackMyWeight** — built to be used, not to be a business.
