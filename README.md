# EOSA

Site de la marque EOSA, bijoux en pierres naturelles faits main à Nancy : une boutique en ligne publique et une administration séparée. Application Java 25 (Javalin, Mustache, PostgreSQL).

Ce fichier est l'index de la documentation : chaque document de [`docs/`](docs/) y est référencé.

## Démarrer en local

```bash
docker compose up --build -d
```

- Boutique : http://localhost:8080
- Administration : http://localhost:18080 (`admin` / `admin` par défaut)
- E-mails capturés (Mailpit) : http://localhost:8025

Build et tests : `./mvnw clean verify` (Java 25).

## Documentation

### La marque

| Document | Contenu |
|---|---|
| [Notre histoire](docs/brand-story.md) | L'histoire de la marque, texte de la page « Notre histoire » |
| [Les ateliers](docs/self-made-workshop.md) | Présentation des ateliers de création, texte de la page « Ateliers » |

### Spécifications fonctionnelles

| Document | Contenu |
|---|---|
| [Bijoux et personnalisation](docs/jewel-customization-design.md) | Renommage `Product` → `Jewel`, matériaux, breloques, parcours de personnalisation |
| [Réservation des ateliers](docs/workshop-booking-spec.md) | Lieux, sessions, réservation confirmée par e-mail, liste d'attente, rappels, absences, tâches planifiées, sujets juridiques |

### Exploitation

| Document | Contenu |
|---|---|
| [Déploiement](docs/deployment.md) | Image Docker et référence complète des variables d'environnement |
| [`.env.example`](.env.example) | Modèle de configuration locale, prêt à copier |
| [`kubernetes/`](kubernetes/) | Manifestes de production : application, PostgreSQL, tâches planifiées des ateliers |

### Développement

| Document | Contenu |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Architecture, conventions, routes, variables d'environnement : guide pour contribuer au code (humain ou agent) |

## Ajouter un document

Chaque nouveau document de `docs/` est ajouté dans la bonne section ci-dessus, avec une ligne qui dit ce qu'il contient.
