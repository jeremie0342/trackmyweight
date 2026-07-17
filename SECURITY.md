# Security Policy

## Versions supportées

Seule la dernière release majeure reçoit des correctifs de sécurité. Le projet
est en développement actif, met à jour vers la dernière version.

| Version | Supportée |
|---------|-----------|
| 1.x     | ✅        |
| < 1.0   | ❌        |

## Signaler une vulnérabilité

**Ne PAS ouvrir d'issue publique pour un problème de sécurité.**

Envoie un rapport privé via un des canaux suivants :

1. **Preferred** : [GitHub Security Advisory](https://github.com/jeremie0342/trackmyweight/security/advisories/new)
2. **Alternative** : DM au mainteneur principal sur GitHub (@jeremie0342)

Inclus :
- Description du problème et impact estimé
- Version affectée
- Étapes de reproduction
- Suggestion de correctif si tu en as une

## Ce que tu peux attendre

- **Accusé de réception** sous 72h
- **Évaluation initiale** sous 7 jours (impact, sévérité, plan de correction)
- **Correctif** dans un délai raisonnable selon la sévérité :
  - Critique : release patch sous 7 jours
  - Élevée : sous 30 jours
  - Moyenne : dans la prochaine release mineure
  - Basse : dans la prochaine release majeure
- **Crédit** dans le CHANGELOG et l'advisory publié après le fix (sauf si tu préfères rester anonyme)

## Périmètre

L'application est **local-first** : toutes tes données restent sur ton téléphone.
Les seuls composants réseau sont :

- Health Connect (lecture uniquement, données Android sandbox)
- Aucune API externe, aucune télémétrie, aucun analytics

Périmètre de sécurité :

- ✅ Chiffrement AES-256 des photos progression (Android Keystore)
- ✅ Backup JSON exportable localement uniquement (pas d'upload auto cloud)
- ✅ Aucune donnée envoyée à un serveur tiers
- ✅ Permissions minimales (caméra opt-in, notifications opt-in, Health Connect opt-in)

## Hors périmètre

- Vulnérabilités dans les dépendances tierces (Room, Compose, Hilt) — reporter directement à Google/JetBrains
- Attaques nécessitant un accès physique déverrouillé au téléphone
- Vulnérabilités OS Android en dehors de la sandbox de l'app
