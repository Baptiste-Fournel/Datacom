# Datacom

![ci](https://github.com/Baptiste-Fournel/Datacom/actions/workflows/ci.yml/badge.svg)

Refonte sécurisée de l'application DataCom : un **opérateur de saisie** enregistre des fiches produits et les remplit en quatre étapes, puis les soumet à un **responsable conformité** qui les valide avant mise sur le marché. Réécriture *from scratch* d'un monolithe legacy (servlets + JSP + JDBC concaténé) en API REST propre, testée et sécurisée.

Backend : **Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, Maven, Docker**. Front (dépôt séparé) : SvelteKit + Tailwind, consommant le contrat [`docs/openapi.yaml`](docs/openapi.yaml).

## Démarrer

Prérequis : JDK 21, Docker.

```bash
docker compose up --build
```

L'API écoute sur http://localhost:8080 ; le schéma et les comptes de démonstration sont créés par Flyway.

### Avec le front

Le front vit dans un dépôt séparé, à placer à côté de celui-ci. Deux modes de fonctionnement, selon la façon de lancer le front.

**Développement (recommandé).** Le front sert l'API via son proxy Vite : le navigateur reste sur une seule origine, donc les cookies de session et le jeton CSRF passent sans aucune configuration CORS.

```bash
docker compose up --build        # ici : API + PostgreSQL sur :8080
npm install && npm run dev       # dans datacom-front : http://localhost:5173
```

**Conteneur de production.** Le front est alors servi par Node sur le port `3000` et appelle l'API directement : il faut lui donner l'URL publique de l'API, et l'API doit autoriser son origine.

```bash
PUBLIC_API_BASE_URL=http://localhost:8080 docker compose up --build   # côté front
```

L'API n'autorise que les origines déclarées par `FRONT_ORIGINS` (liste séparée par des virgules, par défaut `http://localhost:5173,http://localhost:3000`) et accepte les requêtes authentifiées (`allowCredentials`). Pour une autre origine :

```bash
FRONT_ORIGINS=https://datacom.example.com docker compose up --build
```

Comptes de **démonstration** (seed Flyway `V2`, **usage développement/démo uniquement** — à remplacer par des comptes réels et des secrets forts pour un déploiement) : `operator` / `operator` (opérateur de saisie) et `validator` / `validator` (responsable conformité).

## Développer

```bash
./scripts/install-hooks.sh          # active les hooks git (style + règles)
docker compose up -d db             # PostgreSQL local
./mvnw spring-boot:run
```

Vérification complète (exactement ce que joue la CI) :

```bash
./mvnw verify
```

Elle exécute, et bloque en cas d'échec : le style (Checkstyle), la politique de commentaires (aucun en production, seuls AAA/GWT en test), les règles d'architecture (ArchUnit), les tests unitaires et d'intégration (Testcontainers), et le seuil de couverture (JaCoCo : 80 % global, 90 % `domain`+`application`). Rapport de couverture dans `target/site/jacoco/index.html`.

## Architecture

Architecture en couches dirigée vers le domaine, vérifiée par `ArchitectureRulesTest` :

```
web  →  application  →  domain  ←  infrastructure
```

Le `domain` (agrégat `Product`, machine à états `WorkflowStep`, `User`/`Role`) ne dépend d'aucun framework et porte toutes les règles métier. Les interfaces de repository sont déclarées dans le domaine et implémentées par l'infrastructure (inversion de dépendance). L'`application` orchestre les cas d'usage sous transaction ; le `web` expose l'API REST et mappe des DTO. Décisions détaillées : [`docs/adr/`](docs/adr).

## Sécurité

Authentification par session (cookie `SESSION` HttpOnly), mots de passe **BCrypt**, régénération de l'ID de session au login (anti-fixation). **CSRF** en cookie `XSRF-TOKEN` / header `X-XSRF-TOKEN` (double-submit adapté aux SPA). Autorisation par rôle au niveau des routes (`/api/products/**` opérateur, `/api/validation/**` validateur). Côté validation, cette autorisation est renforcée par une **garde IDOR** par ressource — une fiche qui n'est pas en attente de validation est refusée (403), l'URL directe ne suffit pas — puis re-vérifiée dans le domaine (défense en profondeur). Erreurs uniformisées en `application/problem+json` (RFC 7807) avec des codes stables ; aucun détail interne exposé sur les 500.

## Documentation

- [`docs/analyse-existant-et-refonte.md`](docs/analyse-existant-et-refonte.md) — analyse du legacy et stratégie de refonte
- [`docs/dossier-technique.md`](docs/dossier-technique.md) — dossier de synthèse (choix, sécurité, tests, résultats)
- [`docs/adr/`](docs/adr) — décisions d'architecture (espaces par rôle, pivot API REST)
- [`docs/openapi.yaml`](docs/openapi.yaml) — contrat de l'API (source de vérité partagée avec le front)
- [`AGENTS.md`](AGENTS.md) — règles de développement et boucle de travail

## Livraison continue

Chaque merge sur `main` republie l'image `ghcr.io/baptiste-fournel/datacom` (workflow `cd`) ; chaque tag `v*` crée la release GitHub correspondante (workflow `release`). La roadmap vit dans les issues GitHub, groupées en milestones par phase (six phases, de `v0.1` au domaine jusqu'à `v1.0`).

Le projet a été construit en six phases livrées et taguées incrémentalement : `v0.1.0` domaine métier, `v0.2.0` persistance, `v0.3.0` sécurité, `v0.4.0` API REST complète, `v1.0.0` durcissement et livraison. Chaque phase est un ensemble cohérent, testé et déployable.
