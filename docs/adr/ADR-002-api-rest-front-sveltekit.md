# ADR-002 : API REST + front SvelteKit séparé

**Statut :** Accepté — remplace la décision d'interface de l'ADR-001 (rendu serveur Thymeleaf)
**Date :** 27 juillet 2026
**Décideurs :** Baptiste (back), validé pour intégration côté front

## Contexte

La décision « rendu côté serveur Thymeleaf » avait été prise dans un contexte **solo**. Fait nouveau : un **développeur front dédié** rejoint le projet, outillé **SvelteKit + Tailwind**, et doit pouvoir avancer en parallèle sans dépendre du rythme du back.

## Décision

Le backend expose une **API REST** (sessions Spring Security, pas de rendu HTML) ; le front est une application **SvelteKit** dans un dépôt séparé, consommant cette API. Le contrat vit dans `docs/openapi.yaml` (OpenAPI 3) et toute évolution passe par une PR sur ce fichier.

## Options considérées

| Option | Verdict |
|---|---|
| Conserver le SSR : le dev front livre du HTML/Tailwind intégré ensuite en Thymeleaf | Écarté — gaspille la compétence du coéquipier, crée un handoff artificiel (HTML → retravail Java), sérialise le travail au lieu de le paralléliser |
| **API REST + SvelteKit séparé** | **Retenu** — parallélisation réelle, contrat explicite, chacun dans sa stack ; l'axe sécurité du séminaire se démontre au niveau API (sessions, CSRF, autorisation par espace, garde IDOR) |
| SvelteKit full-stack (back Node) | Écarté — sort du périmètre de la refonte Java actée |

## Choix techniques associés (anti-over-engineering)

- **Authentification par session (cookie HttpOnly)**, pas de JWT : même origine de confiance, un seul back, révocation triviale, Spring Security le fait nativement. Un JWT n'apporterait ici que de la surface d'erreur.
- **CSRF** : cookie `XSRF-TOKEN` + header `X-XSRF-TOKEN` (CookieCsrfTokenRepository) — le standard Spring pour les SPA.
- **Erreurs** : `application/problem+json` (RFC 7807) avec un champ `code` métier stable.
- **CORS** : origines de dev (localhost:5173) et de prod explicitement listées, `credentials: include`.

## Conséquences

- Les tickets de la phase 4 (T14-T18) sont **réinterprétés en endpoints REST** (mêmes périmètres fonctionnels, plus de templates) ; les tickets sécurité T11-T13 passent au login JSON + CSRF cookie. Les trois niveaux de défense de l'ADR-001 (URL → service → domaine) restent inchangés.
- La maquette `docs/maquette-ui.html` reste la **référence visuelle** ; ses tokens sont transposés dans la config Tailwind du front.
- Le repo front est créé et tenu par le développeur front ; le back n'héberge que le contrat OpenAPI.
