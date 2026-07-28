# Contribuer à DataCom

Merci de contribuer. Ce projet suit des règles strictes et **exécutables** : la CI les vérifie et bloque toute violation. Les règles complètes vivent dans [`AGENTS.md`](AGENTS.md) ; ce document en donne l'essentiel côté contributeur.

## Prérequis

JDK 21, Docker. À la première installation : `./scripts/install-hooks.sh` (active les hooks git pre-commit / pre-push).

## Boucle de travail

1. Une branche par ticket : `feature/T<numéro>-<slug>` depuis `develop`.
2. **TDD strict** : écrire le test qui échoue d'abord, puis le code minimal, puis refactorer.
3. Vérifier localement : `./mvnw verify` (identique à la CI).
4. Ouvrir une Pull Request vers `develop` (le workflow `autoflow` la crée automatiquement au push).
5. Merge en **squash** une fois la CI verte : un commit explicite par ticket.

Les releases (fin de phase) fusionnent `develop → main` et sont taguées `vX.Y.Z`.

## Règles non négociables (vérifiées par la CI)

- **Aucun commentaire** dans le code de production. Dans les tests, seuls les marqueurs de structure `// Arrange` / `// Act` / `// Assert` (unitaires) ou `// Given` / `// When` / `// Then` (intégration), **obligatoires**.
- **Tous les identifiants en anglais.** Tests nommés `should<Comportement>_when<Condition>`.
- **Architecture dirigée vers le domaine** : `web → application → domain ← infrastructure`. Le domaine ne dépend d'aucun framework (ArchUnit).
- **Couverture** : 80 % global, 90 % `domain` + `application`. Interdiction d'un test dont le seul but est la couverture.
- **SOLID, DRY, YAGNI** : pas d'abstraction avant le deuxième usage réel.
- Messages de commit en [Conventional Commits](https://www.conventionalcommits.org/fr/) (`feat:`, `fix:`, `test:`, `refactor:`, `chore:`, `ci:`, `docs:`).

## Commandes utiles

| Action | Commande |
|---|---|
| Vérification complète (= CI) | `./mvnw verify` |
| Tests unitaires seuls | `./mvnw test` |
| Style seul | `./mvnw checkstyle:check` |
| Lancer l'app + base | `docker compose up --build` |
