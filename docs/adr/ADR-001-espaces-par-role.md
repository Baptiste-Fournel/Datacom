# ADR-001 : Espaces séparés par rôle & statut « en attente de validation »

**Statut :** Accepté
**Date :** 27 juillet 2026
**Décideurs :** Baptiste — à présenter et justifier devant le jury du séminaire
**Portée :** DataCom, refonte (Java 17 / Spring Boot 3)

---

## Contexte

L'application existante ne connaît que deux statuts (`DRAFT`, `VALIDATED`) et une seule page `/product` qui sert à la fois la liste et le formulaire. Le champ `role` est chargé depuis la base mais **jamais utilisé** : n'importe quel utilisateur connecté peut déclencher `action=validate`, à n'importe quelle étape du workflow.

Or le métier décrit deux fonctions distinctes — l'**opérateur de saisie** qui renseigne les fiches, et le **responsable conformité** qui les contrôle — et un moment de **passation** entre les deux qui n'est aujourd'hui modélisé nulle part. Une fiche « terminée mais pas encore contrôlée » est indiscernable d'un brouillon à peine commencé.

Forces en présence :

- La **sécurité est l'axe explicitement évalué** du séminaire — toute règle d'accès doit être démontrable, pas cosmétique.
- La refonte se fait **en TDD**, avec respect des principes SOLID.
- Le périmètre est **petit** : la solution doit rester proportionnée (pas d'over-engineering).
- Le workflow en 4 étapes existe déjà et fonctionne ; on l'étend, on ne le réinvente pas.

Besoins exprimés :

1. Un statut intermédiaire marquant « saisie terminée, en attente de contrôle ».
2. Deux espaces distincts, un par rôle.
3. Le validateur ne voit que **sa file de travail** (fiches en attente), rien d'autre.
4. Le passage en attente de validation est une **action explicite** de l'opérateur.
5. Pas d'action de refus — la validation seule.

---

## Décision

Trois décisions liées :

**D1 — Étendre l'énumération de statut à trois valeurs, avec une machine à états explicite portée par l'agrégat `Product`.** Les transitions deviennent des méthodes gardées ; un état illégal est impossible à atteindre.

**D2 — Créer deux espaces de routes distincts** (`/produits` pour l'opérateur, `/validation` pour le validateur), chacun avec son controller et ses templates, sécurisés de façon déclarative au niveau des URL.

**D3 — Filtrer les données côté serveur, au niveau du repository**, via des méthodes qui portent l'intention métier — et garder chaque accès unitaire par une vérification de visibilité (protection IDOR).

---

## Options considérées

### Décision D1 — modélisation du statut

#### Option A — Étendre l'enum + machine à états dans l'agrégat *(retenue)*

| Dimension | Évaluation |
|---|---|
| Complexité | Faible |
| Testabilité | Excellente (domaine pur, sans infra) |
| Robustesse | Élevée — transition illégale impossible |
| Familiarité | Standard Java/DDD léger |

**Pour :** une seule source de vérité ; les règles métier sont lisibles au même endroit ; se teste sans base ni framework ; l'ajout d'un statut futur est additif.
**Contre :** demande d'écrire les gardes explicitement (quelques lignes).

#### Option B — Booléen `completed` à côté du statut existant

| Dimension | Évaluation |
|---|---|
| Complexité | Faible en apparence |
| Testabilité | Moyenne |
| Robustesse | **Faible** |
| Familiarité | Élevée |

**Pour :** modification minimale du schéma.
**Contre :** **deux sources de vérité pour un même concept** — rien n'empêche `completed = true` avec `status = VALIDATED`, ou l'inverse. Ces incohérences finissent toujours par arriver, et chaque lecture doit croiser deux champs.

#### Option C — Dériver le statut de `currentstep`

| Dimension | Évaluation |
|---|---|
| Complexité | Faible |
| Testabilité | Moyenne |
| Robustesse | Faible |
| Familiarité | Moyenne |

**Pour :** aucun champ supplémentaire.
**Contre :** rend indistinguables « étape 4 remplie mais non soumise » et « soumise au contrôle » — précisément la distinction demandée. Rend le statut implicite et non auditable.

### Décision D2 — organisation des pages

#### Option A — Deux routes et deux controllers *(retenue)*

| Dimension | Évaluation |
|---|---|
| Complexité | Faible à moyenne |
| Sécurité | **Déclarative, vérifiable d'un coup d'œil** |
| Maintenabilité | Élevée (une responsabilité par page) |
| Duplication | Maîtrisée via fragments partagés |

**Pour :** la règle d'accès tient en une ligne par espace dans la configuration de sécurité, ce qui est direct à tester et à défendre à l'oral ; chaque controller a une seule raison de changer (SRP) ; les deux interfaces peuvent diverger naturellement, car ce sont deux métiers différents ; une route non déclarée renvoie un 403, pas une fuite.
**Contre :** deux templates à maintenir — atténué en factorisant le layout et les blocs communs en fragments Thymeleaf.

#### Option B — Une route unique qui s'adapte au rôle

| Dimension | Évaluation |
|---|---|
| Complexité | Faible au début, croissante |
| Sécurité | **Fragile** |
| Maintenabilité | Faible |
| Duplication | Faible |

**Pour :** un seul controller, un seul template, moins de fichiers.
**Contre :** la logique d'accès se disperse en conditions dans le controller *et* dans le template ; **une branche oubliée est une fuite de données** ; le controller accumule les responsabilités des deux métiers et viole le SRP ; impossible de démontrer la règle de sécurité en un point unique.

#### Option C — Deux applications distinctes

Écartée sans analyse détaillée : disproportionné pour ce périmètre (deux déploiements, deux configurations, duplication massive) pour un bénéfice d'isolation dont on n'a aucun besoin ici.

### Décision D3 — emplacement du filtrage

| Emplacement | Verdict |
|---|---|
| **Repository, via des méthodes intentionnelles** | **Retenu** — le filtre est appliqué par la requête elle-même ; impossible de fuiter par distraction |
| Service, sur un `findAll()` générique | Écarté — fonctionne, mais un appelant peut court-circuiter le filtre ; charge inutilement toutes les lignes |
| Template / vue | **Écarté — dangereux.** Masquer dans la vue ne protège rien : les données transitent déjà, et l'URL directe reste accessible |

---

## Analyse des compromis

Le compromis central de **D2** est *duplication de templates* contre *sûreté et lisibilité de la règle d'accès*. À cette échelle, la duplication est faible et se factorise (fragments) ; la fragilité d'une page conditionnelle, elle, ne se factorise pas — elle se paie en revue de code et en failles. Sur un projet dont la sécurité est le critère noté, le choix est net.

Le compromis de **D1** est *quelques lignes de gardes explicites* contre *garantie d'intégrité*. Les gardes se testent en TDD en quelques minutes et suppriment définitivement la classe de bugs présente aujourd'hui (valider un brouillon, valider deux fois, valider sans droit).

Point important sur **D3** : filtrer la liste ne suffit **pas**. Un validateur dont la liste est filtrée peut toujours tenter `/validation/produits/104` sur une fiche en brouillon. Sans vérification par ressource, c'est une faille **IDOR** (référence directe non sécurisée) — une liste filtrée donne une fausse impression de sécurité. Les deux niveaux sont nécessaires.

---

## Conséquences

**Ce qui devient plus simple :**

- La règle « seul le responsable conformité valide » est appliquée en trois endroits cohérents (URL, service, domaine) au lieu de nulle part.
- Chaque page a une responsabilité unique ; les templates cessent d'accumuler des conditions.
- Les transitions illégales deviennent impossibles par construction, et se testent sans infrastructure.
- L'interface et la sécurité disent enfin la même chose : le validateur ne voit que ce sur quoi il peut agir.

**Ce qui devient plus contraignant :**

- Deux jeux de templates à maintenir (atténué par les fragments partagés).
- La navigation doit s'adapter au rôle, et la redirection après connexion doit router vers le bon espace.
- L'ajout d'un troisième rôle impliquerait un troisième espace — acceptable, mais à garder en tête.

**Ce qu'il faudra réexaminer :**

- **Le sort d'une fiche non conforme.** En l'absence d'action de refus (choix assumé), une fiche soumise que le validateur juge non conforme n'a **aucune porte de sortie** : elle reste en attente indéfiniment. C'est la limite connue du périmètre retenu. L'échappatoire est peu coûteuse si le besoin apparaît — ajouter `REJECTED` à l'enum et une transition `PENDING_VALIDATION → DRAFT` est purement additif, sans refonte.
- **La modifiabilité après soumission.** La décision retenue gèle la fiche une fois soumise (c'est le sens même d'une soumission : le validateur contrôle un état stable). Combiné au point précédent, cela signifie qu'une erreur de saisie découverte après soumission est bloquante. À surveiller à l'usage.

---

## Plan d'action

1. [ ] Écrire les tests du domaine (machine à états) — **rouges d'abord**
2. [ ] Implémenter `ProductStatus` et les transitions gardées sur `Product`
3. [ ] Définir les méthodes de repository intentionnelles + tests d'intégration Testcontainers
4. [ ] Configurer Spring Security (règles par URL, redirection post-connexion par rôle)
5. [ ] Implémenter les deux controllers et leurs templates, avec fragments partagés
6. [ ] Écrire les tests MockMvc d'accès croisé (403) et le test IDOR
7. [ ] Écrire la migration Flyway (statuts + nettoyage des rôles)

---

# Spécification d'implémentation

## 1. Domaine — la machine à états

Le chemin est linéaire, sans retour arrière :

```
DRAFT ──submitForValidation()──► PENDING_VALIDATION ──validate(user)──► VALIDATED
      (opérateur, 4 étapes OK)                       (validateur uniquement)     (terminal)
```

```java
public enum ProductStatus { DRAFT, PENDING_VALIDATION, VALIDATED }
```

Statuts en anglais dans le code, libellés français dans les templates — on ne mélange pas les langues dans le domaine.

```java
public class Product {

    private ProductStatus status;
    private WorkflowStep currentStep;

    /** L'opérateur soumet une fiche complète au contrôle de conformité. */
    public void submitForValidation() {
        requireStatus(DRAFT);
        if (!allStepsCompleted()) {
            throw new IncompleteProductException(id);
        }
        this.status = PENDING_VALIDATION;
    }

    /** Le responsable conformité valide une fiche en attente. */
    public void validate(AuthenticatedUser by) {
        if (!by.hasRole(Role.VALIDATOR)) {
            throw new AccessDeniedException("Seul le responsable conformité peut valider");
        }
        requireStatus(PENDING_VALIDATION);
        this.status = VALIDATED;
    }

    /** Une fiche soumise ou validée n'est plus modifiable. */
    public boolean isEditable() {
        return status == DRAFT;
    }

    private void requireStatus(ProductStatus expected) {
        if (this.status != expected) {
            throw new IllegalStatusTransitionException(this.status, expected);
        }
    }
}
```

Ces quelques lignes suppriment d'un coup les trois défauts actuels : valider un brouillon, valider deux fois, et valider sans en avoir le droit.

## 2. Repository — le filtrage porte l'intention

```java
public interface ProductRepository {
    List<Product> findAll();                            // espace opérateur — comportement actuel conservé
    List<Product> findByStatus(ProductStatus status);   // file du validateur
    Optional<Product> findById(ProductId id);
}
```

> **Note de périmètre :** l'opérateur continue de voir **toutes** les fiches (tous statuts confondus), exactement comme aujourd'hui — la restriction demandée ne portait que sur le validateur. Si tu veux plus tard qu'un opérateur ne voie que ses propres fiches, cela s'ajoute par une méthode `findByCreatedBy(UserId)` sans rien casser.

## 3. Application — la garde par ressource (IDOR)

```java
@Transactional(readOnly = true)
public ProductDetail openForValidation(ProductId id, AuthenticatedUser user) {
    Product product = repository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));

    if (product.status() != PENDING_VALIDATION) {
        throw new AccessDeniedException();   // ← la garde qu'on oublie systématiquement
    }
    return ProductDetail.from(product);
}
```

Sans ce contrôle, filtrer la liste ne protège rien : l'URL `/validation/produits/104` reste devinable.

## 4. Sécurité — règles déclaratives

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**").permitAll()
            .requestMatchers("/produits/**").hasRole("OPERATOR")
            .requestMatchers("/validation/**").hasRole("VALIDATOR")
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler(roleBasedRedirectHandler()))   // → /produits ou /validation
        .logout(logout -> logout.logoutSuccessUrl("/login"))
        .build();   // CSRF et régénération de session actifs par défaut
}
```

Défense en profondeur, sur trois niveaux qui ne font pas doublon : l'**URL** protège l'espace (grossier, déclaratif), le **service** protège la ressource (IDOR), le **domaine** protège l'invariant métier (`validate` refuse un non-validateur même appelé depuis du code interne).

## 5. Migration Flyway

```sql
-- V2__statut_en_attente_de_validation.sql

-- Les fiches déjà complètes mais non validées basculent en attente
UPDATE products
   SET status = 'PENDING_VALIDATION'
 WHERE status = 'DRAFT' AND currentstep = 4;

ALTER TABLE products
  ADD CONSTRAINT products_status_check
  CHECK (status IN ('DRAFT', 'PENDING_VALIDATION', 'VALIDATED'));

-- Alignement des rôles sur le métier (le sujet n'en définit que deux)
UPDATE users SET role = 'OPERATOR' WHERE role = 'ADMIN';
DELETE FROM users WHERE role = 'USER';   -- compte 'test' : aucun espace où atterrir
```

Le statut reste un `VARCHAR` côté base, mappé par `@Enumerated(EnumType.STRING)` — lisible en base, évolutif, sans type enum spécifique à PostgreSQL. La contrainte `CHECK` empêche toute valeur hors référentiel, y compris via un accès direct à la base.

## 6. Tests à écrire en premier (TDD)

**Domaine** — rapides, sans infrastructure :

- `nouveauProduit_estEnBrouillon`
- `brouillonIncomplet_neSoumetPas` → `IncompleteProductException`
- `brouillonComplet_passeEnAttenteDeValidation`
- `produitDejaSoumis_neSeResoumetPas`
- `brouillon_nePeutPasEtreValideDirectement`
- `validateur_valideUnProduitEnAttente`
- `operateur_nePeutPasValider` → `AccessDeniedException`
- `produitValide_neSeRevalidePas`
- `produitSoumis_nEstPlusModifiable`

**Persistance** (Testcontainers) :

- `findByStatus_neRemonteQueLeStatutDemande`

**Web** (MockMvc) — les tests d'accès croisé :

- `operateur_surEspaceValidation_recoit403`
- `validateur_surEspaceOperateur_recoit403`
- `validateur_ouvrantUnBrouillonParId_recoit403`  ← le test IDOR
- `utilisateurNonConnecte_estRedirigeVersLogin`

## 7. Impact sur l'interface

Un troisième badge « En attente de validation » (distinct du brouillon ambre et du validé vert). Côté opérateur, l'étape 4 troque son bouton de validation contre **« Soumettre à validation »**, et la fiche devient lecture seule une fois soumise. Côté validateur, l'espace est une **file d'attente** : uniquement les fiches en attente, ouvertes en lecture seule avec l'action de validation. La navigation et la redirection après connexion s'adaptent au rôle.

Voir la maquette `datacom-maquette-ui.html` pour le rendu des deux espaces.
