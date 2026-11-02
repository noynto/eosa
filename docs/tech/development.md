# Développement

Prérequis : Java 25 et Docker. Toutes les commandes se lancent à la racine du dépôt (là où se trouve `pom.xml`).

## Construire et tester

```bash
./mvnw clean verify                 # build complet + tous les tests
./mvnw test -Dtest=NomDeLaClasse    # une seule classe de test
```

Toujours `clean` après une modification de constructeur ou de signature : la compilation incrémentale ne recompile pas les classes de test inchangées et peut annoncer un succès alors que les tests ne compilent plus.

## Environnement local

```bash
docker compose up --build -d        # PostgreSQL, création de l'administratrice, application, Mailpit
docker compose down -v              # tout arrêter et repartir d'une base vide
```

| Service | Adresse |
|---|---|
| Boutique | http://localhost:8080 |
| Administration | http://localhost:18080 (`admin` / `admin` par défaut) |
| E-mails capturés (Mailpit) | http://localhost:8025 |
| PostgreSQL | `127.0.0.1:5432` (base `eosa`) |

Aucune configuration n'est nécessaire ; une valeur se surcharge dans un fichier `.env` (modèle : [`.env.example`](../../.env.example)). Les tâches planifiées se lancent à la main : voir [tâches planifiées](scheduled-tasks.md). Les requêtes HTTP de test sont dans `requests/`.

## Tests

- Chaque cas d'usage a sa classe de test dans `src/test/java/.../application/` : JUnit 6 et Mockito.
- Le temps est fixé par une `Clock` (`Clock.fixed(...)`), jamais l'horloge réelle.
- Quand un provider est interrogé avec des filtres, un provider en mémoire (`InMemoryWorkshopBookings`, `InMemoryWorkshopWaitlistEntries`) remplace les mocks : mêmes filtres et même ordre que la version JDBC.
- Une interface se vérifie en vrai, en plus des tests : dans le navigateur aux trois tailles (voir [web](web.md)), et les e-mails dans Mailpit.

## Conventions de code

- Noms : interfaces du domaine `XProvider` ; implémentations `JdbcPersistedX`, `<Service>FetchedX`, `SmtpSentMails`, `SecuredX` ; cas d'usage nommés par l'action. Détails dans [architecture](architecture.md).
- Messages d'erreur et textes visibles en français ; code, noms de classes et commits en anglais.
- Les fichiers se modifient avec un éditeur ou l'outil d'édition, pas par des scripts `sed`/`python` lancés dans le terminal.

## Git et livraison

- Une branche par sujet depuis `main`, puis une pull request.
- Messages de commit au format Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…) : semantic-release en déduit le numéro de version.
- CI (`.github/workflows/ci.yml`) : `mvn verify` sur chaque push et PR vers `main`.
- Sécurité (`security.yml`) : scan Trivy des dépendances ; une vulnérabilité HIGH ou CRITICAL bloque la PR.
- Release (`release.yml`) : à chaque push sur `main`, semantic-release crée la version, puis l'image `ghcr.io/noynto/eosa:<version>` est construite et publiée. Le déploiement est décrit dans [déploiement](../ops/deployment.md).

## Documentation

Chaque document de `docs/` est référencé dans le [README](../../README.md). En ajouter, renommer ou supprimer un met à jour le README dans le même changement.
