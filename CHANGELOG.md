# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- **Session preparation screen.** Tapping a template now opens a setup screen —
  pick the gym, reorder exercises, adjust sets/reps/load/rest — and the session
  is only created when you press "Lancer". Previously a tap started the session
  immediately with no way to configure it.
- **Resume an in-progress session** from a banner on the Séance tab. Leaving the
  session screen no longer strands it: an exit dialog offers keep-running,
  finish, or discard.
- **Reliable rest timer**: deadline-based (no drift), survives leaving the screen,
  and rings through a system alarm with sound and vibration even when the app is
  backgrounded or the screen is off. Adjustable by ±30 s, startable manually,
  and honours the per-exercise rest set during preparation.
- **Session chronometer** plus live set count and volume.
- **Template targets shown during the session** (e.g. "3 séries × 8-10 @ 60 kg")
  with a per-exercise completion counter. They are snapshotted at launch, so
  editing a template later does not rewrite past sessions.
- **Correcting or deleting a logged set**, with automatic renumbering and volume
  recomputation.
- Exercise catalogue expanded from 66 to **200 exercises**, each with a French
  coaching cue and a two-frame animated demonstration (start → end position).
- **Exercise detail sheet**: animated demonstration, primary/secondary muscles,
  movement type, French execution cue, required equipment with photos, and last
  performance. Reachable from every exercise picker and from the session screen.
- **Thumbnails in every exercise picker** — the lists were name-only, which made
  picking an unfamiliar movement guesswork.
- **Gym equipment photos** (38 of 44 items) when auditing a gym's equipment:
  ticking "hack squat" without knowing what it looks like was pointless.
- **Max-load tracking**: a reference 1RM per exercise, either tested, declared,
  or estimated from a real set. The estimated value is recorded automatically
  whenever a 1RM personal record falls, so the curve builds itself with no
  bookkeeping. Full history, never overwritten.
- **Working-load table** derived from the reference max (90 % down to 60 %),
  with expected reps and, for barbell movements, the plate loading per side.
- **Monthly tonnage**, overall and per exercise, with month-over-month variation.
- **Progression screen** (Séance → Progression): volume lifted per month, and
  exercises split between those progressing and those stalling over 90 days.
- **Custom exercises.** Searching the picker for something the catalogue does
  not have now offers to create it on the spot, and adds it straight to the
  session. Marked `isCustom`, so catalogue syncs never touch it.
- **Gym filtering in the exercise pickers**, driven by the equipment actually
  registered for the gym, with a toggle back to the full catalogue. The filter
  is skipped entirely when a gym has no equipment recorded, rather than
  presenting an empty list.
- **All six set types** are now selectable — working, warm-up, drop, AMRAP,
  back-off and failure. Only working and warm-up were reachable before. The type
  resets to "working" after each logged set so an untouched toggle cannot
  silently distort volume.
- **Weekly volume per muscle group vs MEV/MAV/MRV**, with a per-muscle verdict
  and a set-count suggestion, on the Progression screen.
- **Generated warm-up sets**: from the target load, the ramp-up series are
  previewed and logged in one tap, typed as warm-up.
- **Supersets.** Consecutive exercises can be chained in the template editor or
  at preparation; during the session they are labelled and, crucially, the rest
  timer only fires after the last exercise of the round instead of between A1
  and A2. Groups holding a single exercise are dissolved automatically, and
  reordering or deleting renumbers them.
- **Resume banner on the Today tab**, not just on Séance — the home screen is
  the first thing opened, and it is where you notice a session left running
  overnight.
- **Reordering exercises during a live session** — previously only possible at
  preparation, which is exactly the wrong time: adapting happens in the gym when
  a machine is taken. Moving an exercise out of a superset dissolves the group
  rather than silently dropping the rest between two exercises that no longer
  follow each other.
- **Required equipment when creating a custom exercise**, so custom movements
  are subject to gym filtering like the rest.
- **Template rotations**: alternate several templates on the same weekday, with
  the next one suggested on the Séance tab. The resolver (a recursive SQL query)
  had existed since the beginning with no screen able to create a group, making
  the advertised feature unreachable.
- **Eight-week weekly volume trend** on the Progression screen, read back from
  the persisted aggregate.
- Explicit Room migrations, 5 → 6, 6 → 7 and 7 → 8.

### Changed
- Material You is disabled: the Ink palette was being overridden on every
  Android 12+ device, making the design system invisible. The light colour
  scheme, previously only three tokens deep, is now fully defined.
- History lists only finished sessions; in-progress ones no longer appear with
  0 kg of volume.
- Session volume is recomputed on every set change instead of only at the end.
- Muscle groups and equipment categories are displayed in French instead of raw
  enum constants (`SHOULDERS_FRONT` → "Épaules avant", `BAR` → "Barres et
  disques"), and dates instead of raw ISO instants.

### Fixed
- **Destructive database fallback.** Any schema bump silently wiped all user
  data. Destructive fallback is now restricted to pre-v5 databases; from v5
  onwards a missing or broken migration fails loudly instead.
- Warm-up sets no longer count towards personal records.
- The "max reps at a given weight" PR could never trigger — its reference value
  was always passed as null.
- The PR badge in the session screen never appeared, as `isPrCandidate` was
  hardcoded to false.
- Four engines were written and unit-tested but had no caller at all:
  `PlateCalculator` now drives the plate breakdown in the working-load table,
  `WarmupCalculator` generates the ramp-up sets, `VolumeBucketing` feeds the
  weekly MEV/MAV/MRV verdicts, and `muscle_group_volume_weekly` — a table that
  was never written to — is finally populated.
- `observeAvailableInGym` existed down to the DAO with no UI caller: the
  equipment-based filtering advertised in the README did not exist on screen.
- The exercise pickers silently truncated the list to 60 entries, and rebuilt
  every row eagerly; they are now complete and lazily rendered.

### Coming next
- Nutrition: cost per gram of protein in FCFA (`CostPerProtein` has no caller)
- Nutrition: automatic calorie adaptation (`CalorieAdapter` has no caller)
- Analytics: habit/weight correlations (`PearsonCorrelation` has no caller)
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
