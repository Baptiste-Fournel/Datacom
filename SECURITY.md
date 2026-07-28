# Politique de sécurité

La sécurité est un objectif central de DataCom. Cette page résume les mesures en place et la marche à suivre pour signaler une vulnérabilité.

## Versions supportées

| Version | Supportée |
|---|---|
| 1.0.x | ✅ |
| < 1.0 | ❌ |

## Signaler une vulnérabilité

Merci de **ne pas** ouvrir d'issue publique pour une faille de sécurité. Utilisez l'onglet **Security → Report a vulnerability** du dépôt (GitHub Private Vulnerability Reporting), ou contactez directement le mainteneur. Un accusé de réception est visé sous 72 heures.

## Mesures en place

- **Authentification** par session (cookie `SESSION` HttpOnly), mots de passe hachés en **BCrypt**, régénération de l'identifiant de session au login (anti-fixation).
- **CSRF** : cookie `XSRF-TOKEN` / en-tête `X-XSRF-TOKEN` (double-submit), requis sur toute mutation.
- **Autorisation** en défense de profondeur : au niveau des routes (rôles), par ressource (garde anti-IDOR côté validation), et re-vérifiée dans le domaine.
- **Injection SQL** rendue impossible par construction (requêtes paramétrées via JPA).
- **Fuite d'information** : erreurs uniformisées en `problem+json`, aucun détail interne ni trace exposé sur les erreurs serveur.
- **Secrets** externalisés (variables d'environnement), jamais dans le code.

## Comptes de démonstration

Les comptes seed (`operator`, `validator`) sont destinés **au développement et à la démonstration uniquement**. Un déploiement réel doit les remplacer par des comptes dédiés et des secrets forts.
