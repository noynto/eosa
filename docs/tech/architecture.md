# Architecture

EOSA est une application Java 25 sans framework d'injection : `Bootstrap.java` lit la configuration, construit toutes les dépendances à la main et démarre deux serveurs Javalin (boutique publique et administration). Le code est découpé en trois couches : domaine, cas d'usage, infrastructure.

## Couches et packages

```
me.noynto.eosa
├── Bootstrap.java          composition root : configuration, câblage, tâches one-shot, routes
├── jewel/ charm/ metal/    domaine : classes métier + interfaces XProvider
│   cart/ checkout/ image/
│   identity/ hash/
│   workshop/ mail/
├── shared/                 identifiants (records : JewelId, WorkshopSessionId…)
├── application/            cas d'usage, un par commande
├── task/                   tâches one-shot lancées par Bootstrap puis arrêt (CronJobs)
└── infrastructure/
    ├── persistence/        JdbcPersistedX : implémentations JDBC des providers
    │   └── jdbc/           JdbcConfiguration : pool HikariCP + migrations Flyway
    ├── fetch/stripe/       StripeFetchedCheckouts : appels à l'API Stripe
    ├── mail/               SmtpSentMails + MailTemplates (voir emails.md)
    ├── security/           SecuredCrypts (BCrypt)
    └── web/                handlers Javalin
```

Les dépendances vont de l'infrastructure vers le domaine, jamais l'inverse : le domaine ne connaît ni JDBC, ni Javalin, ni Stripe.

## Domaine

- Les objets métier sont des classes avec getters et setters (`Jewel`, `WorkshopSession`, `WorkshopBooking`…) ; les identifiants et les petites valeurs sont des records (`shared/`).
- Une règle propre à l'objet peut y vivre comme méthode (`WorkshopSession.getBookingOpensAt()`, `WorkshopBooking.occupiesAPlace(now)`), de même que ses constantes métier (`WorkshopBooking.CONFIRMATION_DELAY`).
- Chaque agrégat a une interface `XProvider` limitée à de la lecture et de l'écriture : `readIds(filtres…)`, `read(id)`, `write(objet)` et, si besoin, `delete(id)`. Les filtres de `readIds` sont des paramètres, `null` voulant dire « pas de filtre » (comme `JewelProvider.readIds(states, categories)`).
- Aucune règle métier dans les providers : pas de `reserve`, `countX` ou `expireX`. La capacité, les délais ou l'envoi d'e-mails se décident dans les cas d'usage.

## Cas d'usage (`application/`)

- Un cas d'usage = un record nommé par l'action (`CreateWorkshopVenue`, `RequestWorkshopBooking`), avec une méthode `handle(Command)` et un record `Command` imbriqué.
- Il reçoit ses dépendances par son constructeur : providers, `Clock` (jamais `Instant.now()` direct, pour les tests), adresse publique du site si besoin.
- Il vérifie la commande puis applique les règles. Un refus lève une `RuntimeException` avec un message en français, prêt à être montré à l'utilisateur.
- La logique calculée à partir des providers (places occupées, par exemple) est écrite dans chaque cas d'usage qui en a besoin, sans classe utilitaire partagée.
- Un résultat composé est un record imbriqué (`ReadWorkshopSessions.Overview`, `RetryWorkshopEmails.Result`, `ConfirmWorkshopBooking.Outcome`).

État actuel à faire évoluer (voir la [TODO](../../TODO.md)) : certains cas d'usage en appellent d'autres (`ConfirmWorkshopBooking` appelle `SendWorkshopBookingConfirmation`) et certains handlers reçoivent plusieurs cas d'usage. Les règles cibles (un cas d'usage n'en injecte pas d'autre, un handler injecte un seul cas d'usage, services statiques entre domaines) seront détaillées ici quand elles seront appliquées.

## Infrastructure

| Rôle | Convention de nom | Exemples |
|---|---|---|
| Persistance PostgreSQL | `JdbcPersistedX` | `JdbcPersistedWorkshopBookings`, `JdbcPersistedCharms` |
| Appel d'une API externe | `<Service>FetchedX` | `StripeFetchedCheckouts` |
| Envoi d'e-mails | `SmtpSentMails` | — |
| Sécurité | `SecuredX` | `SecuredCrypts` |
| Handler web | `Get…Handler`, `Post…Handler`, `<Action>Handler` | `GetWorkshopSessionHandler`, `CancelWorkshopBookingByAdminHandler` |

## Persistance

- JDBC brut, sans ORM : une requête SQL par méthode, écriture par `INSERT … ON CONFLICT (id) DO UPDATE` (upsert), identifiants `UUID` générés à l'écriture.
- Le schéma évolue par migrations Flyway (`src/main/resources/db/migration/Vn__description.sql`), appliquées automatiquement au démarrage de l'application et de chaque tâche. Une migration déjà déployée ne se modifie jamais : on en ajoute une nouvelle.
- Les dates sont des `Instant` côté Java et des `timestamptz` en base, stockées en UTC. Les règles calendaires des ateliers (ouverture à J-7, 48 h, rappel la veille) se calculent dans le fuseau `Europe/Paris` (`WorkshopSession.ZONE`), et les dates s'affichent via `WorkshopDates`.

## Tâches planifiées

Les traitements périodiques ne tournent pas dans l'application : ce sont des tâches one-shot lancées par des CronJobs Kubernetes. Voir [tâches planifiées](scheduled-tasks.md).
