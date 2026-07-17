# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Coming next
- Widgets for daily protein and next scheduled session
- Coach program import from PDF/photo
- English UI translation

---

## [1.0.0-beta] — 2026-07-17

First public release. The application covers the full initial scope of a
complete fitness/nutrition assistant, local-first, optimized for Benin and
West African cuisine.

### Added

**Onboarding & profile**
- 7-step flow (identity, goal, activity level, gym, coach mode, recap)
- Auto computation of BMR (Mifflin-St Jeor), TDEE, macro targets by phase
- Gym equipment audit with 44 pre-filled items

**Body tracking**
- Weight screen: quick entry, 7-day moving average, linear projection, stagnation detection, BMI
- Measurements: 14 fields + auto Navy body fat % + WHtR
- Progress photos: CameraX capture with silhouette overlay, comparison slider, MP4 timelapse
- Home screen widget with "+ Add weigh-in" 1-tap button

**Workout journal**
- Library of ~60 exercises with required equipment
- Editable session templates with sets/reps targets
- Active session with auto-fill, adaptive rest timer, automatic PR detection
- French voice input "12 reps à 80 kilos" via RecognizerIntent
- Weekly volume per muscle group vs MEV/MAV/MRV
- Cardio with MET-based kcal computation (10 activities)
- Session text export for sending to coach

**Nutrition**
- Benin food database (~60 items: pâte, foutou, attiéké, wagashi, sauces, etc.)
- 3 stackable logging modes: visual portions (fist/palm/thumb), precise grams, favorites
- Qualitative protein distribution advice
- Cost per protein gram in FCFA

**Habits & recovery**
- "Today" dashboard: readiness, weight, macros, sleep, water, habits, resting pulse
- 7 default habits with streaks (recursive SQL CTE)
- Sleep, steps, water, alcohol tracked separately
- Readiness score across 4 dimensions (sleep/energy/soreness/mood)

**Analytics & coaching**
- Weekly report: 6-signal weighted adherence, auto narrative, non-linear projection, ETA
- Coach Advisor with 10 prioritized rules (refeed, deload, stagnation, protein, sleep)
- One-tap application of 7-day refeed

**System**
- Multi-gym with active switch
- Health Connect: auto read of weight/steps/sleep (12h sync)
- Notifications: morning weigh-in, monthly measurements, session not logged (8:30 PM), hydration (2 PM / 6 PM)
- Full JSON backup (export/import via System File Picker)
- Dynamic Material You theme + dark-first
- Bottom navigation with 5 tabs (Today / Workout / Nutrition / Body / More)

**Data layer**
- Room 2.6 with 45 entities
- FTS4 for accent-insensitive food search
- SQLCipher-ready (activatable via Room config)
- AES-256 encrypted photos via Android Keystore

**Tests & CI**
- ~137 JVM tests (pure business logic)
- Room + Compose instrumented tests (emulator in CI as `continue-on-error`)
- Signed APK build automatic on every push to main
- Automatic distribution via GitHub Releases

### Technical details
- **minSdk** 26 (Android 8.0)
- **targetSdk** 35 (Android 15)
- **Kotlin** 2.1.0, **Compose** BOM 2024.12
- **Hilt** DI, **KSP** annotation processing
- **Coil** image loading, **CameraX** 1.4
- **Health Connect** 1.1
