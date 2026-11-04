# Architecture

EOSA est une application Java 25 sans framework d'injection : `Bootstrap.java` lit la configuration, construit toutes les dépendances à la main et démarre deux serveurs Javalin (boutique publique et administration). Le code est découpé en couches : domaine, services, cas d'usage, infrastructure.

## Les règles en bref

1. **Seuls les cas d'usage injectent des providers.** Ni un service, ni un handler, ni un objet du domaine n'en reçoit.
2. **Un cas d'usage n'injecte jamais un autre cas d'usage.** Il reçoit des providers, une `Clock` et de la configuration, rien d'autre.
3. **Un cas d'usage rend un `Result`**, un record imbriqué qui porte tout ce dont l'appelant a besoin.
4. **Une règle qui croise deux domaines ou plus est un service statique** : une fonction qui reçoit des objets déjà lus et rend un résultat, sans provider ni entrée-sortie.
5. **Un handler injecte un seul cas d'usage**, l'appelle une fois et traduit son `Result` en réponse HTTP.
6. **Une page qui a besoin d'informations d'un autre cas d'usage les demande par htmx** : le navigateur fait des appels complémentaires, chacun servi par son propre handler.

## Couches et packages

```
me.noynto.eosa
├── Bootstrap.java          composition root : configuration, câblage, tâches one-shot, routes
├── jewel/ charm/ metal/    domaine : classes métier + interfaces XProvider
│   cart/ checkout/ image/
│   identity/ hash/
│   workshop/ mail/
├── shared/                 identifiants (records : JewelId, WorkshopSessionId…)
├── service/                services statiques : règles qui croisent deux domaines ou plus
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

Les dépendances vont de l'infrastructure vers les cas d'usage, des cas d'usage vers les services et le domaine, jamais l'inverse : le domaine ne connaît ni JDBC, ni Javalin, ni Stripe.

```
handler (infrastructure/web)  ──▶  cas d'usage  ──▶  services statiques ──▶  domaine
                                        │                                       ▲
                                        └──▶  providers (interfaces)  ──────────┘
                                                    ▲
                                          JdbcPersistedX, SmtpSentMails… (infrastructure)
```

## Domaine

- Les objets métier sont des classes avec getters et setters (`Jewel`, `WorkshopSession`, `WorkshopBooking`…) ; les identifiants et les petites valeurs sont des records (`shared/`).
- Une règle propre à l'objet peut y vivre comme méthode (`WorkshopSession.getBookingOpensAt()`, `WorkshopBooking.occupiesAPlace(now)`), de même que ses constantes métier (`WorkshopBooking.CONFIRMATION_DELAY`).
- Chaque agrégat a une interface `XProvider` limitée à de la lecture et de l'écriture : `readIds(filtres…)`, `read(id)`, `write(objet)` et, si besoin, `delete(id)`. Les filtres de `readIds` sont des paramètres, `null` voulant dire « pas de filtre » (comme `JewelProvider.readIds(states, categories)`).
- Aucune règle métier dans les providers : pas de `reserve`, `countX` ou `expireX`. La capacité, les délais ou l'envoi d'e-mails se décident dans les cas d'usage.

## Cas d'usage (`application/`)

- Un cas d'usage = un record nommé par l'action (`CreateWorkshopVenue`, `RequestWorkshopBooking`), avec une méthode `handle(Command)` et un record `Command` imbriqué.
- Il reçoit ses dépendances par son constructeur : providers, `Clock` (jamais `Instant.now()` direct, pour les tests), adresse publique du site si besoin.
- Il vérifie la commande puis applique les règles. Un refus lève une `RuntimeException` avec un message en français, prêt à être montré à l'utilisateur.
- **Il est le seul à parler aux providers** : il lit, applique les règles, écrit. Ce qui est lu est passé aux services et aux méthodes des objets métier ; ce qui est écrit l'est par lui.
- **Il n'injecte jamais un autre cas d'usage.** Une étape qui sert à plusieurs cas d'usage (prévenir la liste d'attente, envoyer un e-mail) se réécrit dans chacun avec les providers dont il a besoin (`MailProvider`, les providers de la liste d'attente) ; seul le calcul qui croise deux domaines ou plus se partage, dans un [service statique](#services-statiques-service). Un peu de répétition entre cas d'usage est voulue : chaque cas d'usage se lit seul, de haut en bas.
- Un cas d'usage de lecture prend une `Query` à la place de la `Command`.

### Result

`handle` rend le record `Result` imbriqué dans le cas d'usage, jamais une entité du domaine seule, un `Optional` ni un type partagé :

```java
public record ReadSitemap(JewelProvider jewelProvider, WorkshopSessionProvider workshopSessionProvider, Clock clock) {

    public Result handle() { … }

    public record Result(List<JewelId> jewelIds, List<WorkshopSessionId> workshopSessionIds) {
    }
}
```

- Le `Result` porte tout ce dont l'appelant a besoin pour répondre (objets du domaine, compteurs, un statut) : le handler n'appelle jamais un deuxième cas d'usage pour le compléter.
- Un statut qui décide de la réponse est une énumération dans le `Result` : `Result(Outcome outcome)` avec `CONFIRMED`, `ALREADY_CONFIRMED`, `EXPIRED`… Un refus de règle métier reste une `RuntimeException`, pas un statut.
- Une action qui n'a rien à rendre (un envoi, une suppression) ne retourne rien : pas de `Result` vide.
- Une tâche planifiée ([tâches planifiées](scheduled-tasks.md)) lit le `Result` du cas d'usage pour écrire son compte rendu dans les journaux et décider de son code de sortie (`RetryWorkshopEmailsTask`).

## Services statiques (`service/`)

Un service est le calcul d'une règle qui concerne deux domaines ou plus, quand aucun des objets n'en est le propriétaire naturel. Par exemple : combien de places reste-t-il (une session et ses réservations), ou quel contenu d'e-mail pour une réservation (la réservation, sa session et son lieu).

- Une classe `final` avec un constructeur privé et des méthodes `static`.
- Les méthodes **reçoivent des objets du domaine déjà lus** et rendent une valeur ou un record. Elles n'injectent **aucun provider**, n'appellent aucun cas d'usage, ne lisent ni l'horloge (l'instant est un paramètre `Instant now`), ni l'environnement, ni la base, et ne gardent aucun état.
- Seul un cas d'usage les appelle : il lit avec ses providers, passe les objets au service, puis écrit.
- Se testent sans doublure (pas de Mockito) : des objets en entrée, un résultat en sortie.
- Une règle qui ne concerne qu'**un seul domaine** n'est pas un service : c'est une méthode de l'objet métier (`WorkshopSession.getBookingOpensAt()`), et ses constantes y vivent aussi.

Un service qui aurait besoin de lire ou d'écrire quelque chose n'est plus un service : c'est du code de cas d'usage.

## Handlers (`infrastructure/web/`)

- Un handler reçoit **un seul cas d'usage**, l'appelle **une seule fois**, et ne fait que traduire : lire la requête (paramètre d'URL, formulaire, cookie) vers la `Command`, puis le `Result` vers la réponse (page, fragment, redirection, en-tête htmx).
- Aucune règle métier, aucun accès à un provider.
- Un refus (`RuntimeException`) devient le message affiché : `InlineError` pour un fragment htmx, la page réaffichée avec l'erreur pour un formulaire public (voir [sécurité](security.md)).
- Un handler qui aurait besoin d'un deuxième cas d'usage ne l'appelle pas : c'est la page qui demande ce complément par htmx (ci-dessous), ou le cas d'usage qui le lit lui-même avec ses providers.

## Pages composées avec htmx

Quand une page affiche plusieurs informations qui viennent de cas d'usage différents, le **handler de la page ne sert que l'essentiel** (un cas d'usage), et le navigateur va chercher le reste par des appels complémentaires, un handler et un cas d'usage par appel.

- L'essentiel : tout ce qu'il faut pour lire la page sans JavaScript et pour les moteurs de recherche, c'est-à-dire le contenu principal, le titre, la description et les données structurées (`jsonLd`, voir [SEO](seo.md)). Ce qui n'est pas dans la première réponse n'est pas vu par un moteur de recherche ni par une personne sans JavaScript.
- Le complément : une information secondaire ou qui change (places restantes, autres bijoux, options de personnalisation). Un emplacement `hx-get="…" hx-trigger="load"` avec un squelette `animate-pulse` à la forme du contenu, qui se remplace par le fragment rendu par un template de `partials/`.
- C'est déjà le motif des cartes : `GET /jewels/{id}/card` et `GET /workshops/sessions/{id}/card` (qui se recharge d'elle-même toutes les 30 s).
- Une erreur sur un complément ne casse pas la page : le fragment est vide ou porte un court message, jamais un statut qui remplace la page.
- Un formulaire n'a pas besoin de réafficher la page pour répondre : l'erreur s'affiche sous lui et la confirmation le remplace, par un seul cas d'usage ([web](web.md#htmx)).

## État actuel et mise en conformité

Relevé du 4 novembre 2026, mis à jour au fil des corrections (une pull request par sujet).

**Cas d'usage qui en injectent un autre** : aucun. Les annulations (`CancelWorkshopBookingByAdmin`, `CancelWorkshopBookingByParticipant`, `CancelWorkshopSession`), `ConfirmWorkshopBooking`, `ExpirePendingWorkshopBookings`, `UpdateWorkshopSession` et `RetryWorkshopEmails` lisent, envoient et écrivent eux-mêmes ; le contenu des e-mails vient de `WorkshopMails` et le compte des places de `WorkshopPlaces` (`service/`). `ReadCategoryStats` lit avec `JewelProvider` et prend plusieurs catégories dans une seule `Query` ; `ReadWorkshopSession` lit la session, son lieu, ses réservations et sa liste d'attente avec ses propres providers.

**Handlers qui injectent plusieurs cas d'usage** : aucun. Ce qui a été corrigé : l'accueil (`GetIndexHandler` appelle `ReadCategoryStats` une fois ; les derniers bijoux se chargent par `GET /jewels/latest`), la fiche bijou (`GetJewelHandler` n'appelle que `ReadJewel` ; les autres bijoux, les couleurs de métal et les breloques se chargent par `GET /jewels/{id}/related`, `/customization/metal-colors` et `/customization/charms`, chacun avec son handler et son cas d'usage), l'administration des sessions, le formulaire de réservation publique et le panier. Dans l'administration, la page d'une session n'appelle que `ReadWorkshopSession` ; les inscrites (`GET /workshops/{id}/bookings`, avec `ReadWorkshopBookings` qui rend aussi la session), la liste d'attente (`GET /workshops/{id}/waitlist`) et les lieux proposés (`GET /workshop-venues/options`) se chargent à part. La liste des sessions n'appelle que `ReadWorkshopSessions` ; le formulaire « Nouvelle session » se charge à chaque ouverture de la fenêtre (`GET /workshops/new`). Les formulaires de réservation et de liste d'attente d'une session s'envoient en htmx : `PostWorkshopBookingHandler` et `PostWorkshopWaitlistHandler` n'appellent que `RequestWorkshopBooking` et `JoinWorkshopWaitlist`, sans réafficher la page. Le filtre `EnsureCartHandler` pose le panier dans l'attribut `cartId` de la requête, que `GetCartHandler` (`ReadCart`) et `PostCartItemHandler` (`AddJewelToCart`) lisent : ils ne créent plus un deuxième panier, qui restait vide en base à la première visite.

**Autres écarts :**

- Seuls quatre cas d'usage ont un `Result` (`ReadSitemap`, `PurgeWorkshopPersonalData`, `RetryWorkshopEmails`, `SendWorkshopReminders`) ; les autres rendent une entité, un record à un autre nom (`Overview`, `Outcome`) ou rien.
- `WorkshopSessionRules` (`application/`) ne concerne que le domaine des sessions : ses contrôles de capacité et de durée vont dans l'objet métier.

Reste : la généralisation du `Result` aux autres cas d'usage, puis les contrôles de `WorkshopSessionRules` dans l'objet métier. Chaque étape garde les tests au vert.

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
