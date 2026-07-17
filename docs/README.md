# Documentation & assets

Ce dossier contient les captures d'écran, illustrations et documentation additionnelle du projet.

## Contribuer une capture d'écran

Les 6 captures manquantes du README principal sont :

| Nom fichier attendu | Écran à capturer | Ce qu'il faut montrer |
|---|---|---|
| `screenshots/01-today.png` | Onglet **Aujourd'hui** | Carte readiness remplie + macros du jour + habitudes cochées |
| `screenshots/02-workout.png` | Onglet **Séance** | Templates listés + PRs récents + historique |
| `screenshots/03-nutrition.png` | Onglet **Nutrition** | Progression macros + un repas rempli avec 2-3 aliments |
| `screenshots/04-body.png` | Onglet **Corps** | Hub avec 3 cartes Poids/Mensurations/Photos |
| `screenshots/05-report.png` | Écran **Rapport hebdo** | Adhérence + narrative + coach advice visible |
| `screenshots/06-widget.png` | Écran d'accueil Android | Widget avec dernière pesée + bouton "+ Ajouter" |

### Spécifications techniques

- Format : PNG (préférable) ou JPEG à 90% qualité
- Ratio : **portrait 9:19** ou natif de ton téléphone
- Largeur cible : **1080 px** (redimensionner si supérieur)
- Poids max : **500 Ko** par image
- **Aucune info personnelle** visible (poids réel OK si tu assumes, mais pas de nom/email)

### Comment capturer

**Depuis le téléphone** : bouton volume bas + power → recadrer si besoin → transférer par câble/cloud.

**Depuis un PC via ADB** :

```bash
adb shell screencap -p /sdcard/tmw.png
adb pull /sdcard/tmw.png
adb shell rm /sdcard/tmw.png
```

### Soumettre

1. Fork le repo
2. Ajoute tes captures dans `docs/screenshots/`
3. Ouvre une PR avec la description "docs: add screenshot for [écran]"

## Autres assets

- `logo/` — variantes du logo (à venir)
- `press-kit/` — kit presse pour blogs/reviews (à venir)
