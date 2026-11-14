# EOSA

Site de la marque EOSA, bijoux en pierres naturelles faits main à Nancy : une boutique en ligne publique (bijoux, panier, paiement Stripe, réservation des ateliers) et une administration séparée. Application Java 25 (Javalin, Mustache, htmx, PostgreSQL).

Ce fichier est le sommaire de la documentation : chaque document de [`docs/`](docs/) y est référencé.

## Démarrer en local

```bash
docker compose up --build -d
```

- Boutique : http://localhost:8080
- Administration : http://localhost:18080 (`admin` / `admin` par défaut)
- E-mails capturés (Mailpit) : http://localhost:8025

## Commandes

```bash
./mvnw clean verify               # build + tests (toujours clean : le build incrémental rate les tests périmés)
./mvnw test -Dtest=NomDeLaClasse  # une seule classe de test
docker compose run --rm <job>     # une tâche planifiée à la main (voir Tâches planifiées)
docker compose down -v            # repartir d'une base vide
```

Détails dans [Développement](docs/tech/development.md).

## Pile technique

| Couche | Technologie |
|---|---|
| Web | Javalin 7.2.3 : deux serveurs, boutique `8080` et administration `18080` |
| Templates | Mustache (`javalin-rendering-mustache`) + Tailwind + htmx |
| Persistance | PostgreSQL 17, JDBC brut (sans ORM), HikariCP 7, migrations Flyway 11 |
| E-mails | Jakarta Mail (Angus 2.0.3), SMTP Infomaniak, Mailpit en local |
| Paiement | Stripe Checkout (client HTTP, Gson) |
| Mots de passe | jBCrypt 0.4 |
| Tests | JUnit Jupiter 6.1.0 + Mockito 5.18.0 |
| Exécution | Java 25, image Docker distroless, Kubernetes (Deployment + CronJobs) |

## Règles qui s'appliquent toujours

- **Architecture** ([détails](docs/tech/architecture.md)) : `Bootstrap.java` est le seul point de câblage (pas de framework d'injection). Les packages du domaine contiennent les classes métier et des interfaces `XProvider` limitées à `readIds(filtres…)`, `read`, `write`, `delete`. Les règles métier sont dans les cas d'usage (`application/`, un record par action avec un `Command` imbriqué et un `Result` imbriqué), jamais dans les providers. Seuls les cas d'usage injectent des providers ; un cas d'usage n'injecte jamais un autre cas d'usage ; une règle qui croise deux domaines ou plus est un service statique (`service/`, sans provider) ; un handler injecte un seul cas d'usage et le reste d'une page se charge par des appels htmx complémentaires. Les cas d'usage reçoivent une `Clock` et n'appellent jamais `Instant.now()`.- **Nommage** : interfaces `XProvider` ; implémentations `JdbcPersistedX`, `<Service>FetchedX`, `SmtpSentMails`, `SecuredX`.
- **Persistance** : le schéma ne change que par une nouvelle migration Flyway (`Vn__description.sql`), jamais en modifiant une migration déjà déployée ; dates en `Instant` / `timestamptz` UTC, règles des ateliers dans le fuseau `Europe/Paris`.
- **Templates** ([détails](docs/tech/web.md)) : modèles de vue en `Map<String, Object>` construites à la main ; Mustache n'a pas de logique, tout se calcule en Java ; un `{{> partial}}` se résout depuis le dossier du template qui l'inclut.
- **Sécurité** ([détails](docs/tech/security.md)) : SQL uniquement en `PreparedStatement` avec des paramètres `?` ; aucun HTML concaténé en Java (un fragment se rend avec Mustache, une erreur htmx avec `InlineError`) ; pas de `{{{…}}}` dans un template HTML (sauf `{{{jsonLd}}}`) ; une action qui modifie des données n'est jamais un `GET` ; `SecureRequestHandler` refuse les requêtes venues d'un autre site.
- **Liens envoyés par e-mail** : ils n'agissent que sur un `POST` (les messageries ouvrent les liens d'elles-mêmes) et portent un jeton aléatoire, jamais un identifiant.
- **Templates d'e-mail** ([détails](docs/tech/emails.md)) : mise en page en tableaux, padding sur les `<td>` seulement, attributs `bgcolor`, ni margin, ni border-radius, ni font-weight ; partials appelés par leur nom de fichier complet.
- **Traitements périodiques** ([détails](docs/tech/scheduled-tasks.md)) : des tâches one-shot activées par une variable d'environnement et lancées par des CronJobs Kubernetes, jamais un planificateur dans l'application ; une tâche est idempotente.
- **Design system** ([fondations](docs/design/foundations.md)) : méthode Material 3, sans composant JavaScript ; uniquement les jetons de `tailwind.config.js` (aucune couleur, taille ou arrondi arbitraire) ; contrastes WCAG AA, focus clavier toujours visible, cibles tactiles de 44 px minimum. La boutique reste éditoriale et sobre ([boutique](docs/design/storefront.md)), l'administration suit les composants Material ([administration](docs/design/admin.md)).
- **Responsive** : toute modification de template gère mobile (< 640 px, minimum 390 px), tablette (640–1024 px) et ordinateur (≥ 1024 px) ; le parcours principal se vérifie à 390, 768 et 1280 px.
- **Commits** : Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…), une branche et une pull request par sujet ; semantic-release crée la version et publie l'image depuis `main`.
- **Configuration** : les variables d'environnement sont documentées dans `.env.example` et le [déploiement](docs/ops/deployment.md) ; une nouvelle variable obligatoire s'ajoute aussi à `compose.yaml` et aux manifestes Kubernetes.
- **Documentation** : chaque document de `docs/` est référencé ci-dessous ; en ajouter, renommer ou supprimer un met à jour ce README dans le même changement.

## Documentation

### La marque — [`docs/brand/`](docs/brand/)

| Document | Contenu |
|---|---|
| [Notre histoire](docs/brand/brand-story.md) | L'histoire de la marque, texte de la page « Notre histoire » |
| [Présentation des ateliers](docs/brand/workshops-presentation.md) | Les ateliers de création, texte de la page « Ateliers » |
| [`images/`](docs/brand/images/) | Photos de colliers |

### Spécifications fonctionnelles — [`docs/specs/`](docs/specs/)

| Document | Contenu |
|---|---|
| [Bijoux et personnalisation](docs/specs/jewel-customization.md) | Renommage `Product` → `Jewel`, matériaux, breloques, parcours de personnalisation |
| [Réservation des ateliers](docs/specs/workshop-booking.md) | Lieux, sessions, réservation confirmée par e-mail, liste d'attente, rappels, absences, sujets juridiques |
| [Commandes](docs/specs/orders.md) | Commande enregistrée au paiement Stripe (sans webhook), administration, expédition et e-mail de suivi, étiquette et suivi Mondial Relay, sujets juridiques, étapes (à valider) |
| [Codes de promotion](docs/specs/promo-codes.md) | Codes gérés par EOSA (pourcentage ou montant), limites, remise envoyée à Stripe sous forme de coupon, enregistrée sur la commande, étapes (à valider) |

### Design — [`docs/design/`](docs/design/)

| Document | Contenu |
|---|---|
| [Fondations](docs/design/foundations.md) | Jetons communs selon Material 3 : couleurs et contrastes, typographie, arrondis, élévation, mouvement, états, accessibilité, écarts à corriger |
| [Boutique](docs/design/storefront.md) | Application éditoriale et sobre à la vitrine : ce qu'on prend à Material, motifs de la boutique |
| [Administration](docs/design/admin.md) | Composants Material 3 et leur équivalent en Tailwind + htmx, densité, cibles tactiles |

### Technique — [`docs/tech/`](docs/tech/)

| Document | Contenu |
|---|---|
| [Architecture](docs/tech/architecture.md) | Règles en bref, composition root, domaine, cas d'usage et `Result`, services statiques, handlers, pages composées avec htmx, état du code, infrastructure, persistance |
| [Web](docs/tech/web.md) | Les deux serveurs, routes, templates Mustache, htmx, liens envoyés par e-mail, responsive |
| [Sécurité](docs/tech/security.md) | Injections SQL et JavaScript, requêtes forgées (CSRF), en-têtes de sécurité, règles à suivre, points restants |
| [SEO](docs/tech/seo.md) | Référencement en place (sitemap, données structurées), règles d'une page publique, actions pour amener du trafic |
| [E-mails](docs/tech/emails.md) | Envoi SMTP, écriture des templates compatibles, tests avec Mailpit, délivrabilité |
| [Tâches planifiées](docs/tech/scheduled-tasks.md) | Tâches one-shot, CronJobs Kubernetes, jobs compose, ajouter une tâche |
| [Développement](docs/tech/development.md) | Build, tests, environnement local, conventions, Git et livraison |

### Exploitation — [`docs/ops/`](docs/ops/)

| Document | Contenu |
|---|---|
| [Déploiement](docs/ops/deployment.md) | Image Docker et référence complète des variables d'environnement |
| [`.env.example`](.env.example) | Modèle de configuration locale, prêt à copier |
| [`kubernetes/`](kubernetes/) | Manifestes de production : application, PostgreSQL, tâches planifiées des ateliers et des commandes |

### Pilotage

| Document | Contenu |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Point d'entrée de Claude Code : il importe ce README, sans contenu propre |
| [TODO](TODO.md) | Sujets à traiter : design system, SEO, architecture, sécurité, conventions |

## Ajouter un document

Chaque nouveau document va dans le dossier de son thème (`brand/`, `specs/`, `design/`, `tech/`, `ops/`) et est ajouté ici, dans la bonne section, avec une ligne qui dit ce qu'il contient.
