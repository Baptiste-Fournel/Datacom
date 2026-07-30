# Changelog

Toutes les évolutions notables de DataCom. Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) ; versionnage [SemVer](https://semver.org/lang/fr/). Le projet a été construit en six phases livrées et taguées incrémentalement.

## [1.0.8] — 2026-07-30

### Corrigé
- Une saisie plus longue que la colonne de destination renvoyait une erreur serveur (`500`) au lieu d'une erreur client. Les champs de fiche sont désormais bornés aux longueurs réelles du schéma et l'échec est rendu en `400 VALIDATION_ERROR`, comme les autres erreurs de requête.

### Ajouté
- Cinq tests d'intégration sur les bornes de saisie, dont un qui garantit qu'un brouillon partiellement rempli reste accepté.

## [1.0.7] — 2026-07-30

### Ajouté
- Profil Compose `full` : base, API et interface démarrées par une seule commande (`docker compose --profile full up --build`), le front étant construit depuis le dépôt voisin ou directement depuis son dépôt distant via `FRONT_CONTEXT`.
- Ports surchargeables (`API_PORT`, `FRONT_PORT`, `DB_PORT`), la configuration CORS suivant automatiquement.

### Modifié
- README : démarrage rapide pour un dépôt fraîchement cloné, avec Docker pour seul prérequis.

## [1.0.6] — 2026-07-30

### Ajouté
- Dossier technique complet et illustré (`docs/dossier-technique.md`) : architecture, modèle de données, patterns, flux des cas d'usage, sécurité, tests et collaboration, avec schémas Mermaid rendus par GitHub.

### Modifié
- CORS : les deux origines du front sont autorisées par défaut (`5173` en développement via le proxy Vite, `3000` pour le conteneur de production).
- README : documentation des deux modes de démarrage du front.

## [1.0.5] — 2026-07-30

### Ajouté
- Documentation du démarrage conjoint front + API, et surcharge de l'origine autorisée par `FRONT_ORIGINS`.
- `.dockerignore` : contexte de build limité aux sources nécessaires.

### Modifié
- Origine CORS par défaut alignée sur le port de développement du front (`http://localhost:5173`).
- Dependabot restreint aux correctifs (les montées majeures et mineures sont ignorées), groupés et mensuels.

### Supprimé
- `docs/maquette-ui.html` : la maquette est remplacée par le véritable front SvelteKit.

## [1.0.4] — 2026-07-29

### Ajouté
- Rapport de synchronisation enrichi (branche `ci-status`) : URL du board, nombre d'items et liste des boards « Datacom » pour repérer d'éventuels doublons.

### Modifié
- `project-sync` peut se resynchroniser depuis `develop` (déclenchement restreint au fichier de workflow) ; sélection stricte du board existant, jamais de création.

## [1.0.3] — 2026-07-29

### Modifié
- Roadmap tenue exclusivement dans le board GitHub Projects : `project-sync` cible le board existant (jamais de création) et positionne les tickets de développement sur la chronologie lun. 27 → mer. 29 juillet, l'oral réservé au jeu. 30.

### Supprimé
- Version HTML de la roadmap (`docs/roadmap.html`), redondante avec le board GitHub Projects.

## [1.0.2] — 2026-07-29

### Corrigé
- Fin de la création de boards Projects en double : `project-sync` ne crée plus de board et ne s'exécute qu'à la demande (ou sur modification du workflow).

## [1.0.1] — 2026-07-29

### Ajouté
- Professionnalisation du dépôt : `CHANGELOG`, `CONTRIBUTING`, `SECURITY`, `LICENSE`, `CODEOWNERS`, templates d'issues, Dependabot.
- Roadmap datée (champs Start/Target) dans le board GitHub Projects, avec une chronologie par phase.

## [1.0.0] — 2026-07-29 · Phase 5 — Durcissement & livraison

### Ajouté
- Gestion d'erreurs centralisée `application/problem+json` (RFC 7807) : `400 VALIDATION_ERROR` sur corps malformé, filet `500` neutre sans fuite de détail interne.
- Logs d'audit SLF4J (validation de fiche, échec d'authentification sans donnée sensible).
- Documentation finale : dossier technique, README complet.
- Automatisation de la release GitHub sur tag `v*`.

## [0.4.0] — 2026-07-29 · Phase 4 — API REST complète

### Ajouté
- Parcours opérateur : création de brouillon, édition par étape, avancement, soumission à validation.
- Parcours validateur : file d'attente, détail en lecture seule, validation de conformité.
- **Garde IDOR** par ressource côté validation (défense en profondeur URL → service → domaine).
- Socle API : erreurs `problem+json`, CORS pour le front SvelteKit.

## [0.3.0] — 2026-07-28 · Phase 3 — Sécurité

### Ajouté
- Authentification par session, mots de passe **BCrypt**, régénération de session au login (anti-fixation).
- Protection **CSRF** en cookie double-submit adaptée aux SPA.
- Espaces de routes par rôle (`/api/products` opérateur, `/api/validation` validateur) et `GET /api/auth/me`.

## [0.2.0] — 2026-07-28 · Phase 2 — Persistance

### Ajouté
- Mapping JPA du domaine, repositories dirigés par le domaine (inversion de dépendance).
- Requête intentionnelle de la file du validateur.
- Tests d'intégration sur PostgreSQL réel (Testcontainers).

## [0.1.0] — 2026-07-27 · Phase 1 — Domaine métier

### Ajouté
- Agrégat `Product` et machine à états du workflow en quatre étapes.
- Cycle de vie `DRAFT → PENDING_VALIDATION → VALIDATED`, validation réservée au rôle validateur.
- Modèle `User` / `Role`.

## [0.0.0] — 2026-07-27 · Phase 0 — Socle & outillage

### Ajouté
- Squelette Spring Boot 3.5 (Java 21), schéma Flyway corrigé, comptes BCrypt, Docker Compose.
- Règles exécutables : architecture (ArchUnit), politique de commentaires, style (Checkstyle), couverture (JaCoCo).
- CI/CD GitHub Actions, hooks git, stratégie de branches.

[1.0.1]: https://github.com/Baptiste-Fournel/Datacom/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Baptiste-Fournel/Datacom/compare/v0.4.0...v1.0.0
[0.4.0]: https://github.com/Baptiste-Fournel/Datacom/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/Baptiste-Fournel/Datacom/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Baptiste-Fournel/Datacom/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Baptiste-Fournel/Datacom/releases/tag/v0.1.0
