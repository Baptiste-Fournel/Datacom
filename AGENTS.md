# AGENTS.md — Règles de développement du projet Datacom

Refonte from scratch d'une application legacy (séminaire « Réécriture & sécurisation d'une application monolithique »). Contexte complet dans `docs/analyse-existant-et-refonte.md` et `docs/adr/ADR-001-espaces-par-role.md`. Périmètre actuel : **backend uniquement** (phases 0 à 3, puis web SSR en phase 4). La roadmap vit dans les issues GitHub et le board Projects « Datacom — Refonte ».

## Règles non négociables

1. **TDD strict.** Aucun code de production sans test rouge écrit d'abord. Cycle red → green → refactor, commits qui racontent ce cycle.
2. **Couverture bloquante.** JaCoCo : 80 % global, 90 % sur `domain` et `application`. Interdiction absolue d'écrire un test dont le seul but est de faire monter la couverture : chaque test vérifie un comportement métier nommé.
3. **Aucun commentaire dans le code.** Ni Javadoc, ni commentaire de bloc, ni commentaire de ligne dans `src/main`. Seule exception : `// Arrange`, `// Act`, `// Assert`, `// Given`, `// When`, `// Then` dans les tests. Règle exécutable : `CommentPolicyTest`. Si le code a besoin d'un commentaire, il a besoin d'un meilleur nom.
4. **Architecture dirigée vers le domaine.** Couches `domain` / `application` / `infrastructure` / `web`. Le domaine ne dépend de rien (Spring interdit dans le domaine), les repositories sont des interfaces du domaine implémentées par l'infrastructure. Règle exécutable : `ArchitectureRulesTest`.
5. **SOLID obligatoire**, en particulier : une raison de changer par classe (SRP), dépendre d'abstractions définies côté domaine (DIP).
6. **DRY et YAGNI.** Pas d'abstraction avant le deuxième usage réel. Pas de pattern sans problème présent. Pas de champ, paramètre ou indirection « pour plus tard ».
7. **Lisibilité maximale.** Méthodes ≤ 30 lignes, complexité cyclomatique ≤ 10, imbrication ≤ 2 (Checkstyle bloquant). Identifiants de production en anglais ; noms de méthodes de test en français descriptif (`brouillonComplet_passeEnAttenteDeValidation`).
8. **Ne rien inventer.** Toute règle métier absente de `docs/` ou des issues est une question pour Baptiste, pas une supposition. On pose la question et on s'arrête sur ce point-là (on continue sur un autre ticket si possible).

## Boucle de travail par ticket

1. Choisir le ticket ouvert le plus prioritaire (ordre des phases, puis ordre des numéros) ; le passer en « in progress ».
2. Créer une branche `feat/T<numero>-<slug>` depuis `main` à jour.
3. **Red** : écrire les tests qui expriment les critères d'acceptation du ticket ; vérifier qu'ils échouent.
4. **Green** : écrire le minimum de code de production pour les faire passer.
5. **Refactor** : nettoyer sans changer le comportement.
6. **Revue adversariale** : relire le diff en cherchant activement les violations (SOLID, YAGNI, lisibilité, sécurité, cas limites oubliés). Avec un agent : confier cette revue à un sous-agent au contexte vierge et traiter chaque objection.
7. Vérifier : `./mvnw verify` (dans un environnement sans Docker : `./mvnw verify -DskipITs`, la CI exécute les ITs).
8. Ouvrir une PR (template fourni), message et commits en Conventional Commits (`feat:`, `test:`, `refactor:`, `chore:`, `ci:`, `docs:`).
9. Merge (squash) uniquement quand la CI est verte ; fermer le ticket via `Closes #<numero>` ; passer au suivant.

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

- **Pousser une branche** `feat/Txx-...` → le workflow `autoflow` ouvre la PR automatiquement.
- **Lire le résultat de la CI** : `git fetch origin ci-status` puis lire `status/<sha>.json` sur cette branche (conclusion + dernières lignes du log Maven en cas d'échec). Le sha à chercher est celui du sommet de la branche poussée.
- **Merger** : uniquement quand le rapport est `success` → `git merge --no-ff feat/Txx` sur `main` avec `Closes #<numero>` dans le message, puis push de `main` (la PR se ferme en « merged », l'issue se ferme, `ci` et `cd` tournent sur main). Supprimer ensuite la branche distante.
- **Numéros des tickets** : `roadmap/issues.json` sur la branche `ci-status` (publié par le workflow `bootstrap-roadmap`).

## Définition of done d'un ticket

Critères d'acceptation couverts par des tests nommés d'après le comportement, CI verte (style, commentaires, architecture, tests, couverture), aucun commentaire hors exceptions, aucune règle métier inventée, PR mergée et ticket fermé.
