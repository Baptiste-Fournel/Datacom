# DataCom — Analyse de l'existant & stratégie de refonte

*Dossier technique — analyse du legacy et justification des choix de refonte. Séminaire « Réécriture & sécurisation d'une application monolithique ».*

---

## 1. Contexte & objectif

DataCom est une application interne de **commercialisation de produits** : on enregistre des fiches produit, on suit leur avancement dans un workflow, puis un responsable en valide la conformité avant mise sur le marché. Deux profils d'utilisateurs : l'**opérateur de saisie** (Admin) qui crée/édite les fiches, et le **responsable conformité** (Validateur) qui valide.

L'objectif n'est **pas** d'ajouter des fonctionnalités, mais de **reprendre l'existant** et de le réécrire proprement : dette technique traitée, responsabilités séparées, application sécurisée, code testé (TDD) et respect du Clean Code / des principes SOLID. Ce document couvre l'**analyse** et les **choix** ; il précède l'implémentation.

**Périmètre fonctionnel à reproduire (iso-fonctionnel) :** authentification, création d'une fiche produit, saisie multi-étapes (4 étapes), consultation de la liste et du détail, validation de conformité. Rien de plus.

---

## 2. Analyse de l'existant

### 2.1 Stack réelle constatée

| Élément | Détail |
|---|---|
| Langage | Java 11 |
| Web | Servlets `javax.servlet` 4.0.1 + JSP à scriptlets |
| Accès données | JDBC brut (`DriverManager` + `Statement`), aucune abstraction |
| Base | PostgreSQL 16 |
| Build / packaging | Maven → WAR |
| Runtime | Tomcat 9 |
| Conteneurisation | Docker + docker-compose (web + postgres) |
| Tests | **Aucun** (pas de `src/test`, pas de JUnit) |
| Versioning | **Aucun dépôt Git** (pas de `.git`, seul un `.gitignore` est présent) |

Le périmètre est **petit et clair** (≈ 12 fichiers source utiles). C'est un point positif : une réécriture *from scratch* est réaliste, à faible risque, et se prête très bien au TDD.

### 2.2 Inventaire de la dette technique

Chaque constat ci-dessous est **vérifié dans le code**. Sévérité : 🔴 critique · 🟠 majeur · 🟡 mineur.

#### Sécurité — l'axe explicitement pointé par le sujet (« /!\ Contrôle qualité & sécurité /!\ »)

| # | Sév. | Constat | Preuve |
|---|---|---|---|
| S1 | 🔴 | **Injection SQL généralisée** : toutes les requêtes sont concaténées, aucun `PreparedStatement`. | `LoginServlet` : `"SELECT * FROM users WHERE login='" + login + "' AND password='" + password + "'"`. Contournable via `' OR '1'='1' -- `. Idem sur `id` (`ProductServlet` edit/save/validate) et sur tous les champs texte du `save`. |
| S2 | 🔴 | **Mots de passe en clair** : stockés en clair, comparés en clair, et **replacés dans la session**. | `file.sql` : `INSERT ... VALUES ('admin','admin', ...)` ; `LoginServlet` : `user.setPassword(result.getString("password"))`. Aucun hachage. |
| S3 | 🔴 | **Aucune autorisation par rôle** : le rôle est chargé mais jamais utilisé. N'importe quel utilisateur connecté peut valider. | `ProductServlet.doPost`, branche `action=validate` : aucune vérification de `user.getRole()`. La règle « seul le Validateur valide » n'existe pas. |
| S4 | 🟠 | **Secrets en dur dans le code source** (committés). | `Database.java` : `USER = "postgres"`, `PASSWORD = "postgres"`, URL en dur. |
| S5 | 🟠 | **Fuite d'informations** : requêtes SQL (donc mots de passe) loguées, objet utilisateur complet affiché à l'écran. | `System.out.println(sql)` partout ; bloc « Debug information » de `product.jsp` : `<%=session.getAttribute("user")%>`. |
| S6 | 🟠 | **Pas de protection CSRF** sur les POST (login/save/validate) ; **pas de régénération d'ID de session** au login (fixation de session possible). | Formulaires `product.jsp` / `login.jsp` sans token ; `request.getSession(true)` sans `changeSessionId()`. |

#### Architecture & séparation des responsabilités

| # | Sév. | Constat | Preuve |
|---|---|---|---|
| A1 | 🔴 | **Aucune couche** : le Servlet fait HTTP + logique métier + SQL. | `ProductServlet` : 537 lignes mêlant routage, règles et requêtes. |
| A2 | 🟠 | **Vue couplée à la base** : un `ResultSet` JDBC est passé à la JSP, qui itère un curseur pendant le rendu. | `ProductServlet` : `request.setAttribute("products", result)` ; `product.jsp` : `while(rs.next())`. |
| A3 | 🟠 | **Ressources non fermées proprement** : pas de `try-with-resources` ni `finally` → fuite de connexions sur exception ; une connexion physique par requête (pas de pool). | `Database.getConnection()` via `DriverManager` à chaque appel. |
| A4 | 🟠 | **Workflow fragile** : `currentstep` incrémenté à la main, plafonné à 4 en dur, sans vérifier que les étapes précédentes sont complètes. | `ProductServlet.doPost` : `currentStep++; if(currentStep > 4) currentStep = 4;`. |

#### Clean Code / SOLID

| # | Sév. | Constat | Preuve |
|---|---|---|---|
| C1 | 🟠 | **Code dupliqué & code mort** : deux modèles utilisateur quasi identiques dont un inutilisé ; JS mort. | `User` (jamais utilisée) vs `UserCopy` (partout) ; `UserCopy.age` jamais lu ; `app.js` vide ; `validateForm(){ return true; }`. |
| C2 | 🟠 | **Primitive obsession** : statuts/rôles/étapes en chaînes magiques ; dates en `VARCHAR`. | `'DRAFT'`, `'VALIDATED'`, `'ADMIN'`… en dur ; `createdat`/`updatedat VARCHAR(255)`. |
| C3 | 🟡 | **Gestion d'erreurs incohérente**. | Tantôt `throw new ServletException(e)`, tantôt `throw new RuntimeException(e)` (même classe), `printStackTrace()` partout. |
| C4 | 🟡 | **Bug réel** : lecture hors du `if(result.next())`. | `ProductServlet` edit : `result.getString("validation")` après la fermeture du `if` → un `id` inexistant fait planter la page (500). |
| C5 | 🟡 | **Incohérence de schéma** : colonne jamais utilisée. | `products.conformity` n'est ni lue ni écrite. |
| C6 | 🟡 | **Présentation dupliquée & HTML invalide**. | Bloc `<style>` copié-collé à l'identique dans chaque JSP ; `<form>` imbriqué dans un autre `<form>` (`product.jsp`). |
| C7 | 🟡 | **Hygiène des assets** : ~8 Mo de bitmaps non compressés committés. | `productpark.bmp` (6 Mo) + `datacom.bmp` (2 Mo). |

#### Process (exigé par le séminaire)

| # | Sév. | Constat |
|---|---|---|
| P1 | 🔴 | **Zéro test** alors que le TDD est demandé. |
| P2 | 🟠 | **Pas de dépôt Git** alors que « Utilisation de GIT dans un cadre collaboratif » est un objectif. |

---

## 3. Stratégie de refonte

**Réécriture *from scratch***, pas de rustines sur le legacy : la dette est structurelle (injection, absence de couches, aucun test), et le périmètre est assez petit pour reconstruire proprement en TDD sans risque.

Trois axes, dans cet ordre de priorité, alignés sur la grille du séminaire :

1. **Sécuriser** — supprimer l'injection SQL par construction, hacher les mots de passe, imposer l'autorisation par rôle, externaliser les secrets. C'est l'axe le plus noté.
2. **Structurer** — séparer présentation / application / domaine / infrastructure ; encoder le workflow comme une vraie machine à états.
3. **Fiabiliser** — TDD, gestion d'erreurs centralisée, schéma corrigé, Git dès le départ.

On reste **iso-fonctionnel** : mêmes écrans, même parcours. On corrige au passage les **règles métier cassées** (rôles non appliqués, workflow non contraint, bug C4) — c'est le sens même de « sécurisation », pas une nouvelle feature.

---

## 4. Choix technologiques (justifiés)

### 4.1 Langage & framework → Java 17 (LTS) + Spring Boot 3

Le langage était libre (tu es à l'aise en Java et en TypeScript). Choix fait **objectivement, sur les mérites du projet** :

- **La sécurité est l'axe noté, et Spring Security la traite avec des composants éprouvés** : authentification, autorisation par rôle, hachage BCrypt, protection CSRF, régénération de session — configuration plutôt que code maison. En Node/TS il faut assembler soi-même Passport + bcrypt + csurf, ce qui multiplie la surface d'erreur sur précisément ce qui est évalué.
- **Rendu côté serveur = terrain de prédilection de Spring MVC + Thymeleaf.** Le SSR moderne en TS pousse vers React/Next (SPA), ce qui contredit ton choix « rendu serveur, pas de front séparé ».
- **JPA rend l'injection SQL structurellement impossible** (requêtes paramétrées par défaut) et supprime tout le JDBC manuel.
- **Injection de dépendances native** → inversion de dépendances (le « D » de SOLID) et testabilité, sans effort.
- **Cohérence avec le sujet** : on modernise du legacy Java — exactement « être capable de reprendre un logiciel existant ».
- Passage de **Java 11 → 17** (LTS, 11 en fin de vie) : `record` pour les DTO, `switch` expressions, `sealed` pour modéliser des états.

> **Alternative honnête :** NestJS (TypeScript) est l'équivalent le plus proche (structuré, à base d'injection de dépendances) et resterait un choix défendable. Il perd ici sur deux points concrets : moins « batteries incluses » côté sécurité, et un rendu HTML serveur moins premier-plan que Thymeleaf. Si tu préfères tout de même le TS, la même architecture se transpose — dis-le-moi et j'adapte.

### 4.2 Reste de la stack

| Besoin | Choix | Justification |
|---|---|---|
| Persistance | **Spring Data JPA / Hibernate** | Repositories via interfaces, requêtes paramétrées → fin de l'injection SQL ; mapping objet propre. |
| Migrations BDD | **Flyway** | Schéma versionné et reproductible (remplace le `file.sql` lancé à la main) ; corrige les types (`timestamp` au lieu de `VARCHAR`). |
| Sécurité | **Spring Security** | Auth + rôles + BCrypt + CSRF + anti-fixation de session. Cœur du volet « sécurisation ». |
| Vue | **Thymeleaf** | Templates serveur sans logique métier possible (bonne contrainte, remplace les scriptlets JSP). |
| Base | **PostgreSQL** (conservée) | Aucun besoin d'en changer ; le schéma est simple. |
| Build | **Maven** (conservé) | Déjà en place, suffisant ; migration WAR→JAR exécutable. |
| Conteneur | **Docker Compose** (conservé) | Spring Boot embarque Tomcat → image mono-JAR, Dockerfile simplifié. |
| Tests | **JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers** | Stack TDD standard de l'écosystème (détail §7). |
| Logs | **SLF4J + Logback** (fourni) | Remplace `System.out.println` / `printStackTrace` par des niveaux maîtrisés. |

---

## 5. Architecture cible

### 5.1 Le style retenu : couches propres + inversion de dépendance sur le domaine

Objectivement, pour **un seul contexte métier, ~2 entités et une seule intégration (la BDD)**, l'**architecture hexagonale complète (ports & adapters partout) serait de l'over-engineering** : sa cérémonie ne se rentabilise que quand le domaine est complexe, les intégrations nombreuses/volatiles ou la durée de vie longue — ce n'est pas le cas ici.

On adopte donc une **architecture en couches classique**, en lui empruntant la **seule idée d'hexagonal qui compte à cette échelle** : la **règle de dépendance dirigée vers le domaine**. Le domaine ne dépend de rien ; les `Repository` sont des **interfaces définies dans le domaine** et **implémentées dans l'infrastructure**. On obtient ~80 % du bénéfice (domaine pur, testable, indépendant du framework) pour ~20 % du coût.

```
        web  ─────────►  application  ─────────►  domain  ◄─────────  infrastructure
   (Controllers,          (Services,            (Entities, VOs,        (Spring Data JPA,
    Thymeleaf, DTO)        use cases,            enums, State machine,   Security config,
                           transactions)         Repository interfaces)  Flyway)
```

Règle de dépendance : **tout pointe vers le `domain`, qui ne dépend de personne.**

### 5.2 Découpage en paquets proposé

```
com.datacom
├── domain/
│   ├── product/  Product, ProductStatus, WorkflowStep, ProductRepository (interface)
│   └── user/     User, Role, UserRepository (interface)
├── application/  ProductService, AuthenticationService  (orchestration, @Transactional)
├── infrastructure/
│   ├── persistence/  implémentations Spring Data des repositories du domaine
│   ├── security/     SecurityConfig, PasswordEncoder (BCrypt), UserDetails
│   └── config/
└── web/
    ├── ProductController, AuthController
    └── dto/          + mappers (web ⇄ domaine, jamais l'entité brute exposée)
src/test/java/com/datacom/...   (miroir de l'arborescence)
resources/
├── templates/        (Thymeleaf)
├── db/migration/      (Flyway : V1__init.sql, ...)
└── application.yml
```

### 5.3 Le seul curseur « puriste » à connaître (pour l'oral)

L'hexagonal strict interdirait les annotations JPA sur l'entité de domaine (il faudrait un modèle de persistance séparé + un mapper). **Pour une appli de cette taille, annoter directement l'entité de domaine et se passer de cette couche de mapping est un compromis pragmatique largement accepté** — c'est ce que fait la majorité des projets Spring « clean ». Si ton évaluateur est puriste sur ce point, la séparation se rajoute sans réarchitecturer. Je pars sur le compromis pragmatique par défaut, en le documentant comme un choix assumé.

---

## 6. Design patterns (dosés, sans excès)

| Pattern | Où | Pourquoi | 
|---|---|---|
| **Injection de dépendances** | Partout (Spring) | Inversion de dépendances (DIP), testabilité. Non négociable. |
| **Repository** | `domain` (interface) + `infrastructure` (impl) | Abstrait la persistance ; permet de tester l'application avec un repo en mémoire/mocké. |
| **DTO + Mapper** | `web` | Découple les écrans du domaine ; empêche d'exposer l'entité (et le mot de passe). `record` Java. |
| **Machine à états (enum)** | `domain/product` | Remplace le `currentstep++` : `WorkflowStep` déclare ses transitions autorisées ; `Product` refuse une transition invalide. Encode « on ne valide qu'à l'étape finale ». |

**Volontairement écartés (YAGNI) :** CQRS, event sourcing, microservices, DDD tactique lourd (agrégats multiples, domain events), Strategy/Factory pour la validation. Aucun besoin réel à ce périmètre ; les introduire nuirait à la maintenabilité. Un principe directeur : *le pattern doit résoudre un problème présent, pas un problème imaginé.*

---

## 7. Sécurité — correspondance faille → correctif

C'est le tableau à présenter à l'oral : chaque problème de l'existant a une réponse dans la cible.

| Faille existante | Correctif dans la refonte |
|---|---|
| S1 Injection SQL | JPA / requêtes paramétrées **par construction** — plus aucune concaténation. |
| S2 Mots de passe en clair | **BCrypt** (`PasswordEncoder` Spring Security) ; le hash ne transite ni en session ni en DTO. |
| S3 Pas d'autorisation | **Spring Security** : `validate` réservé au rôle `VALIDATOR` (`@PreAuthorize` / règles d'accès) ; la règle vit aussi dans le domaine. |
| S4 Secrets en dur | Externalisés (variables d'environnement / `application.yml` non committé, profils). |
| S5 SQL loggué / debug exposé | Logger SLF4J avec niveaux adaptés ; suppression du bloc « Debug information ». |
| S6 CSRF / fixation de session | CSRF activé par défaut (token dans les formulaires Thymeleaf) ; régénération de l'ID de session au login (défaut Spring Security). |

---

## 8. Stratégie de tests (TDD)

**Cycle red-green-refactor**, en attaquant par le cœur métier — la partie pure, la plus rentable à tester.

**Pyramide :**
- **Beaucoup d'unitaires** sur le `domain` (machine à états, règle de rôle) et l'`application` (services, repositories mockés) — rapides, sans infra.
- **Quelques tests de tranche web** (`@WebMvcTest` + `MockMvc`) : routage, sécurité des endpoints, mapping DTO.
- **Peu d'intégration** mais ciblés : repositories réels sur un **PostgreSQL jetable via Testcontainers**, et un test de bout en bout du parcours de validation.

**Outils :** JUnit 5, AssertJ (assertions lisibles), Mockito, `spring-boot-starter-test`, MockMvc, Testcontainers.

**Premiers tests à écrire (rouges d'abord) :**
- `newProduct_startsInDraftAtStep1`
- `onlyValidator_canValidate` / `operator_cannotValidate_isRejected`
- `cannotAdvanceBeyondFinalStep`
- `cannotValidate_beforeReachingFinalStep`
- `login_withWrongPassword_isRejected` et `password_isStoredHashed_neverPlaintext`

Couverture **ciblée métier + sécurité**, pas 100 % dogmatique : on teste ce qui porte du risque, pas les getters.

---

## 9. Git & collaboration (exigé par le séminaire)

- **Initialiser le dépôt dès le commit 0** (squelette + `.gitignore` déjà présent, à compléter pour exclure `target/`, secrets).
- **GitHub Flow léger** : `main` toujours déployable, une branche par tâche, Pull Request pour intégrer.
- **Commits atomiques** avec messages clairs (idéalement *Conventional Commits* : `feat:`, `fix:`, `test:`, `refactor:`) — ça raconte l'histoire de la refonte, utile pour l'évaluation.
- **CI (GitHub Actions)** qui lance `mvn test` à chaque push : garantit que le TDD ne se dégrade pas.

---

## 10. Feuille de route proposée

| Phase | Contenu | Livrable |
|---|---|---|
| 0 | Init Git + squelette Spring Boot + Docker Compose + Flyway (schéma corrigé : `timestamp`, colonnes mortes retirées, conformité modélisée proprement). | Projet qui démarre, `docker compose up`. |
| 1 | **Domaine en TDD** : `Product`, `WorkflowStep` (machine à états), `User`, `Role`, règles métier. | Domaine 100 % testé, sans infra. |
| 2 | **Persistance** : entités JPA + repositories + tests d'intégration Testcontainers. | Lecture/écriture réelle testée. |
| 3 | **Sécurité** : Spring Security, BCrypt, rôles, CSRF, secrets externalisés. | Auth + autorisation testées. |
| 4 | **Web** : controllers + templates Thymeleaf + DTO, parcours iso-fonctionnel. | Écrans équivalents à l'existant. |
| 5 | **Durcissement + docs + CI** : gestion d'erreurs centralisée (`@ControllerAdvice`), logs, README, pipeline. | Appli livrable + dossier technique final. |

---

## 11. Prochaine étape

Ce document couvre l'**analyse** et les **choix** — ce que tu m'avais demandé pour l'instant. Quand tu veux avancer, je peux :

- **initialiser le squelette du projet** (phase 0) et lancer le TDD du domaine (phase 1), ou
- détailler un point précis (modèle de données cible, config Spring Security, exemples de tests, machine à états du workflow), ou
- préparer la trame de la **présentation orale** à partir de ce dossier.

Dis-moi par quel bout tu veux commencer.
