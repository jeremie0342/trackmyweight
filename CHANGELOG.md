# Changelog

Toutes les modifications notables de ce projet sont documentées dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
et le projet adhère à [Semantic Versioning](https://semver.org/lang/fr/).

## [Non publié]

### À venir
- Widgets protéines du jour et prochaine séance
- Import automatique de coach programs depuis PDF/photo
- Traduction anglaise

---

## [1.0.0-beta] — 2026-07-17

Première release publique. L'application couvre l'intégralité du scope initial d'un
assistant fitness/nutrition complet, en local-first, orienté cuisine béninoise et
Afrique de l'Ouest.

### Ajouté

**Onboarding & profil**
- Flow 7 étapes (identité, objectif, activité, salle, mode coach, récap)
- Calcul auto BMR (Mifflin-St Jeor), TDEE, macros cibles selon phase
- Audit équipement de salle avec 44 items pré-remplis

**Suivi corporel**
- Écran Poids : saisie rapide, moyenne mobile 7j, projection linéaire, détection stagnation, IMC
- Mensurations : 14 mesures + calcul auto Navy body fat + WHtR
- Photos progression : capture CameraX avec silhouette overlay, comparaison slider, timelapse MP4
- Widget écran d'accueil avec bouton "+ Ajouter une pesée" 1-tap

**Journal d'entraînement**
- Bibliothèque de ~60 exercices avec équipements requis
- Templates de séance éditables avec sets/reps cibles
- Séance active avec auto-fill, chrono repos adaptatif, détection PR automatique
- Voice input FR "12 reps à 80 kilos" via RecognizerIntent
- Volume hebdo par groupe musculaire vs MEV/MAV/MRV
- Cardio avec calcul kcal via table MET (10 activités)
- Export texte des séances pour envoi au coach

**Nutrition**
- Base alimentaire béninoise (~60 aliments : pâte, foutou, attiéké, wagashi, sauces, etc.)
- 3 modes de log par repas : portions visuelles (poing/paume/pouce), grammes précis, favoris
- Distribution protéines avec conseils qualitatifs
- Cost per protein en FCFA

**Habitudes & récupération**
- Dashboard "Aujourd'hui" : readiness, poids, macros, sommeil, eau, habitudes, pouls
- 7 habitudes par défaut avec streaks (CTE récursive SQL)
- Sommeil, pas, eau, alcool séparés
- Readiness score 4 dimensions (sommeil/énergie/courbatures/humeur)

**Analytique & coaching**
- Rapport hebdo : adhérence 6 signaux, narrative auto, projection non-linéaire, ETA
- Coach Advisor avec 10 règles priorisées (refeed, deload, stagnation, protéines, sommeil)
- Application automatique du refeed 7 jours

**Système**
- Multi-salles avec switch actif
- Health Connect : lecture auto poids/pas/sommeil (sync 12h)
- Notifications : pesée matinale, mensurations mensuelles, séance non loguée (20h30), hydratation (14h/18h)
- Backup JSON complet (export/import via System File Picker)
- Thème Material You dynamique + dark-first
- Navigation bottom 5 tabs (Aujourd'hui / Séance / Nutrition / Corps / Plus)

**Data layer**
- Room 2.6 avec 45 entités
- FTS4 pour recherche aliments insensible aux accents
- SQLCipher-ready (à activer via config Room)
- Photos chiffrées AES-256 via Android Keystore

**Tests & CI**
- ~137 tests JVM (business logic pure)
- Tests instrumentés Room + Compose (émulateur en CI en `continue-on-error`)
- Build APK signé automatique à chaque push sur main
- Distribution automatique via GitHub Releases

### Détails techniques
- **minSdk** 26 (Android 8.0)
- **targetSdk** 35 (Android 15)
- **Kotlin** 2.1.0, **Compose** BOM 2024.12
- **Hilt** DI, **KSP** annotation processing
- **Vico** (unused), **Coil** image loading, **CameraX** 1.4
- **Health Connect** 1.1
