# DataCom — Dossier technique

*Synthèse pour la soutenance. Réécriture sécurisée d'une application monolithique legacy. Renvoie aux documents détaillés du dépôt.*

## 1. Contexte

DataCom gère l'enregistrement de fiches produits et leur validation de conformité avant mise sur le marché. Deux rôles : l'opérateur de saisie remplit une fiche en quatre étapes ; le responsable conformité la valide. L'existant est un monolithe Java servlets + JSP + JDBC dont la dette rend l'évolution risquée. Le mandat : reprendre le logiciel, l'analyser, le réécrire proprement, le sécuriser, le tester (TDD) et le documenter.

## 2. Ce que le legacy faisait de dangereux

Analyse complète dans [`analyse-existant-et-refonte.md`](analyse-existant-et-refonte.md). Les points saillants, tous vérifiés dans le code d'origine :

- **Injection SQL généralisée** : toutes les requêtes concaténées, aucune requête paramétrée. Le login était contournable par `' OR '1'='1' -- `.
- **Mots de passe en clair** : stockés, comparés et remis en session sans hachage.
- **Aucune autorisation** : le rôle était chargé mais jamais utilisé ; n'importe qui pouvait valider.
- **Aucune séparation des responsabilités** : un servlet mêlait HTTP, métier et SQL ; la vue itérait un `ResultSet` JDBC.
- **Aucun test, aucun dépôt Git, secrets en dur, workflow d'états géré à la main.**

## 3. Correctifs — table de correspondance

| Faille legacy | Correctif dans la refonte |
|---|---|
| Injection SQL | JPA / requêtes paramétrées par construction — plus aucune concaténation |
| Mots de passe en clair | BCrypt ; le hash ne transite jamais en session ni en DTO |
| Pas d'autorisation | Rôles au niveau des routes + garde IDOR par ressource + vérification dans le domaine |
| Tout dans le servlet | Couches `web` / `application` / `domain` / `infrastructure`, vérifiées par ArchUnit |
| Secrets en dur | Externalisés (variables d'environnement, `application.yml`) |
| Workflow fragile | Machine à états explicite dans le domaine (transitions gardées) |
| Aucun test | TDD strict, couverture bloquante 80 % / 90 % domaine |
| Pas de Git | Git Flow allégé, CI/CD, revue à chaque ticket |

## 4. Choix technologiques

Java 21 + Spring Boot 3.5, PostgreSQL 16 + Flyway, Maven, Docker. Le langage était libre : Java a été retenu objectivement parce que la sécurité — axe le plus évalué — est traitée par Spring Security avec des composants éprouvés (auth, rôles, BCrypt, CSRF, anti-fixation), là où un assemblage manuel multiplierait la surface d'erreur. Le pivot vers une **API REST** (au lieu du rendu serveur initialement envisagé) est acté dans [`adr/ADR-002`](adr/ADR-002-api-rest-front-sveltekit.md) : un développeur front dédié (SvelteKit) a rejoint le projet, le contrat [`openapi.yaml`](openapi.yaml) permet aux deux de travailler en parallèle.

## 5. Architecture

Architecture en couches **dirigée vers le domaine** (une clean architecture allégée, pas d'hexagonal complet — proportionnée au périmètre, cf. [`adr/ADR-001`](adr/ADR-001-espaces-par-role.md)). Règle de dépendance `web → application → domain ← infrastructure`, exécutable via `ArchitectureRulesTest` (le domaine ne dépend d'aucun framework ; pas de cycles). Le domaine porte la machine à états du workflow et les invariants ; les repositories sont des interfaces du domaine implémentées par l'infrastructure (DIP). Patterns retenus, sans excès : injection de dépendances, Repository, DTO + mapper, machine à états ; CQRS/event sourcing/microservices explicitement écartés (YAGNI).

## 6. Sécurité — défense en profondeur

Trois niveaux qui ne font pas doublon, chacun testé :

1. **URL** : `/api/products/**` réservé à l'opérateur, `/api/validation/**` au validateur (403 croisés).
2. **Ressource (garde IDOR)** : le détail et la validation d'une fiche refusent (403 neutre) toute fiche qui n'est pas en attente de validation — une liste filtrée ne suffit pas, l'URL directe doit être gardée.
3. **Domaine** : `Product.validate()` re-vérifie le rôle même appelé depuis du code interne ; une dérive session/base est interceptée et rendue en 403 propre, jamais en 500.

Authentification par session, BCrypt, CSRF double-submit, régénération de session au login. Erreurs en `problem+json` (RFC 7807) à codes stables, sans fuite de détail interne.

## 7. Qualité & TDD

TDD strict : aucun code de production sans test rouge d'abord. Règles **exécutables** (donc non contournables) : politique de commentaires (aucun en production ; seuls Arrange/Act/Assert et Given/When/Then en test, vérifié par analyse d'AST), architecture (ArchUnit), style et lisibilité (Checkstyle : méthodes ≤ 30 lignes, complexité ≤ 10), couverture bloquante (JaCoCo 80 % global / 90 % domaine+application). Tests d'intégration sur PostgreSQL réel et jetable (Testcontainers). Chaque ticket a fait l'objet d'une **revue adversariale** par un relecteur au contexte vierge, dont les objections (mutants survivants, mappings dangereux, YAGNI) ont été traitées avant fusion.

## 8. Processus & livraison

Git Flow allégé : `main` (releases) / `develop` (intégration) / `feature/*` (un ticket, squash-merge en un commit explicite). CI GitHub Actions bloquante sur chaque PR (rejoue `verify` intégralement), CD publiant l'image sur `main`. Roadmap en 21 tickets répartis en six phases, livrées et taguées incrémentalement (`v0.1` domaine → `v0.4` API complète → `v1.0`). Historique volontairement compact et lisible.

## 9. Périmètre & limites assumées

Refonte **iso-fonctionnelle** (mêmes fonctionnalités que le legacy) plus la sécurisation et la correction des règles métier cassées. Choix de périmètre documentés : pas d'action de refus d'une fiche (la boucle s'arrête à valider ; échappatoire additive notée dans l'ADR-001), pas de validation de contenu des champs à la soumission (iso-legacy), pas de verrou optimiste sur les soumissions concurrentes (comportement bénin). Ces limites sont conscientes et défendables, pas des oublis.
