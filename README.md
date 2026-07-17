# TrackMyWeight

**Assistant fitness & nutrition Android — local-first, orienté Bénin / Afrique de l'Ouest.**

Une app tout-en-un pour piloter une transformation physique : poids, mensurations, photos, journal d'entraînement, nutrition adaptée aux aliments locaux, habitudes, récupération, rapport hebdomadaire avec coaching algorithmique.

Aucune donnée n'est envoyée sur un serveur. Tout reste sur ton téléphone, avec import optionnel depuis Health Connect et backup manuel JSON vers ton cloud personnel.

[![Build & Release](https://github.com/jeremie0342/trackmyweight/actions/workflows/android-build.yml/badge.svg)](https://github.com/jeremie0342/trackmyweight/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/minSdk-26-green.svg)](https://developer.android.com/about/versions/oreo)
[![Release](https://img.shields.io/github/v/release/jeremie0342/trackmyweight?include_prereleases)](https://github.com/jeremie0342/trackmyweight/releases/latest)

---

## Pourquoi cette app

La plupart des trackers fitness assument un utilisateur occidental avec balance connectée, montre haut de gamme, et cuisine standardisée. Cette app est pensée pour :

- 📱 Un téléphone Android sans accessoires obligatoires
- 🥘 Une base d'aliments locaux (pâte, foutou, attiéké, wagashi, sauces arachide/gombo/feuille, poulet bicyclette, tilapia, kluiklui, bissap, etc.)
- 💰 Un budget contraint (calcul auto du coût par gramme de protéine en FCFA)
- 🏋️ Un usage salle de sport quotidien avec matériel variable
- 👨‍🏫 La cohabitation avec un coach humain (mode passif qui logue sans contredire)
- ⚡ Un usage pur local-first, sans compte, sans télémétrie, sans pub

## Screenshots

Les captures officielles seront ajoutées dans `docs/screenshots/`. Voir [docs/README.md](docs/README.md) pour contribuer.

| Aujourd'hui | Séance | Nutrition |
|-------------|--------|-----------|
| _à venir_   | _à venir_ | _à venir_ |

| Corps | Rapport hebdo | Widget |
|-------|---------------|--------|
| _à venir_ | _à venir_ | _à venir_ |

## Fonctionnalités

### Suivi corporel
- Pesée quotidienne avec **graphique + moyenne mobile 7j + projection linéaire**
- Widget écran d'accueil avec bouton **+ Ajouter une pesée** 1-tap
- Mensurations 14 mesures + calcul auto **% masse grasse méthode Navy** (pas d'impédancemètre requis)
- Photos de progression avec **overlay silhouette** de la dernière photo pour aligner
- **Timelapse MP4** généré à la demande, partageable
- Ratios santé WHtR / WHR
- Détection auto de **stagnation** (14 jours sans progression)

### Journal d'entraînement
- Bibliothèque de **~60 exercices** avec muscles primaires/secondaires + équipement requis
- Filtrage auto selon **l'équipement de ta salle** (multi-salles supportées)
- Templates de séance avec **rotation** (ex : lundi bras/jambes alterné)
- Séance active : **auto-fill** de la dernière perf, chrono repos adaptatif (compound 3min / isolation 90s)
- **Détection automatique de PR** (max weight, 1RM estimé Epley/Brzycki, max reps à un poids donné)
- **Volume hebdo par groupe musculaire** vs landmarks MEV/MAV/MRV (Renaissance Periodization)
- **Voice input FR** : "12 reps à 80 kilos" → champs auto-remplis
- Cardio : 9 activités avec **calcul kcal via table MET** modulé par RPE
- **Export texte** pour envoi au coach

### Nutrition
- Base alimentaire **béninoise** (~60 aliments) + **internationale**
- **3 modes de log par repas cumulables** :
  - Portions visuelles (poing, paume, pouce, louche, cuillère)
  - Grammes précis (pour les curieux)
  - Repas favoris 1-tap
- Compteur **protéines + calories quotidien** vs cible personnalisée
- **Distribution protéines** dans la journée avec conseils qualitatifs
- Suivi fibres, sodium, alcool séparé
- **Prix par gramme de protéine en FCFA** — classe automatiquement tes aliments par ratio économique
- Phases de régime : **cut / recomp / bulk / maintenance / refeed / diet break**
- **Adaptation calorique automatique** selon la tendance réelle du poids

### Habitudes & récupération
- Dashboard "Aujourd'hui" avec cartes readiness, poids, macros, sommeil, eau, habitudes, pouls
- **7 habitudes par défaut** (pesée matinale, créatine, 10k pas, 2L eau, sommeil ≥7h, étirements, sans alcool)
- **Streaks** en jours consécutifs (SQL récursif)
- **Check-in readiness matinal** 15s (sommeil/énergie/courbatures/humeur → score 0-5 avec conseil)
- Compteur d'eau avec presets 250/500/750/1000 ml

### Analytique & coaching
- **Rapport hebdomadaire** : adhérence pondérée sur 6 signaux, narrative auto, projection non-linéaire vers l'objectif avec ETA
- **Coach Advisor** avec 10 règles priorisées :
  - Semaine de refeed auto-suggérée après 8 semaines de cut
  - Deload si readiness moyen < 2.5/5
  - Ajustement calories si stagnation > 14j ou perte > 0.9kg/sem
  - Volume au-dessus du MRV par muscle
  - Sommeil insuffisant
- **Application automatique** du refeed 7 jours d'un tap

### Système
- **Health Connect** : lecture auto poids / pas / sommeil toutes les 12h (opt-in)
- **Backup / restore complet en JSON** — export vers Drive/iCloud/USB
- **Notifications intelligentes** contextuelles (pesée matin, hydratation, séance non loguée)
- Multi-salles avec switch actif
- Thème **Material You** dynamique + dark-first
- **Photos chiffrées AES-256** via Android Keystore
- **Zéro compte, zéro télémétrie, zéro cloud**

## Installation

### Utilisateur final

1. Va sur la [page Releases](https://github.com/jeremie0342/trackmyweight/releases/latest)
2. Télécharge l'APK `trackmyweight-vX.X.X.apk`
3. Autorise les sources inconnues dans les paramètres Chrome/Firefox de ton téléphone
4. Ouvre l'APK depuis les téléchargements et installe

Les mises à jour futures remplaceront l'app sans effacer tes données (signature stable).

### Développeur

Prérequis : **JDK 17** + **Android SDK** (via Android Studio ou command-line tools).

```bash
git clone https://github.com/jeremie0342/trackmyweight.git
cd trackmyweight
./gradlew assembleDebug
# APK dans app/build/outputs/apk/debug/
```

Installation sur téléphone via ADB :

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Build sans PC puissant (via CI cloud)

Le projet est configuré pour compiler entièrement dans GitHub Actions.
Détails du setup dans [CONTRIBUTING.md](CONTRIBUTING.md#setup-développement).

## Stack technique

- **Kotlin** 2.1 + **Jetpack Compose** (BOM 2024.12) + **Material 3**
- **Room** 2.6 (FTS4 pour recherche aliments, ~45 entités, migrations testées)
- **Hilt** 2.53 (DI)
- **WorkManager** (rappels et sync périodique)
- **Health Connect** 1.1
- **CameraX** 1.4 (capture custom avec overlay Compose)
- **Coil** (image loading)
- **kotlinx-serialization** (backup JSON)
- **MediaCodec + MediaMuxer** (encodage timelapse H.264)
- **Android Keystore** (chiffrement photos)

Architecture : MVI léger avec ViewModels + StateFlow, repositories par domaine, use cases pour la logique métier (dans `domain/calc`), tout offline.

## Roadmap

Priorités court terme :
- 🌍 Traduction anglaise
- 📸 Screenshots officiels dans le README
- 🎨 Iconographie personnalisée
- 📊 Widgets protéines du jour + prochaine séance

Moyen terme :
- 📥 Import de programmes coach depuis PDF/photo (OCR)
- 🔄 Sync multi-device via Turso/PowerSync (opt-in)
- 🏆 Achievements & progression visuelle
- 📈 Vue mensuelle / annuelle
- 💬 Coach IA local via petit LLM on-device (Gemma 2B)

Ce qui ne sera **pas** ajouté :
- Compte utilisateur / cloud obligatoire
- Publicité, télémétrie, analytics
- Achats in-app
- Fonctions sociales

## Contribuer

Les contributions sont les bienvenues, particulièrement :

- 🥘 Ajout d'aliments locaux (Bénin, Sénégal, Côte d'Ivoire, Togo, autres)
- 🏋️ Ajout d'exercices ou variantes
- 🐛 Bug reports détaillés
- 🌍 Traductions

Consulte [CONTRIBUTING.md](CONTRIBUTING.md) pour le processus complet. Assure-toi de respecter le [Code de Conduite](CODE_OF_CONDUCT.md).

Pour reporter une vulnérabilité de sécurité, voir [SECURITY.md](SECURITY.md).

## Remerciements

Ce projet repose sur les épaules de géants :

- **Google Jetpack** (Compose, Room, Hilt, Health Connect, CameraX, WorkManager, Glance)
- **JetBrains** (Kotlin, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization)
- **Coil** pour le chargement d'images
- **Tables FAO West Africa** pour les valeurs nutritionnelles des aliments locaux
- **Renaissance Periodization / Dr. Mike Israetel** pour les landmarks MEV/MAV/MRV
- **Contributor Covenant** pour le Code de Conduite

Un merci particulier à toute personne qui teste, remonte des bugs, propose des aliments locaux, ou contribue une ligne de code.

## Licence

Ce projet est distribué sous licence [MIT](LICENSE) — libre d'usage, de modification et de redistribution, y compris commercial. Aucune garantie.

---

**TrackMyWeight** — bâti pour être utilisé, pas pour être un business.
