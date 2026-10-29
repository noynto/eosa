# Spécifications : réservation des ateliers

## Contexte

Aujourd'hui, la page `/workshops` présente l'atelier (8 personnes max, collier 33 €, bracelet 23 €), mais la réservation se fait uniquement par message privé sur Instagram/TikTok : aucune date n'est publiée et aucune place n'est suivie dans l'application.

Les ateliers ont lieu chez des partenaires, en particulier des **cafés avec lesquels EOSA collabore**. Le lieu fait donc partie de l'attrait de chaque session et doit être mis en avant.

L'objectif est de permettre à une participante de réserver elle-même sa place sur une session datée, depuis le site, et à l'administration de gérer les lieux, les sessions et les inscrites.

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Paiement | Pas de paiement en ligne de l'atelier : il se règle sur place. Une **empreinte bancaire Stripe de 10 €** est prise à la réservation pour garantir la venue. Elle est **rendue** (libérée) après l'atelier. Elle n'est débitée qu'en cas d'absence non excusée : une participante qui n'est **pas arrivée 30 minutes après le début** de la séance est considérée absente, et le débit peut alors être déclenché. |
| 2 | Ouverture des réservations | Les réservations d'une session ouvrent **toujours 7 jours avant** son début, ce qui reste compatible avec la durée de validité d'une empreinte Stripe. Avant l'ouverture, la session est visible avec un **compte à rebours**. Une fois ouverte, un **compteur de places restantes** est affiché. |
| 3 | Type de bijou | La participante **ne choisit pas** collier ou bracelet à la réservation : le choix se fait sur place. |
| 4 | Nombre de places | **Une réservation = une place.** Chaque participante réserve la sienne (pas de réservation groupée). |
| 5 | Lieux | Le lieu est une entité à part (`WorkshopVenue`), réutilisable d'une session à l'autre et **mis en avant** partout : nom du café, photo, adresse, lien vers le partenaire. |
| 6 | Administration | Gérer les lieux et les sessions, voir les inscrites, **annuler une session** et **annuler la réservation d'une participante**. |
| 7 | Annulation | La participante peut annuler elle-même **jusqu'à 48 h avant** le début. Au-delà, elle doit contacter EOSA sur Instagram avec un motif valable ; l'admin décide. |
| 8 | Confirmation | Une **page de confirmation** après réservation + un **e-mail récapitulatif**. |
| 9 | E-mails | Envoyés depuis **`contact@eosa.me`** via le **SMTP Infomaniak**. |
| 10 | Parcours | **Séparé du panier** : un atelier ne se met pas dans le panier et n'utilise pas le checkout des bijoux. |
| 11 | Périmètre | Le rappel par e-mail, la liste d'attente, les valeurs par défaut des sessions et la mise à jour des CGV/FAQ font partie **de cette même livraison**. |

## Vocabulaire

| Terme | Rôle |
|---|---|
| `WorkshopVenue` | Lieu partenaire (café, bar, boutique…) : nom, courte présentation, adresse, ville, photo, lien (site ou Instagram), capacité et durée par défaut, actif/archivé. |
| `WorkshopSession` | Session datée : lieu, début, durée, capacité, état. |
| `WorkshopSessionState` | `SCHEDULED` \| `CANCELLED`. L'ouverture (J-7) et la fin des réservations sont calculées à partir de la date de début, sans changement d'état. |
| `WorkshopBooking` | Réservation d'une place par une participante sur une session. |
| `WorkshopBookingState` | `PENDING` (empreinte en cours sur Stripe) \| `CONFIRMED` \| `CANCELLED` \| `EXPIRED` (empreinte jamais finalisée). Après la session : `ATTENDED` \| `NO_SHOW`. |
| `WorkshopWaitlistEntry` | Inscription sur la liste d'attente d'une session complète. |
| Empreinte | Autorisation bancaire de 10 € via Stripe Checkout en capture manuelle (voir « Empreinte Stripe »). |

## Cycle de vie d'une session

```
création (admin)            ouverture = début - 7 j          début - 48 h           début      début + 30 min
     │   « Bientôt »             │   « Réserver »                 │                   │ présente   │ absente possible
     │   compte à rebours        │   compteur de places           │ plus d'annulation │ possible   │ → empreinte débitée
     │                           │   annulation libre             │ en autonomie      │ → empreinte│
     │                           │                                │                   │   libérée  │
─────┴───────────────────────────┴────────────────────────────────┴───────────────────┴────────────┴──────────▶
                                                                    rappel e-mail (veille)
```

- **Avant l'ouverture** : la session est affichée avec le lieu et un compte à rebours (« Ouverture des réservations dans 2 j 5 h », puis l'heure exacte : « lundi 9 novembre à 14 h 00 »). Le bouton est désactivé.
- **Réservations ouvertes** : de début − 7 jours jusqu'au début de la session. Le compteur indique les places restantes (« 5 places restantes », « Dernière place », « Complet »).
- **Complet** : le bouton « Réserver » est remplacé par « Me prévenir si une place se libère » (liste d'attente).
- **Session passée** : elle n'apparaît plus côté public.

L'heure d'ouverture est exactement l'heure de début moins 7 jours, dans le fuseau `Europe/Paris`.

## Parcours participante

### 1. Choisir une session — `GET /workshops`

La page existante est conservée (présentation, déroulé, tarifs). Une section **« Prochains ateliers »** est ajoutée en haut, sous l'introduction. Elle liste les sessions `SCHEDULED` à venir, triées par date. Chaque carte met **le lieu au premier plan** :

- photo du lieu (grande, en tête de carte) ;
- « Chez **{nom du café}** » en titre, avec la mention « en collaboration avec » ;
- ville et adresse, lien « Voir sur la carte » (OpenStreetMap ou Google Maps, construit à partir de l'adresse) ;
- date, heure et durée (« Samedi 14 novembre · 14 h 00 – 16 h 00 ») ;
- état : compte à rebours avant l'ouverture, compteur de places une fois ouverte, ou « Complet ».

Le compte à rebours est calculé côté serveur (Mustache n'a pas de logique). Un petit script le fait défiler côté navigateur et recharge la carte via HTMX à l'heure d'ouverture. Le compteur de places est rafraîchi par HTMX (`hx-trigger="every 30s"` sur le fragment de la carte).

S'il n'y a aucune session à venir, un message invite à suivre `@eosa.bijoux` pour être prévenue. Les boutons Instagram/TikTok restent pour les demandes d'ateliers privés (entre amies).

### 2. Page de la session — `GET /workshops/sessions/{id}`

- En tête : **le lieu** (photo, nom, présentation courte, adresse, carte, lien vers le partenaire), puis date, heure, durée et compteur.
- Si les réservations sont ouvertes et qu'il reste de la place, un formulaire court :
  - prénom, nom ;
  - e-mail (récapitulatif, rappel, lien d'annulation) ;
  - téléphone (optionnel, pour prévenir en cas d'imprévu) ;
  - case obligatoire : acceptation des conditions (empreinte de 10 €, annulation libre jusqu'à 48 h avant, débit de 10 € en cas d'absence non excusée ou de retard de plus de 30 minutes).
- Texte explicatif : « Aucun paiement maintenant : nous prenons une empreinte de 10 € pour garantir votre place. Elle vous est rendue après l'atelier, et n'est débitée qu'en cas d'absence non excusée (au-delà de 30 minutes de retard). L'atelier se règle sur place. »
- Si la session n'est pas encore ouverte : compte à rebours à la place du formulaire.
- Si elle est complète : formulaire de liste d'attente (voir plus bas).

Cette page reprend la photo du lieu comme image Open Graph, pour être partagée sur Instagram par EOSA et par le café.

### 3. Empreinte — `POST /workshops/sessions/{id}/bookings`

1. Vérifier que la session est réservable (`SCHEDULED`, réservations ouvertes, places restantes) et que cet e-mail n'a pas déjà une réservation active sur la session.
2. Créer un `WorkshopBooking` en `PENDING`. Une réservation `PENDING` **compte dans les places occupées**, pour éviter le surbooking pendant que la participante est sur Stripe.
3. Créer une session Stripe Checkout d'empreinte (voir « Empreinte Stripe ») avec `client_reference_id` = id de la réservation et une expiration de 30 min (le minimum Stripe).
4. Rediriger vers Stripe.

Si la participante abandonne ou que la session Stripe expire, la place est libérée. La réservation passe en `EXPIRED` via `cancel_url`, ou via le CronJob `ExpirePendingWorkshopBookingsTask`.

### 4. Confirmation — `GET /workshops/bookings/success?session_id=…`

1. Récupérer la session Stripe et vérifier que l'empreinte est autorisée (`PaymentIntent` en `requires_capture`).
2. Passer la réservation en `CONFIRMED`, enregistrer l'id du `PaymentIntent` et la date limite de capture renvoyée par Stripe (`capture_before`). L'opération est idempotente : recharger la page ne fait rien de plus.
3. Envoyer l'e-mail récapitulatif (une seule fois).
4. Afficher la page de confirmation : lieu mis en avant (photo, nom, adresse, carte), date, heure, prénom, rappel de l'empreinte et des conditions d'annulation, lien « Annuler ma réservation ».

Si la participante annule sur Stripe (`cancel_url`), elle revient sur la page de la session avec un message, et la réservation passe en `EXPIRED`.

### 5. Annulation par la participante — `GET/POST /workshops/bookings/{token}/cancel`

Il n'y a pas de compte participante : le lien d'annulation contient un **jeton aléatoire non devinable** propre à la réservation (jamais l'id de la réservation).

- `GET` affiche le récapitulatif et un bouton « Confirmer l'annulation ».
- `POST` :
  - **plus de 48 h avant** le début : réservation → `CANCELLED`, empreinte libérée, place rendue disponible (et liste d'attente prévenue), e-mail de confirmation d'annulation ;
  - **sinon** : refus, avec une page qui explique qu'il faut contacter EOSA sur Instagram avec un motif, et un lien direct.
- Un lien vers une réservation déjà annulée affiche simplement son état.

### 6. Liste d'attente — `POST /workshops/sessions/{id}/waitlist`

- Quand une session ouverte est complète, la participante laisse prénom et e-mail (une inscription par e-mail et par session). Elle voit ensuite « Vous serez prévenue par e-mail si une place se libère ».
- Quand une place se libère (annulation, réservation expirée, capacité augmentée par l'admin), **toutes les inscrites encore en attente reçoivent un e-mail** avec un lien vers la session : la première qui réserve obtient la place. Ce choix est plus simple qu'une file nominative avec délai réservé et convient aux petits groupes.
- Une inscrite qui réserve sa place est retirée de la liste d'attente.
- Le lien de chaque e-mail permet aussi de se désinscrire de la liste (jeton dédié).
- Aucune notification n'est envoyée une fois la session commencée.

## E-mails

Tous envoyés depuis `contact@eosa.me` (nom affiché « EOSA »), avec `Reply-To` sur la même adresse. Ils sont rédigés en Mustache (`templates/mail/`), en HTML avec une version texte. **Le lieu y est toujours présent** : nom du café, adresse, lien vers la carte.
| E-mail | Déclencheur | Contenu principal |
|---|---|---|
| Récapitulatif | Réservation confirmée | « Votre place est réservée » ; lieu, date, heure, durée ; atelier réglé sur place (collier 33 €, bracelet 23 €, choix sur place) ; empreinte de 10 € rendue après l'atelier ; règle des 48 h ; lien d'annulation. |
| Rappel | La veille de la session (24 h avant) | « C'est demain ! » ; lieu, adresse, carte, heure ; rappel qu'il n'est plus possible d'annuler en ligne, contact Instagram en cas d'imprévu. |
| Annulation (participante) | Annulation par la participante | Confirmation de l'annulation et de la libération de l'empreinte. |
| Annulation (admin) | L'admin annule une réservation | Votre réservation a été annulée, empreinte libérée. |
| Session annulée | L'admin annule une session | La session chez {café} du {date} est annulée, empreinte libérée, lien vers les prochaines dates. |
| Place libérée | Une place se libère sur une session complète | Lien vers la session, « premier arrivé, premier servi », lien de désinscription. |

Envoi fiable : l'e-mail est envoyé après l'enregistrement en base. Chaque envoi est tracé par une colonne `…_sent_at`. En cas d'échec SMTP, l'erreur est journalisée et le CronJob `RetryWorkshopEmailsTask` retente l'envoi. Pour éviter les doublons (application et job en parallèle), chaque envoi est « réservé » d'abord par un `UPDATE … WHERE …_sent_at IS NULL`.

## Parcours administration

Toutes les routes sont sur le serveur admin, protégées par `EnsureIdentityHandler`, sur le même principe que `/jewels`, `/metal-colors` et `/charms`. Deux entrées sont ajoutées à la navigation admin : « Ateliers » et « Lieux ».

### Lieux

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/workshop-venues` | Liste des lieux (actifs puis archivés). |
| POST | `/workshop-venues` | Créer un lieu : nom, présentation courte, adresse, code postal, ville, lien (site ou Instagram du café), capacité par défaut (8), durée par défaut (120 min). |
| GET | `/workshop-venues/{id}` | Détail et modification, une section par bloc comme sur les bijoux. |
| PATCH | `/workshop-venues/{id}` | Modifier les informations (nom, présentation, adresse, lien). |
| PATCH | `/workshop-venues/{id}/defaults` | Modifier la capacité et la durée par défaut. |
| POST | `/workshop-venues/{id}/image` | Ajouter ou remplacer la photo (réutilise le domaine `image/`). |
| POST | `/workshop-venues/{id}/archive` | Archiver le lieu : il n'est plus proposé pour de nouvelles sessions et les sessions existantes ne changent pas. Un lieu n'est jamais supprimé, il reste dans l'historique. |

### Sessions

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/workshops` | Sessions à venir (puis passées) avec, pour chacune : lieu, date, **compteurs** (places occupées / capacité, inscrites en liste d'attente), date d'ouverture des réservations. |
| POST | `/workshops` | Créer une session : lieu, date, heure de début. Le choix du lieu **pré-remplit la capacité et la durée** avec ses valeurs par défaut, modifiables. |
| GET | `/workshops/{id}` | Détail : informations modifiables + liste des inscrites (nom, e-mail, téléphone, état, date de réservation, date limite de l'empreinte) + liste d'attente. |
| POST | `/workshops/{id}` | Modifier une session. La capacité ne peut pas descendre sous le nombre de places occupées. Changer la date ou le lieu n'est possible que tant qu'il n'y a aucune réservation active. Sinon, il faut annuler et recréer la session. |
| POST | `/workshops/{id}/cancel` | Annuler la session : réservations actives → `CANCELLED`, empreintes libérées, e-mail « Session annulée » à chaque inscrite et à la liste d'attente. |
| POST | `/workshops/{id}/bookings/{bookingId}/cancel` | Annuler la réservation d'une participante, **sans contrainte de délai** (cas du « motif valable » reçu sur Instagram) : empreinte libérée, e-mail d'annulation, liste d'attente prévenue. |
| POST | `/workshops/{id}/bookings/{bookingId}/attended` | Dès le début de la séance : marquer présente → empreinte libérée. |
| POST | `/workshops/{id}/bookings/{bookingId}/no-show` | **À partir de début + 30 min** seulement : marquer absente → empreinte de 10 € débitée. Avant ce délai, le bouton est désactivé et affiche l'heure à laquelle il sera disponible ; la route refuse aussi la demande. |
| POST | `/workshops/{id}/attended` | Raccourci : marquer présentes toutes les inscrites restées `CONFIRMED`. |

Les actions destructives (annulation, absence) demandent une confirmation dans l'interface.

## Empreinte Stripe

- Stripe Checkout en `mode=payment`, une ligne « Empreinte — Atelier EOSA chez {café} du {date} » de 10 €, `payment_intent_data[capture_method]=manual`, collecte de l'e-mail, `success_url` et `cancel_url` dédiées, `expires_at` à 30 min.
- **Présence, annulation, session annulée** → `POST /v1/payment_intents/{id}/cancel` : l'empreinte disparaît du compte de la participante.
- **Absence** → `POST /v1/payment_intents/{id}/capture` : 10 € débités.
- Si rien n'est fait, Stripe libère l'empreinte automatiquement à la date `capture_before`, ce qui revient à une présence.

Le client HTTP Stripe existant (`StripeHttpClient`) est réutilisé, avec une ressource dédiée aux ateliers. `StripeCheckoutSessionResource.post` reste propre au panier (livraison, téléphone obligatoire).

**Fenêtre de validité.** Une empreinte en ligne est valable environ 7 jours chez Stripe (certains réseaux de cartes ont une fenêtre plus courte, à vérifier dans la documentation Stripe au moment de l'implémentation). L'ouverture à J-7 a été choisie pour cette raison. Il reste un cas limite : une réservation faite dans les toutes premières heures de l'ouverture peut voir son empreinte expirer **pendant ou juste avant** l'atelier, et l'absence ne peut alors plus être débitée. C'est pour cela que la date `capture_before` de chaque réservation est enregistrée et affichée dans l'admin (voir le point ouvert n° 1).

## Tâches planifiées (CronJobs Kubernetes)

Il n'y a **pas de planificateur dans l'application**. Les traitements périodiques sont des **CronJobs Kubernetes** qui lancent l'image `eosa` en mode tâche « one-shot ». C'est le mécanisme déjà utilisé par `CreateDefaultAdministratorIdentityTask` : une variable d'environnement active la tâche, `Bootstrap` l'exécute puis quitte avec le code `0` (succès) ou `1` (échec), sans démarrer les serveurs.

| Tâche (`task/`) | Variable d'activation | Planification proposée | Rôle |
|---|---|---|---|
| `ExpirePendingWorkshopBookingsTask` | `EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK` | `*/5 * * * *` | Passe en `EXPIRED` les réservations `PENDING` dont la session Stripe a expiré, et prévient la liste d'attente si une place se libère. |
| `SendWorkshopRemindersTask` | `EOSA_SEND_WORKSHOP_REMINDERS_TASK` | `*/15 * * * *` | Envoie le rappel aux inscrites confirmées des sessions qui commencent dans les 24 h et ne l'ont pas encore reçu. |
| `RetryWorkshopEmailsTask` | `EOSA_RETRY_WORKSHOP_EMAILS_TASK` | `*/15 * * * *` | Renvoie les e-mails transactionnels (récapitulatif, annulation, place libérée) dont l'envoi a échoué. |

Chaque tâche délègue à un cas d'usage de `application/` (`ExpirePendingWorkshopBookings`, `SendWorkshopReminders`, `RetryWorkshopEmails`), testé unitairement. La classe de tâche ne fait que l'appeler et journaliser le résultat.

Manifeste `kubernetes/workshop-cronjobs.yaml` (ajouté à `kustomization.yaml`), un `CronJob` par tâche :

- même image, même `securityContext` et mêmes variables que le `Deployment` (secrets, ConfigMap et SMTP), puisque `Bootstrap` câble toutes les dépendances avant de lancer les tâches ; plus la variable d'activation de la tâche à `"true"` ;
- `timeZone: Europe/Paris` ;
- `concurrencyPolicy: Forbid` : une exécution ne démarre pas tant que la précédente tourne ;
- `startingDeadlineSeconds: 120`, `backoffLimit: 1`, `activeDeadlineSeconds: 240`, `restartPolicy: Never` ;
- `successfulJobsHistoryLimit: 1`, `failedJobsHistoryLimit: 3` pour garder les journaux des échecs ;
- mêmes ressources que l'`initContainer` existant, et volume `/tmp` en `emptyDir`.

Principes de robustesse :

- **Le bon fonctionnement de la boutique ne dépend pas de la cadence des CronJobs.** Une réservation `PENDING` ne compte dans les places occupées que pendant la durée de validité de sa session Stripe (30 min). Une place se libère donc à temps même si un job est en retard. Le job ne fait que mettre l'état à jour et prévenir la liste d'attente.
- **Chaque traitement est idempotent** : il est protégé par des `UPDATE … WHERE …_sent_at IS NULL` (ou `WHERE state = 'PENDING'`). Une exécution relancée ou en double ne renvoie pas d'e-mail et ne modifie rien deux fois.
- Au démarrage, chaque job exécute aussi les migrations Flyway, comme l'`initContainer`. Le verrou de Flyway évite les conflits. Les CronJobs doivent utiliser **la même version d'image** que le `Deployment`.

## Règles métier

- Places occupées = réservations `PENDING` **non expirées** (session Stripe créée il y a moins de 30 min) + `CONFIRMED` (+ `ATTENDED`/`NO_SHOW` pour l'historique).
- Une session est réservable si elle est `SCHEDULED`, si `maintenant ≥ début − 7 j`, si `maintenant < début` et s'il reste des places.
- Le contrôle de capacité est fait **en base, dans la même transaction** que la création de la réservation (`SELECT … FOR UPDATE` sur la session), pour éviter le surbooking quand deux réservations arrivent en même temps.
- Une participante peut être marquée présente dès le début de la séance. Elle ne peut être marquée absente (débit de l'empreinte) qu'à partir de **début + 30 minutes**, délai de tolérance pour les retards. Le débit est déclenché par l'admin, jamais automatiquement.
- Une même adresse e-mail (comparée sans tenir compte de la casse) ne peut avoir qu'une seule réservation active par session.
- La limite des 48 h et l'ouverture à J-7 se calculent à partir du début de la session, dans le fuseau `Europe/Paris`. Les dates sont stockées en `timestamptz`.

## Persistance — migration `V4__workshops.sql`

- `workshop_venues` : `id`, `name`, `description`, `address`, `postal_code`, `city`, `link_url` (nullable), `image_id` (nullable), `default_capacity`, `default_duration_minutes`, `archived`, `created_at`.
- `workshop_sessions` : `id`, `venue_id` (FK), `starts_at`, `duration_minutes`, `capacity`, `state`, `created_at`, `cancelled_at` (nullable).
- `workshop_bookings` : `id`, `session_id` (FK), `first_name`, `last_name`, `email`, `phone` (nullable), `state`, `cancel_token` (unique), `stripe_checkout_session_id`, `stripe_payment_intent_id` (nullable), `hold_capture_before` (nullable), `created_at`, `confirmed_at`, `cancelled_at`, `confirmation_email_sent_at`, `reminder_email_sent_at`, `cancellation_email_sent_at` (tous nullable).
- `workshop_waitlist_entries` : `id`, `session_id` (FK), `first_name`, `email`, `unsubscribe_token` (unique), `created_at`, `last_notified_at` (nullable), `removed_at` (nullable).
- Index uniques partiels sur `(session_id, lower(email))` pour les réservations actives et pour les inscriptions en attente.

## Organisation du code

Conformément à l'architecture existante :

- **Domaine `workshop/`** : records `WorkshopVenue`, `WorkshopSession`, `WorkshopBooking`, `WorkshopWaitlistEntry`, enums d'état, interfaces `…Provider`, et `WorkshopHolds` (interface de l'empreinte, implémentée par Stripe).
- **Domaine `mail/`** : record `Mail` (destinataire, sujet, HTML, texte) et interface `Mailer`.
- **`application/`** : un cas d'usage par commande, chacun avec ses tests unitaires Mockito :
  - lieux : `CreateWorkshopVenue`, `UpdateWorkshopVenue`, `AddImageToWorkshopVenue`, `ArchiveWorkshopVenue`, `ReadWorkshopVenues` ;
  - sessions : `CreateWorkshopSession`, `UpdateWorkshopSession`, `CancelWorkshopSession`, `ReadUpcomingWorkshopSessions` ;
  - réservations : `InitiateWorkshopBooking`, `ConfirmWorkshopBooking`, `ExpirePendingWorkshopBookings`, `CancelWorkshopBookingByParticipant`, `CancelWorkshopBookingByAdmin`, `MarkWorkshopBookingAttended`, `MarkWorkshopBookingNoShow` ;
  - liste d'attente : `JoinWorkshopWaitlist`, `LeaveWorkshopWaitlist`, `NotifyWorkshopWaitlist` ;
  - e-mails : `SendWorkshopReminders`, `RetryWorkshopEmails`.
- **`infrastructure/persistence/`** : `JdbcPersistedWorkshopVenues`, `JdbcPersistedWorkshopSessions`, `JdbcPersistedWorkshopBookings`, `JdbcPersistedWorkshopWaitlistEntries`.
- **`infrastructure/fetch/stripe/`** : `StripeWorkshopHoldResource` + adaptateur `StripeWorkshopHolds`.
- **`infrastructure/mail/`** : `SmtpMailer` (Jakarta Mail, dépendance `org.eclipse.angus:angus-mail`) + rendu des templates `templates/mail/`.
- **`task/`** : `ExpirePendingWorkshopBookingsTask`, `SendWorkshopRemindersTask`, `RetryWorkshopEmailsTask`, sur le modèle de `CreateDefaultAdministratorIdentityTask`, enregistrées dans le bloc `// TASK` de `Bootstrap`.
- **`kubernetes/`** : `workshop-cronjobs.yaml`, plus les clés SMTP dans `secret.yaml.example` et `config.yaml.example`, et dans le `Deployment`.
- **Templates** : section dans `workshops.mustache`, `workshop-session.mustache`, `workshop-booking-success.mustache`, `workshop-booking-cancel.mustache`, `workshop-waitlist-leave.mustache`, `admin/workshops.mustache`, `admin/workshop.mustache`, `admin/workshop-venues.mustache`, `admin/workshop-venue.mustache`, `mail/*.mustache`. Tous responsives (390 / 768 / 1280 px).
- **`robots.txt`** : exclure `/workshops/bookings/` et `/workshops/waitlist/`.

## Configuration

Nouvelles variables d'environnement (à ajouter à `.env.example` et `docs/deployment.md`) :

| Variable | Requise | Production (Infomaniak) | Local (Mailpit) |
|---|---|---|---|
| `EOSA_SMTP_HOST` | Oui | `mail.infomaniak.com` | `mailpit` (dans compose) ou `localhost` |
| `EOSA_SMTP_PORT` | Non (défaut `465`) | `465` | `1025` |
| `EOSA_SMTP_SECURITY` | Non (défaut `ssl`) | `ssl` (ou `starttls` avec le port `587`) | `none` |
| `EOSA_SMTP_USERNAME` | Non | `contact@eosa.me` | absente |
| `EOSA_SMTP_PASSWORD` | Non | mot de passe de la boîte, ou mot de passe d'application si la double authentification est active | absente |
| `EOSA_MAIL_FROM` | Non (défaut `contact@eosa.me`) | `contact@eosa.me` | `contact@eosa.me` |

Sans `EOSA_SMTP_USERNAME`, `SmtpMailer` se connecte sans authentification, ce qui est le cas de Mailpit. `EOSA_SMTP_SECURITY=none` n'est fait que pour le local.

L'empreinte utilise la clé Stripe existante (`EOSA_CLIENT_STRIPE_SECRET_KEY`).

Les variables d'activation des tâches (`EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK`, `EOSA_SEND_WORKSHOP_REMINDERS_TASK`, `EOSA_RETRY_WORKSHOP_EMAILS_TASK`, `false` par défaut) ne sont définies qu'à `"true"`, et uniquement dans leur CronJob. Elles ne doivent jamais l'être dans le `Deployment`.

Côté Kubernetes : `EOSA_SMTP_USERNAME` et `EOSA_SMTP_PASSWORD` vont dans le secret `eosa-secrets` (clés `smtp-username`, `smtp-password`) ; `EOSA_SMTP_HOST`, `EOSA_SMTP_PORT`, `EOSA_SMTP_SECURITY` et `EOSA_MAIL_FROM` vont dans la ConfigMap `eosa-configs`.

## Tests en local

### E-mails : Mailpit

Un service **Mailpit** est ajouté à `compose.yaml`. Il capture tous les e-mails envoyés par l'application, sans jamais les délivrer, et les affiche dans une interface web. On peut ainsi vérifier le contenu et le rendu HTML (mobile compris) sans risquer d'écrire à une vraie participante.

```yaml
  mailpit:
    image: axllent/mailpit
    ports:
      - "127.0.0.1:8025:8025"   # interface web : http://localhost:8025
    security_opt: [no-new-privileges:true]
    restart: unless-stopped
```

- Le port SMTP `1025` n'est pas publié : l'application le joint par le réseau compose.
- Dans `x-eosa-environment`, les valeurs par défaut pointent vers Mailpit (`EOSA_SMTP_HOST: ${EOSA_SMTP_HOST:-mailpit}`, `EOSA_SMTP_PORT: ${EOSA_SMTP_PORT:-1025}`, `EOSA_SMTP_SECURITY: ${EOSA_SMTP_SECURITY:-none}`). `docker compose up` continue donc de fonctionner sans aucune configuration.
- En lançant l'application depuis l'IDE (hors compose), on démarre seulement `docker compose up -d postgres mailpit`, avec `EOSA_SMTP_HOST=localhost`. Le port `1025` doit alors être publié sur `127.0.0.1`.
- Pour tester la délivrabilité réelle (spam, DKIM), il faut passer temporairement par le vrai SMTP Infomaniak, comme le prévoit le critère de fin de l'étape 3.

### Tâches planifiées

Les CronJobs n'existent pas en local. Chaque tâche est déclarée dans `compose.yaml` comme un service dans le profil `jobs`, sur le modèle de `create-admin`. Ces services ne démarrent pas avec `docker compose up` et se lancent à la demande :

```bash
docker compose run --rm expire-pending-workshop-bookings
docker compose run --rm send-workshop-reminders
docker compose run --rm retry-workshop-emails
```

### Empreinte Stripe

- Clé de test Stripe (`sk_test_…`) dans `.env`, et cartes de test Stripe : `4242 4242 4242 4242` pour une autorisation réussie, cartes de refus et cartes 3DS pour les cas d'erreur.
- Le retour de Stripe se fait par `success_url` (pas de webhook), donc aucun tunnel (`stripe listen`, ngrok) n'est nécessaire en local.
- Les empreintes, libérations et captures se vérifient dans le tableau de bord Stripe, en mode test.

## Étapes de mise en place

Chaque étape donne lieu à une PR indépendante, mergeable et déployable, avec un commit Conventional Commits. L'ordre est pensé pour que rien de visible ne soit publié côté boutique avant l'étape 5.

### Étape 0 — Prérequis (hors code)

- [ ] Infomaniak : vérifier que la boîte `contact@eosa.me` existe et récupérer le mot de passe SMTP (mot de passe d'application si la double authentification est active).
- [ ] DNS `eosa.me` : vérifier les enregistrements SPF, DKIM et DMARC fournis par Infomaniak, pour que les e-mails n'arrivent pas en spam.
- [ ] Stripe : vérifier que les captures manuelles sont disponibles sur le compte, et noter la fenêtre de validité réelle des empreintes (point ouvert n° 1).
- [ ] Rassembler les informations des cafés partenaires : nom, adresse, photo, lien, capacité.
- [ ] Rédiger avec EOSA les textes des CGV/FAQ sur l'empreinte et l'annulation.

### Étape 1 — Lieux (admin)

- Migration `V4__workshops.sql` (toutes les tables, pour n'avoir qu'une migration).
- Domaine `WorkshopVenue`, persistance JDBC, CRUD admin des lieux avec photo et archivage.
- **Terminé quand** : on peut créer, modifier, illustrer et archiver un café depuis l'admin ; les tests unitaires des cas d'usage passent.

### Étape 2 — Sessions (admin)

- Domaine `WorkshopSession`, CRUD admin, valeurs par défaut reprises du lieu, annulation d'une session (sans e-mail pour l'instant, puisqu'il n'y a pas encore de réservation).
- Liste des sessions avec compteurs (0 / capacité) et date d'ouverture des réservations.
- **Terminé quand** : l'admin peut planifier des sessions dans plusieurs cafés et les voir dans l'ordre chronologique.

### Étape 3 — Brique e-mail

- Dépendance `angus-mail`, `Mailer` + `SmtpMailer`, variables `EOSA_SMTP_*`, mise à jour de `.env.example`, `docs/deployment.md` et des manifestes `kubernetes/` (secret, ConfigMap, `Deployment`).
- Service Mailpit dans `compose.yaml`, avec les valeurs SMTP par défaut qui pointent dessus.
- Gabarit commun des e-mails (`templates/mail/`), aux couleurs d'EOSA.
- **Terminé quand** : en local, un e-mail de test apparaît dans Mailpit (`http://localhost:8025`) ; avec le SMTP Infomaniak, il part de `contact@eosa.me` et arrive en boîte de réception (pas en spam) sur Gmail et sur une adresse Infomaniak.

### Étape 4 — Réservation, empreinte et confirmation

- Ressource Stripe d'empreinte (Checkout en capture manuelle, cancel, capture).
- `InitiateWorkshopBooking`, `ConfirmWorkshopBooking`, page de session, formulaire, pages de succès et d'annulation Stripe, e-mail récapitulatif.
- Premiers CronJobs : `ExpirePendingWorkshopBookingsTask` et `RetryWorkshopEmailsTask`, avec `kubernetes/workshop-cronjobs.yaml` et leurs services équivalents (profil `jobs`) dans `compose.yaml`.
- Les inscrites sont visibles dans le détail admin de la session.
- **Terminé quand** : en mode test Stripe, une réservation complète fonctionne de bout en bout (empreinte visible sur Stripe, réservation `CONFIRMED`, e-mail reçu) ; une réservation abandonnée libère la place au bout de 30 min, même sans passage du job ; deux réservations simultanées sur la dernière place n'en confirment qu'une ; un `kubectl create job --from=cronjob/…` se termine avec le code 0.

### Étape 5 — Affichage public

- Section « Prochains ateliers » sur `/workshops`, avec les cartes centrées sur le lieu, le compte à rebours et le compteur de places rafraîchi par HTMX.
- Lien Open Graph de la page de session (photo du lieu).
- **Terminé quand** : les cas « bientôt », « ouvert », « dernière place », « complet » et « aucune session » s'affichent correctement à 390, 768 et 1280 px.

### Étape 6 — Annulations

- Annulation par la participante via le lien à jeton, avec la règle des 48 h.
- Annulation d'une réservation et d'une session par l'admin, avec libération des empreintes.
- E-mails d'annulation.
- **Terminé quand** : l'annulation fonctionne à 49 h du début, est refusée à 47 h (en renvoyant vers Instagram), et chaque annulation libère l'empreinte côté Stripe.

### Étape 7 — Liste d'attente

- Inscription quand la session est complète, notification quand une place se libère, désinscription, retrait automatique quand la personne réserve.
- Compteur de la liste d'attente dans l'admin.
- **Terminé quand** : une annulation sur une session complète envoie l'e-mail « Place libérée » à toutes les inscrites, et la première qui réserve obtient la place.

### Étape 8 — Rappel de la veille

- `SendWorkshopReminders`, l'e-mail de rappel et le CronJob `SendWorkshopRemindersTask`.
- **Terminé quand** : chaque inscrite confirmée reçoit un seul rappel, environ 24 h avant, même si le job est relancé à la main.

### Étape 9 — Après la session

- Actions « présente », « absente » et « tout le monde présent » dans le détail admin, avec la date limite de l'empreinte affichée pour chaque inscrite.
- **Terminé quand** : « présente » libère l'empreinte ; « absente » est refusée avant début + 30 min et débite 10 € ensuite (vérifié en mode test Stripe).

### Étape 10 — Mise en ligne

- Mise à jour de `cgv.mustache` (empreinte, annulation, absence) et de `faq.mustache` (« Comment réserver ? », « Qu'est-ce que l'empreinte ? », « Puis-je annuler ? »).
- Remplacement, sur `/workshops`, du texte « envoyez-nous un message pour connaître les dates » par la réservation en ligne (Instagram/TikTok restent pour les ateliers privés).
- Mise à jour de `CLAUDE.md` (routes et variables d'environnement).
- Passage de Stripe en mode live, création des vrais lieux et d'une première session.
- **Terminé quand** : une vraie réservation de test en production (puis annulée) fonctionne de bout en bout.

## Points ouverts

1. **Empreinte et réservations faites dès l'ouverture.** Avec une ouverture à exactement J-7, une empreinte prise dans les premières heures peut expirer avant début + 30 min, moment où l'absence peut être débitée, et le débit devient alors impossible. Options :
   - *a.* L'accepter : le cas est rare, et l'empreinte reste dissuasive ;
   - *b.* Ouvrir les réservations à J-6, ce qui laisse une marge d'un jour ;
   - *c.* Garder J-7 et vérifier précisément la fenêtre réelle avec Stripe à l'étape 0 avant de trancher.
2. **Capacité par défaut et durée par défaut** : ce document prend 8 places et 2 h. À confirmer.
3. **Heure de fermeture des réservations** : ce document prend le début de la session. On peut préférer une fermeture plus tôt (par exemple 2 h avant) pour préparer le matériel.
