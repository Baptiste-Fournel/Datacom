# Specs front — DataCom (SvelteKit + Tailwind)

*Document de référence du développeur front. Contrat + conventions pour avancer sans dépendre du back. Toute évolution du contrat passe par une PR sur ce fichier. Décision d'architecture : `docs/adr/ADR-002-api-rest-front-sveltekit.md`. Référence visuelle : `docs/maquette-ui.html` (ouvrir dans un navigateur).*

---

## 1. Le produit en une minute

DataCom enregistre des **fiches produits** et les fait valider avant mise sur le marché. Deux rôles, deux espaces étanches :

- **OPERATOR** (opérateur de saisie) : crée des fiches, les remplit en **4 étapes** (Identification → Classification → Certification → Récapitulatif), puis les **soumet à validation**. Une fiche soumise est **gelée** (lecture seule).
- **VALIDATOR** (responsable conformité) : voit **uniquement** la file des fiches « en attente de validation », les contrôle en lecture seule et les **valide** (irréversible).

Cycle de vie d'une fiche : `DRAFT` → (soumission, opérateur, étape 4 requise) → `PENDING_VALIDATION` → (validation, validateur) → `VALIDATED`. Aucun refus, aucun retour arrière.

---

## 2. Repo & workflow git (commun aux deux devs)

- Repo front séparé (suggestion : `datacom-front`), même stratégie que le back : `main` (stable) / `develop` (intégration) / `feature/<slug>` — squash-merge, **un commit explicite par feature**, branche supprimée après merge, messages **Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`).
- PR obligatoire vers `develop`, CI verte avant merge (suggestion CI front : `lint` + `check` + `test` + `build`).

## 3. Conventions d'écriture (alignées sur le back)

- **Tous les identifiants en anglais** (variables, fonctions, composants, types). Aucun mélange français/anglais dans le code. Les **libellés visibles** sont en français et centralisés (un module `src/lib/labels.ts`), jamais en dur dans les composants.
- **Aucun commentaire dans le code.** Exception : `// Arrange` / `// Act` / `// Assert` (ou Given/When/Then) dans les tests. Un code qui a besoin d'un commentaire a besoin d'un meilleur nom.
- **Tests** (Vitest, et Playwright si e2e) nommés `should<ExpectedBehavior>_when<Condition>` — ex. `shouldRedirectToLogin_whenSessionExpired`.
- TypeScript **strict**, ESLint + Prettier (avec `prettier-plugin-tailwindcss` pour l'ordre des classes).
- SvelteKit : routes par dossiers (`src/routes/...`), composants en PascalCase dans `src/lib/components/`, accès API isolé dans `src/lib/api/` (un module par ressource — les composants n'appellent jamais `fetch` directement), types du contrat dans `src/lib/api/types.ts`.
- Tailwind : **aucune valeur magique** dans les classes (`text-[#2f5aa8]` interdit) — tout passe par les tokens du thème (§4).

## 4. Design tokens (à transposer dans `tailwind.config`)

Issus de la maquette de référence. Palette sobre « outil interne ».

| Token | Valeur | Usage |
|---|---|---|
| `bg` | `#f5f6f8` | fond de page |
| `surface` | `#ffffff` | cartes, barre d'app |
| `border` / `border-strong` | `#e4e7ec` / `#d0d5dd` | séparations, champs |
| `text` / `muted` / `soft` | `#1f2937` / `#667085` / `#98a2b3` | texte |
| `accent` / `accent-hover` / `accent-soft` | `#2f5aa8` / `#274c8f` / `#eaf0fa` | actions primaires, nav active |
| `success-bg` / `success-fg` | `#e7f6ec` / `#1c7a3e` | badge Validé, bouton Valider |
| `warning-bg` / `warning-fg` | `#fef3d7` / `#98650a` | badge Brouillon |
| `pending-bg` / `pending-fg` | `#ecebfa` / `#4c40a8` | badge En attente de validation |
| `danger-bg` / `danger-fg` | `#fdeceb` / `#b42318` | erreurs |
| rayons | `8px` (cartes) / `6px` (champs, boutons) | |
| police | pile système (`system-ui`) | pas de webfont |

Libellés des statuts (front) : `DRAFT` → « Brouillon », `PENDING_VALIDATION` → « En attente de validation », `VALIDATED` → « Validé ». Étapes : 1 « Identification », 2 « Classification », 3 « Certification », 4 « Récapitulatif » (affichage « Étape n/4 »).

## 5. Écrans & routes SvelteKit

| Route | Rôle | Contenu (cf. maquette) |
|---|---|---|
| `/login` | public | carte centrée, champs Identifiant/Mot de passe, erreur en bandeau |
| `/` | connecté | redirection selon le rôle → `/products` ou `/validation` |
| `/products` | OPERATOR | liste complète (réf, nom, badge statut, avancement Étape n/4, Ouvrir), bouton « Nouvelle fiche », état vide accueillant |
| `/products/[id]` | OPERATOR | stepper 4 étapes, formulaire par étape, « Enregistrer le brouillon » / « Enregistrer & continuer », étape 4 = récapitulatif + « Soumettre à validation » ; fiche non-`DRAFT` = lecture seule avec bandeau explicatif |
| `/validation` | VALIDATOR | file d'attente : uniquement les fiches `PENDING_VALIDATION`, action « Contrôler », état vide « Aucune fiche en attente » |
| `/validation/[id]` | VALIDATOR | récapitulatif lecture seule + encart « Validation de conformité » + bouton « Valider le produit » |

Règles transverses : la nav n'affiche que l'espace du rôle connecté ; un 401 → redirection `/login` ; un 403 → page « accès réservé » sobre ; états loading/empty/error prévus partout ; responsive ≥ 360 px ; accessibilité de base (labels reliés, focus visible, contrastes AA, statut jamais porté par la seule couleur).

## 6. Contrat d'API v1

Base : `/api`. JSON UTF-8. Authentification par **cookie de session HttpOnly** — toutes les requêtes en `credentials: 'include'`. **CSRF** : cookie `XSRF-TOKEN` (lisible JS) à renvoyer en header `X-XSRF-TOKEN` sur toute mutation (POST/PUT/DELETE). CORS ouvert en dev pour `http://localhost:5173`.

### Authentification

| Méthode & chemin | Corps | Réponses |
|---|---|---|
| `POST /api/auth/login` | `{"login": string, "password": string}` | `204` (cookies posés) · `401` identifiants invalides |
| `POST /api/auth/logout` | — | `204` |
| `GET /api/auth/me` | — | `200` `{"id", "login", "firstname", "lastname", "role": "OPERATOR"\|"VALIDATOR"}` · `401` |

### Espace opérateur (rôle OPERATOR requis — sinon `403`)

| Méthode & chemin | Corps | Réponses |
|---|---|---|
| `GET /api/products` | — | `200` `ProductSummary[]` |
| `POST /api/products` | — | `201` `ProductDetail` (brouillon vierge, étape 1) |
| `GET /api/products/{id}` | — | `200` `ProductDetail` · `404` |
| `PUT /api/products/{id}/identification` | `{"name", "reference", "description"}` | `200` `ProductDetail` · `409 NOT_EDITABLE` |
| `PUT /api/products/{id}/classification` | `{"category", "subcategory", "manufacturer", "country"}` | idem |
| `PUT /api/products/{id}/certification` | `{"lot", "certification", "validationComment"}` | idem |
| `POST /api/products/{id}/advance` | — | `200` `ProductDetail` · `409 NOT_EDITABLE` ou `409 ILLEGAL_TRANSITION` (déjà à l'étape finale) |
| `POST /api/products/{id}/submit` | — | `200` `ProductDetail` · `409 INCOMPLETE_PRODUCT` (étape 4 non atteinte) · `409 NOT_EDITABLE` (déjà soumis) |

### Espace validateur (rôle VALIDATOR requis — sinon `403`)

| Méthode & chemin | Corps | Réponses |
|---|---|---|
| `GET /api/validation/queue` | — | `200` `ProductSummary[]` (uniquement `PENDING_VALIDATION`) |
| `GET /api/validation/products/{id}` | — | `200` `ProductDetail` · `403` si la fiche n'est pas en attente (garde anti-IDOR) |
| `POST /api/validation/products/{id}/validate` | — | `200` `ProductDetail` (statut `VALIDATED`) · `403` idem |

### Types

```ts
type Role = 'OPERATOR' | 'VALIDATOR';
type ProductStatus = 'DRAFT' | 'PENDING_VALIDATION' | 'VALIDATED';

interface ProductSummary {
  id: number;
  name: string | null;
  reference: string | null;
  status: ProductStatus;
  currentStep: 1 | 2 | 3 | 4;
}

interface ProductDetail extends ProductSummary {
  description: string | null;
  category: string | null;
  subcategory: string | null;
  manufacturer: string | null;
  country: string | null;
  lot: string | null;
  certification: string | null;
  validationComment: string | null;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}
```

Le sélecteur Pays propose : France, Allemagne, Espagne, Italie, Chine, États-Unis (liste fermée côté UI, champ texte côté API).

### Erreurs — `application/problem+json`

```json
{ "type": "about:blank", "title": "Conflict", "status": 409,
  "detail": "The product is no longer editable", "code": "NOT_EDITABLE" }
```

Codes stables : `VALIDATION_ERROR` (400), `UNAUTHENTICATED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `NOT_EDITABLE` | `INCOMPLETE_PRODUCT` | `ILLEGAL_TRANSITION` (409). Le front mappe `code` → message français centralisé.

## 7. Avancer sans le back (mocks)

Tant que l'API n'est pas déployée : mocker dans SvelteKit (handlers `src/routes/api/...` de dev ou MSW) à partir des types du §6. Comptes de dev : `operator` / `operator` et `validator` / `validator`. Jeu de données conseillé : 4 fiches — une `DRAFT` étape 2, une `DRAFT` étape 4, une `PENDING_VALIDATION`, une `VALIDATED` (permet de couvrir tous les badges, la file du validateur et les états gelés).

## 8. Definition of done (front)

Écrans conformes à la maquette et aux tokens ; états loading/empty/error/403 traités ; libellés français centralisés ; identifiants anglais ; zéro commentaire hors AAA/GWT en tests ; lint + tests verts ; aucune valeur magique Tailwind ; navigation et actions conditionnées au rôle (et jamais **que** côté UI — l'API reste l'autorité).
