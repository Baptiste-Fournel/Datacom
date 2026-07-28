# Changelog

Toutes les évolutions notables de DataCom. Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) ; versionnage [SemVer](https://semver.org/lang/fr/). Le projet a été construit en six phases livrées et taguées incrémentalement.

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
