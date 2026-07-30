# Datacom

![ci](https://github.com/Baptiste-Fournel/Datacom/actions/workflows/ci.yml/badge.svg)

Refonte sécurisée de l'application DataCom : enregistrement de fiches produits par un opérateur de saisie, workflow en 4 étapes, puis validation de conformité par un responsable dédié avant mise sur le marché.

Stack : Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, Maven, Docker. Développement en TDD, règles d'architecture et de style exécutables (ArchUnit, Checkstyle, politique de commentaires, couverture JaCoCo bloquante). Les règles du projet vivent dans [AGENTS.md](AGENTS.md), l'analyse et les décisions dans [docs/](docs/), la roadmap dans les issues et le board GitHub Projects.

## Démarrer

Prérequis : JDK 21, Docker.

```bash
docker compose up --build
```

L'application écoute sur http://localhost:8080 ; le schéma et les comptes de démarrage sont créés par Flyway.

Comptes de développement (uniquement locaux) : `operator` / `operator` (opérateur de saisie) et `validator` / `validator` (responsable conformité).

## Développer

```bash
./scripts/install-hooks.sh
docker compose up -d db
./mvnw spring-boot:run
```

Vérification complète (celle que la CI exécute) : `./mvnw verify`. Le rapport de couverture est généré dans `target/site/jacoco/index.html`.

## Livraison continue

Chaque merge sur `main` republie l'image `ghcr.io/baptiste-fournel/datacom` (workflow `cd`).
