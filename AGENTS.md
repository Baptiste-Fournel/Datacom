# AGENTS.md — Règles de développement du projet Datacom

Refonte from scratch d'une application legacy (séminaire « Réécriture & sécurisation d'une application monolithique »). Contexte complet dans `docs/analyse-existant-et-refonte.md`, `docs/adr/ADR-001-espaces-par-role.md` et `docs/adr/ADR-002-api-rest-front-sveltekit.md`. Ce dépôt = **backend API REST** ; le front SvelteKit vit dans un dépôt séparé tenu par le développeur front — le contrat partagé est `docs/openapi.yaml` (OpenAPI 3, toute évolution par PR). La phase 4 s'implémente en endpoints REST, la sécurité (phase 3) en login JSON + CSRF cookie. La roadmap vit dans les issues GitHub et le board Projects.

## Règles non négociables

1. **TDD strict.** Aucun code de production sans test rouge écrit d'abord. Cycle red → green → refactor, commits qui racontent ce cycle.
2. **Couverture bloquante.** JaCoCo : 80 % global, 90 % sur `domain` et `application`. Interdiction absolue d'écrire un test dont le seul but est de faire monter la couverture : chaque test vérifie un comportement métier nommé.
3. **Aucun commentaire dans le code de production** (ni Javadoc, ni bloc, ni ligne dans `src/main`) — si le code a besoin d'un commentaire, il a besoin d'un meilleur nom. **Dans les tests, les commentaires de structure sont OBLIGATOIRES** : chaque méthode `@Test` contient au moins un marqueur parmi `// Arrange` / `// Act` / `// Assert` (tests unitaires) ou `// Given` / `// When` / `// Then` (tests d'intégration) — et rien d'autre. Règle exécutable dans les deux sens : `CommentPolicyTest`.
4. **Architecture dirigée vers le domaine.** Couches `domain` / `application` / `infrastructure` / `web`. Le domaine ne dépend de rien (Spring interdit dans le domaine), les repositories sont des interfaces du domaine implémentées par l'infrastructure. Règle exécutable : `ArchitectureRulesTest`.
5. **SOLID obligatoire**, en particulier : une raison de changer par classe (SRP), dépendre d'abstractions définies côté domaine (DIP).
6. **DRY et YAGNI.** Pas d'abstraction avant le deuxième usage réel. Pas de pattern sans problème présent. Pas de champ, paramètre ou indirection « pour plus tard ».
7. **Lisibilité maximale.** Méthodes ≤ 30 lignes, complexité cyclomatique ≤ 10, imbrication ≤ 2 (Checkstyle bloquant). **Tous les identifiants en anglais, sans aucun mélange de langues.** Convention de nommage des tests : `should<ComportementAttendu>_when<Condition>` (ex. `shouldRejectSubmission_whenDraftIsIncomplete`).
8. **Ne rien inventer.** Toute règle métier absente de `docs/` ou des issues est une question pour Baptiste, pas une supposition. On pose la question et on s'arrête sur ce point-là (on continue sur un autre ticket si possible).

## Stratégie de branches (Git Flow allégé)

- **`main`** : stable, ne reçoit que des releases — merges de `develop` (fin de phase) ou `hotfix/*`. Chaque merge sur main est taggé `v0.<phase>` puis `v1.0.0`. La CD publie l'image depuis main.
- **`develop`** : intégration continue des tickets. Toujours verte.
- **`feature/T<numero>-<slug>`** : une branche par ticket, créée depuis `develop`, mergée en **squash** dans `develop` (un seul commit explicite par ticket), supprimée après merge.
- **`fix/*`** (anomalie sur develop) et **`hotfix/*`** (urgence sur main) : mêmes règles.
- La conformité des PR est vérifiée par le job `branch-policy` de la CI : `feature/fix/chore → develop`, `develop|hotfix → main`. Toute autre cible échoue.
- **Clarté de l'historique — non négociable** : sur `develop`, UN commit explicite par ticket (squash-merge), message `type(scope): titre (Txx)` avec un corps qui mentionne le cycle TDD. Sur la branche de travail, commits libres (test/feat/refactor) — ils disparaissent au squash. Aucun commit de plomberie sur develop/main.

## Boucle de travail par ticket

1. Choisir le ticket ouvert le plus prioritaire (ordre des phases, puis ordre des numéros) ; le passer en « in progress ».
2. Créer une branche `feature/T<numero>-<slug>` depuis `develop` à jour.
3. **Red** : écrire les tests qui expriment les critères d'acceptation du ticket ; vérifier qu'ils échouent.
4. **Green** : écrire le minimum de code de production pour les faire passer.
5. **Refactor** : nettoyer sans changer le comportement.
6. **Revue adversariale** : relire le diff en cherchant activement les violations (SOLID, YAGNI, lisibilité, sécurité, cas limites oubliés). Avec un agent : confier cette revue à un sous-agent au contexte vierge et traiter chaque objection.
7. Vérifier : `./mvnw verify` (dans un environnement sans Docker : `./mvnw verify -DskipITs`, la CI exécute les ITs).
8. Ouvrir une PR (template fourni), message et commits en Conventional Commits (`feat:`, `test:`, `refactor:`, `chore:`, `ci:`, `docs:`).
9. Squash-merge dans `develop` uniquement quand la CI est verte (un commit explicite, corps mentionnant le TDD) ; fermer le ticket par tag ops `done <n>` ; supprimer la branche ; passer au suivant. En fin de phase : merge `develop → main` + tag.

Solliciter Baptiste **uniquement** si : ambiguïté métier réelle, décision irréversible non documentée, ou deux échecs successifs sur le même obstacle.

## Commandes

| Action | Commande |
|---|---|
| Vérification complète (= CI) | `./mvnw verify` |
| Sans les tests d'intégration | `./mvnw verify -DskipITs` |
| Tests unitaires seuls | `./mvnw test` |
| Style seul | `./mvnw checkstyle:check` |
| Rapport de couverture | `target/site/jacoco/index.html` après `verify` |
| Lancer l'app + base | `docker compose up --build` |
| Activer les hooks git | `./scripts/install-hooks.sh` |

## Automatisation (workflows GitHub Actions)

- `ci` — vérification complète (`verify` + ITs Testcontainers), bloquante sur chaque PR et branche.
- `cd` — publie l'image `ghcr.io/baptiste-fournel/datacom` à chaque push sur `main`.
- `release` — crée la release GitHub sur chaque tag `v*`.
- `autoflow` — ouvre automatiquement la PR vers `develop` au push d'une branche `feature/**`.
- `project-sync` — tient à jour le board GitHub Projects (statuts et dates) et publie un rapport d'état.
- `dependabot` — met à jour les dépendances Maven, Actions et Docker chaque semaine.

## Définition of done d'un ticket

Critères d'acceptation couverts par des tests nommés d'après le comportement, CI verte (style, commentaires, architecture, tests, couverture), aucun commentaire hors exceptions, aucune règle métier inventée, PR mergée et ticket fermé.
