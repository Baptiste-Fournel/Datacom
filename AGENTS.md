# AGENTS.md — Règles de développement du projet Datacom

Refonte from scratch d'une application legacy (séminaire « Réécriture & sécurisation d'une application monolithique »). Contexte complet dans `docs/analyse-existant-et-refonte.md`, `docs/adr/ADR-001-espaces-par-role.md` et `docs/adr/ADR-002-api-rest-front-sveltekit.md`. Ce dépôt = **backend API REST** ; le front SvelteKit vit dans un dépôt séparé tenu par le développeur front — le contrat partagé est `docs/openapi.yaml` (OpenAPI 3, toute évolution par PR). La phase 4 s'implémente en endpoints REST, la sécurité (phase 3) en login JSON + CSRF cookie. La roadmap vit dans les issues GitHub et le board Projects.

## Règles non négociables

1. **TDD strict.** Aucun code de production sans test rouge écrit d'abord. Cycle red → green → refactor, commits qui racontent ce cycle.
2. **Couverture bloquante.** JaCoCo : 80 % global, 90 % sur `domain` et `application`. Interdiction absolue d'écrire un test dont le seul but est de faire monter la couverture : chaque test vérifie un comportement métier nommé.
3. **Aucun commentaire dans le code.** Ni Javadoc, ni commentaire de bloc, ni commentaire de ligne dans `src/main`. Seule exception : `// Arrange`, `// Act`, `// Assert`, `// Given`, `// When`, `// Then` dans les tests. Règle exécutable : `CommentPolicyTest`. Si le code a besoin d'un commentaire, il a besoin d'un meilleur nom.
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

## Environnements

- **CI GitHub Actions** : juge de paix, exécute tout (`verify`, ITs Testcontainers incluses).
- **Machine locale (Mac de Baptiste)** : tout fonctionne, activer les hooks git à la première installation.
- **Sandbox cloud de l'agent** : Maven Central et l'API REST GitHub y sont inaccessibles — seul le protocole git passe. Ne pas builder localement, ne pas appeler l'API : piloter par git uniquement (voir ci-dessous).

## Pilotage GitHub par git uniquement (depuis le sandbox)

- **Pousser une branche** `feature/Txx-...` → le workflow `autoflow` ouvre la PR vers `develop` automatiquement.
- **Lire le résultat de la CI** : `git fetch origin ci-status` puis lire `status/<sha>.json` sur cette branche (conclusion + dernières lignes du log Maven en cas d'échec). Le sha à chercher est celui du sommet de la branche poussée.
- **Merger** : uniquement quand le rapport est `success` → `git merge --squash feature/Txx` sur `develop`, un commit explicite, puis push. Fermer le ticket par tag ops (`done <n>`) et supprimer la branche distante. Nota : le squash ne fait pas passer la PR en « merged » — la fermer via le tag ops (`comment` + fermeture auto par GitHub quand la branche est supprimée).
- **Cycle de vie des tickets** : pousser un tag annoté `ops-<slug>` dont le message contient une commande par ligne — `start <n>` (label « in progress »), `done <n> [commentaire]` (retire le label et ferme l'issue), `comment <n> <texte>`, `reopen <n>`. Le workflow `ops` exécute avec les droits du repo puis supprime le tag ; rapport dans `ops.json` sur `ci-status`. Rituel : `start` à l'ouverture de la branche, `done` au merge dans `develop`.
- **Releases** : merge `develop → main` + tag `v0.<phase>` quand une phase est complète (jugement au fil de l'eau) ; la CD publie l'image.
- **Numéros des tickets** : `roadmap/issues.json` sur la branche `ci-status` (publié par le workflow `bootstrap-roadmap`).

## Définition of done d'un ticket

Critères d'acceptation couverts par des tests nommés d'après le comportement, CI verte (style, commentaires, architecture, tests, couverture), aucun commentaire hors exceptions, aucune règle métier inventée, PR mergée et ticket fermé.
