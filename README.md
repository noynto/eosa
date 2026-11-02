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

Build et tests : `./mvnw clean verify` (Java 25). Détails dans [Développement](docs/tech/development.md).

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

### Technique — [`docs/tech/`](docs/tech/)

| Document | Contenu |
|---|---|
| [Architecture](docs/tech/architecture.md) | Composition root, couches domaine / cas d'usage / infrastructure, providers, conventions de nommage, persistance |
| [Web](docs/tech/web.md) | Les deux serveurs, routes, templates Mustache, htmx, liens envoyés par e-mail, responsive |
| [E-mails](docs/tech/emails.md) | Envoi SMTP, écriture des templates compatibles, tests avec Mailpit, délivrabilité |
| [Tâches planifiées](docs/tech/scheduled-tasks.md) | Tâches one-shot, CronJobs Kubernetes, jobs compose, ajouter une tâche |
| [Développement](docs/tech/development.md) | Build, tests, environnement local, conventions, Git et livraison |

### Exploitation — [`docs/ops/`](docs/ops/)

| Document | Contenu |
|---|---|
| [Déploiement](docs/ops/deployment.md) | Image Docker et référence complète des variables d'environnement |
| [`.env.example`](.env.example) | Modèle de configuration locale, prêt à copier |
| [`kubernetes/`](kubernetes/) | Manifestes de production : application, PostgreSQL, tâches planifiées des ateliers |

### Pilotage

| Document | Contenu |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Vue d'ensemble et règles incontournables pour contribuer au code (humain ou agent) |
| [TODO](TODO.md) | Sujets à traiter : design system, SEO, architecture, sécurité, conventions |

## Ajouter un document

Chaque nouveau document va dans le dossier de son thème (`brand/`, `specs/`, `tech/`, `ops/`) et est ajouté ici, dans la bonne section, avec une ligne qui dit ce qu'il contient.
