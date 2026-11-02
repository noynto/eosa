# Tâches planifiées

L'application n'a pas de planificateur interne. Un traitement périodique est une tâche one-shot : une variable d'environnement l'active, `Bootstrap` câble les dépendances, exécute la tâche puis s'arrête avec le code 0 (succès) ou 1 (échec), sans démarrer les serveurs. En production, des CronJobs Kubernetes lancent l'image avec cette variable ; en local, des services compose du profil `jobs`.

## Liste des tâches

| Tâche (`task/`) | Variable d'activation | Planification | Rôle |
|---|---|---|---|
| `CreateDefaultAdministratorIdentityTask` | `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK` | Au déploiement (`initContainer`) | Crée l'administratrice par défaut si elle n'existe pas |
| `SendTestEmailTask` | `EOSA_SEND_TEST_EMAIL_TO` (adresse) | À la demande | Envoie un e-mail de test |
| `AnnounceWorkshopBookingOpeningsTask` | `EOSA_ANNOUNCE_WORKSHOP_BOOKING_OPENINGS_TASK` | Chaque minute | Prévient la liste d'attente à l'ouverture des réservations |
| `ExpirePendingWorkshopBookingsTask` | `EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK` | Toutes les 5 min | Expire les demandes non confirmées et prévient la liste d'attente |
| `SendWorkshopRemindersTask` | `EOSA_SEND_WORKSHOP_REMINDERS_TASK` | Toutes les 15 min | Envoie le rappel de la veille |
| `RetryWorkshopEmailsTask` | `EOSA_RETRY_WORKSHOP_EMAILS_TASK` | Toutes les 15 min | Renvoie les e-mails de réservation qui ont échoué |
| `PurgeWorkshopPersonalDataTask` | `EOSA_PURGE_WORKSHOP_PERSONAL_DATA_TASK` | Chaque nuit à 3 h 30 | Supprime les données personnelles au terme des durées de conservation |

Les variables d'activation ne sont jamais définies dans le `Deployment`, seulement dans leur CronJob (ou leur service compose).

## En local

```bash
docker compose run --rm send-test-email
docker compose run --rm announce-workshop-booking-openings
docker compose run --rm expire-pending-workshop-bookings
docker compose run --rm send-workshop-reminders
docker compose run --rm retry-workshop-emails
docker compose run --rm purge-workshop-personal-data
```

## En production

Les CronJobs sont dans `kubernetes/workshop-cronjobs.yaml` (inclus dans la kustomization) : même image et mêmes variables que le `Deployment`, fuseau `Europe/Paris`, `concurrencyPolicy: Forbid`, historique des échecs conservé. Pour lancer une tâche à la main et lire son résultat :

```bash
kubectl -n eosa create job --from=cronjob/<nom> <nom>-manuel
kubectl -n eosa logs job/<nom>-manuel
kubectl -n eosa delete job <nom>-manuel
```

## Règles d'écriture

- Une tâche ne fait qu'appeler un cas d'usage et journaliser son résultat ; la logique et les tests sont dans le cas d'usage.
- Une tâche est idempotente : relancée ou exécutée deux fois, elle ne renvoie pas d'e-mail et ne modifie rien deux fois (colonnes `…_sent_at`, `opening_announced_at`, états).
- Le fonctionnement du site ne dépend pas de la cadence : par exemple, une demande non confirmée cesse d'occuper sa place au bout de 30 min, même si la tâche d'expiration passe plus tard.
- Chaque exécution applique aussi les migrations Flyway au démarrage : les CronJobs utilisent la même version d'image que le `Deployment`.

## Ajouter une tâche

1. Le cas d'usage dans `application/`, avec ses tests.
2. La classe `XTask` dans `task/` (sur le modèle de `SendWorkshopRemindersTask`) et son bloc dans la partie `// TASK` de `Bootstrap`.
3. Le service compose (profil `jobs`) et le CronJob dans `kubernetes/workshop-cronjobs.yaml`.
4. Une ligne dans le tableau ci-dessus.
