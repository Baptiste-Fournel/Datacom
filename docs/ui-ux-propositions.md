# DataCom — Propositions d'amélioration UI/UX

*À périmètre fonctionnel constant. On ne change **que** la façon de présenter les données et les actions déjà présentes dans le code (authentification, CRUD de fiche produit, workflow en 4 étapes, validation de conformité, consultation). **Aucune nouvelle fonctionnalité.** Ce document accompagne la maquette interactive `datacom-maquette-ui.html`.*

---

## 1. Principe directeur

L'UI actuelle est **fonctionnelle mais brute** : police Arial par défaut, mises en page en `<table>` recopiées à l'identique sur chaque page, styles inline dupliqués, aucune hiérarchie visuelle, aucune identité. Les propositions ci-dessous **réhabillent et réorganisent** l'existant. Règle que je me suis fixée : chaque amélioration doit se rattacher à une donnée ou une action **déjà** dans le code — voir le tableau de périmètre au §7.

Direction retenue : **refonte sobre « outil interne »** — propre, lisible, cohérente, sans fioritures. C'est le registre adapté à un outil métier de saisie/validation (densité d'information, rapidité, clarté priment sur l'esthétique « vitrine »).

---

## 2. Constats transverses sur l'UI actuelle

| Constat | Où |
|---|---|
| Aucun système visuel : Arial, bordures `1px #ccc`, pas de couleurs de marque, pas d'échelle typographique. | Toutes les pages |
| CSS dupliqué à l'identique (copier-coller du bloc `<style>`). | `login.jsp`, `home.jsp`, `product.jsp` |
| Mise en page par tableaux HTML, y compris pour les formulaires (pas de `<label>`). | `login.jsp`, `product.jsp` |
| Statuts affichés en texte cru (`DRAFT`, `VALIDATED`), sans code couleur. | liste + fiche |
| Workflow réduit à un « Current step : 2 » — aucun repère de progression. | `product.jsp` |
| Bloc « Debug information » exposant l'objet utilisateur à l'écran. | `product.jsp` |
| Image d'accueil : bitmap de 6 Mo en 800×600, lien « Login » en `font-size:60px`. | `index.jsp` |
| Non responsive (largeurs fixes, tableaux) ; pas d'états de focus ; contrastes faibles. | Toutes les pages |

---

## 3. Système de design proposé (sobre)

Une base légère, réutilisable (un seul jeu de styles partagé au lieu du copier-coller) :

- **Typographie** : pile système (`system-ui`) — nette, native, sans téléchargement de police.
- **Couleurs** : gris neutres pour le texte et les surfaces, **une** couleur d'accent (bleu ardoise) pour les actions primaires et les éléments actifs. Trois couleurs sémantiques réservées aux statuts (ambre = brouillon, vert = validé, gris = neutre).
- **Composants** : barre d'application (marque + navigation + utilisateur + déconnexion), cartes, boutons hiérarchisés (primaire / secondaire / discret), champs de formulaire avec label, badges de statut, stepper, tableau de données, messages d'alerte et toasts.
- **Espacement & rayons** : échelle cohérente, coins arrondis discrets, ombres légères.

Bénéfice transversal : cohérence entre écrans, code de présentation **factorisé** (aligné avec la refonte : ces styles vivront dans un layout Thymeleaf commun, plus dans chaque page).

---

## 4. Écran par écran

### 4.1 Connexion (`login.jsp`)

**Constat** — Titre « Authentication », formulaire en tableau sans labels associés, erreur affichée en simple `<p>` rouge, pas de focus, logo bitmap.

**Proposition** — Carte centrée, verticale : logo compact, titre, sous-titre, champs **avec labels** (`Identifiant`, `Mot de passe`), bouton primaire pleine largeur, focus visible, `autofocus` sur le premier champ. L'erreur devient un bandeau d'alerte lisible (icône + texte) au lieu d'une ligne rouge.

**Justification** — Le login est la première impression et le point d'entrée quotidien : lisibilité, accessibilité clavier et message d'erreur clair réduisent la friction. *Mêmes deux champs, même action `login`.*

### 4.2 Accueil (`index.jsp`)

**Constat** — Un bitmap de 6 Mo occupe l'écran, un « Welcome. », et un lien « Login » en 60 px. Rendu amateur et lent.

**Proposition** — Page d'entrée épurée : logo correctement dimensionné (idéalement SVG/PNG optimisé au lieu du BMP), phrase d'accroche courte, et un appel à l'action unique « Se connecter ». On supprime le bitmap surdimensionné.

**Justification** — Un asset de 6 Mo non compressé nuit au temps de chargement et à la crédibilité ; le contenu reste identique (accès à la connexion). *Aucune donnée nouvelle.*

### 4.3 Tableau de bord / Accueil connecté (`home.jsp`)

**Constat** — Après connexion, la page n'affiche que « Welcome John Doe » : espace quasi vide, sans orientation.

**Proposition** — En-tête de bienvenue + **deux cartes de navigation** vers des actions qui existent déjà : « Consulter les produits » (→ liste) et « Nouvelle fiche produit » (→ création). Rien de plus.

**Justification** — On transforme une page morte en point de départ utile, en **réutilisant les navigations existantes** (liens `product` et `product?action=new`). *Pas de widget de statistiques ni de contenu inventé — volontairement, pour rester strictement iso-fonctionnel.*

### 4.4 Liste des produits (`product.jsp`, mode `list`)

**Constat** — Tableau brut `Id | Name | Status | Step | Action`, statut en texte, lien « Open » nu, « New Product » en lien texte, état vide « No product. ».

**Proposition** — Barre d'outils avec titre, compteur de fiches et bouton primaire « Nouvelle fiche ». Tableau restylé : survol de ligne, **badge de statut coloré** (Brouillon / Validé), **indicateur d'avancement** visuel (barre + « Étape 2/4 »), et une **référence** produit sous le nom. État vide plus accueillant (« Aucune fiche produit — créez votre première fiche »).

**Justification** — Le badge et la barre transforment des colonnes déjà présentes (`status`, `currentstep`) en information scannable d'un coup d'œil. *On n'affiche que des champs déjà lus par le code actuel.*

### 4.5 Fiche produit — formulaire à étapes (`product.jsp`, mode `form`)

**Constat** — La progression est un simple « Current step : 2 ». Les 4 étapes sont des `<table>` sans labels ; navigation « Previous / Save / Save & Next » dans un tableau, avec un **formulaire imbriqué dans un autre** (HTML invalide). Les mêmes infos de statut/id sont répétées en bas.

**Proposition** — Un **stepper** en haut (1 Identification · 2 Classification · 3 Certification · 4 Validation) qui situe l'utilisateur et marque les étapes franchies. Chaque étape devient un formulaire avec **labels**, textes d'aide et champs espacés (le sélecteur Pays est conservé). Pied de page à hiérarchie claire : « Précédent » (secondaire), « Enregistrer le brouillon » (discret), « Enregistrer & continuer » (primaire). On corrige l'imbrication de formulaires.

**Justification** — Le workflow en 4 étapes **existe déjà** ; on le rend seulement lisible et navigable. Les noms d'étapes ne sont que des intitulés qui résument les champs déjà présents (nom/référence/description, catégorie/fabricant/pays, lot/certification, récap). *Aucune étape ni champ ajouté.*

### 4.6 Écran de validation (étape 4)

**Constat** — L'étape finale liste les champs en tableau, et le bouton « Validate Product » est **visible par tout le monde** — alors que le métier réserve la validation au responsable conformité.

**Proposition** — Récapitulatif en grille lisible (clé/valeur), puis un encart « Validation de conformité » avec le bouton **« Valider le produit »**. Ce bouton n'est présenté qu'au rôle **Validateur** ; pour l'Opérateur, un indicateur « 🔒 Réservé au responsable conformité » explique l'absence d'action.

**Justification** — Ne pas montrer une action qu'on ne peut pas exécuter est un principe UX de base, et cela **aligne l'interface sur la règle de rôle** déjà censée exister (le champ `role` est chargé mais inutilisé aujourd'hui). *C'est la feature de validation existante, simplement montrée à la bonne personne.*

---

## 5. Améliorations transverses

- **Feedback d'action** — Aujourd'hui, après « Save » on est redirigé sans confirmation. Ajout d'un **toast** discret (« Fiche enregistrée », « Produit validé ✓ ») sur les actions déjà existantes. Rassure sans changer le comportement.
- **Actions selon le rôle** — Masquer/expliquer les actions non autorisées (cf. §4.6). UX + cohérence avec la sécurité.
- **Retrait du bloc « Debug information »** — Il expose des données internes et n'a rien à faire dans une interface utilisateur (c'est aussi une fuite d'information relevée dans l'analyse technique).
- **Accessibilité** — `<label>` associés, `lang="fr"`, focus visible, statut jamais codé **uniquement** par la couleur (le texte « Brouillon »/« Validé » accompagne toujours la pastille), contrastes suffisants.
- **Responsive** — Grilles qui s'empilent, tableau à défilement horizontal maîtrisé, plus de largeurs fixes.
- **Assets** — Remplacer les BMP (8 Mo au total) par des images optimisées (SVG/PNG). Gain de performance immédiat.

---

## 6. Ce que je ne propose volontairement PAS (pour rester dans le périmètre)

Recherche/tri/filtre sur la liste, pagination, tableau de bord avec indicateurs, export, notifications, historique/audit, pièces jointes, thème sombre, édition en masse. **Tous seraient de nouvelles fonctionnalités** — hors sujet ici. À garder en tête pour une V2 éventuelle, pas pour cette refonte.

---

## 7. Périmètre : preuve qu'aucune feature n'est ajoutée

| Amélioration UI/UX | S'appuie sur (déjà dans le code) |
|---|---|
| Badge de statut | colonne `status` (`DRAFT`/`VALIDATED`), déjà lue |
| Indicateur d'avancement « Étape n/4 » | colonne `currentstep`, déjà lue |
| Stepper 4 étapes | logique `step 1..4` déjà présente dans `product.jsp` |
| Labels / intitulés d'étapes | champs de formulaire déjà existants (name, reference, category, lot…) |
| Récap de validation | branche `step == 4` déjà présente |
| Bouton « Valider » selon rôle | action `validate` + champ `role` déjà chargés |
| Cartes de navigation (accueil) | liens `product` et `product?action=new` déjà existants |
| Toast de confirmation | s'affiche sur les actions save/validate déjà existantes |
| État vide accueillant | cas « No product. » déjà géré |

---

## 8. Priorisation suggérée

**Gains rapides, fort impact** (peu d'effort) : système de design partagé + badges de statut + labels de formulaire + retrait du bloc debug + correction de l'imbrication de formulaires + assets optimisés.

**Impact fort, effort moyen** : stepper de workflow, écran de validation selon rôle, refonte de la liste (barre d'outils + avancement), page d'accueil connectée.

---

## 9. Lien avec la refonte technique

Dans l'architecture cible (Spring Boot + **Thymeleaf**), ces écrans deviennent des **templates** partageant un **layout commun** (barre d'app, styles, composants) — ce qui supprime nativement la duplication de CSS/HTML constatée dans les JSP. Le badge, le stepper et le récap sont des **fragments Thymeleaf** réutilisables. La règle « le bouton Valider selon le rôle » est portée côté serveur par Spring Security (affichage conditionnel `sec:authorize`) **et** vérifiée côté domaine — l'UI et la sécurité disent alors la même chose.

---

*Maquette interactive fournie : `datacom-maquette-ui.html` — navigue entre les écrans via la barre du haut et bascule le rôle Opérateur/Validateur pour voir l'adaptation de l'écran de validation.*
