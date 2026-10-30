# Spécifications : réservation des ateliers

## Contexte

Aujourd'hui, la page `/workshops` présente l'atelier (8 personnes max, collier 33 €, bracelet 23 €), mais la réservation se fait uniquement par message privé sur Instagram/TikTok : aucune date n'est publiée et aucune place n'est suivie dans l'application.

Les ateliers ont lieu chez des partenaires, en particulier des **cafés avec lesquels EOSA collabore**. Le lieu fait donc partie de l'attrait de chaque session et doit être mis en avant.

L'objectif est de permettre à une participante de réserver elle-même sa place sur une session datée, depuis le site, et à l'administration de gérer les lieux, les sessions et les inscrites.

Deux risques sont à maîtriser : **les fausses réservations** (robot, blague, adresse inventée) qui bloquent des places, et **les participantes qui ne viennent pas**. La V1 les traite sans paiement : confirmation par e-mail, protections anti-robot et suivi des absences. Une empreinte bancaire Stripe est envisagée en V2 si les absences restent un problème (voir « V2 — Empreinte bancaire »).

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Paiement | Pas de paiement en ligne : l'atelier se règle sur place. Pas d'empreinte bancaire en V1. |
| 2 | Confirmation de la place | Après le formulaire, la place est **retenue 30 minutes** et la participante reçoit un e-mail avec un bouton **« Confirmer ma place »**. Sans confirmation dans ce délai, la place est libérée. Une adresse inventée ne peut donc rien bloquer longtemps. |
| 3 | Anti-abus | Une place par adresse e-mail et par session ; **piège à robots** invisible (champ caché) ; **limite de demandes par adresse IP**. |
| 4 | Absences | Après l'atelier, l'admin marque chaque inscrite présente ou absente. Une adresse marquée **absente** ne peut plus réserver en ligne : elle doit passer par Instagram. |
| 5 | Ouverture des réservations | Les réservations d'une session ouvrent **toujours 7 jours avant** son début, à la même heure. Avant l'ouverture, la session est visible avec un **compte à rebours**. Une fois ouverte, un **compteur de places restantes** est affiché. |
| 6 | Type de bijou | La participante **ne choisit pas** collier ou bracelet à la réservation : le choix se fait sur place. |
| 7 | Nombre de places | **Une réservation = une place.** Chaque participante réserve la sienne (pas de réservation groupée). |
| 8 | Lieux | Le lieu est une entité à part (`WorkshopVenue`), réutilisable d'une session à l'autre et **mis en avant** partout : nom du café, photo, adresse, lien vers le partenaire. |
| 9 | Administration | Gérer les lieux et les sessions, voir les inscrites, **annuler une session** et **annuler la réservation d'une participante**. |
| 10 | Annulation | La participante peut annuler elle-même **jusqu'à 48 h avant** le début. Au-delà, elle doit contacter EOSA sur Instagram avec un motif valable ; l'admin décide. |
| 11 | Confirmation | Une **page de confirmation** une fois la place confirmée + un **e-mail récapitulatif**. |
| 12 | E-mails | Envoyés depuis **`contact@eosa.me`** via le **SMTP Infomaniak**. |
| 13 | Parcours | **Séparé du panier** : un atelier ne se met pas dans le panier et n'utilise pas le checkout des bijoux. |
| 14 | Périmètre | Le rappel par e-mail, la liste d'attente, les valeurs par défaut des sessions et la mise à jour des CGV/FAQ font partie **de cette même livraison**. |

## Vocabulaire

| Terme | Rôle |
|---|---|
| `WorkshopVenue` | Lieu partenaire (café, bar, boutique…) : nom, courte présentation, adresse, ville, photo, lien (site ou Instagram), capacité et durée par défaut, actif/archivé. |
| `WorkshopSession` | Session datée : lieu, début, durée, capacité, état. |
| `WorkshopSessionState` | `SCHEDULED` \| `CANCELLED`. L'ouverture (J-7) et la fin des réservations sont calculées à partir de la date de début, sans changement d'état. |
| `WorkshopBooking` | Réservation d'une place par une participante sur une session. |
| `WorkshopBookingState` | `PENDING` (en attente de confirmation par e-mail) \| `CONFIRMED` \| `CANCELLED` \| `EXPIRED` (jamais confirmée). Après la session : `ATTENDED` \| `NO_SHOW`. |
| `WorkshopWaitlistEntry` | Inscription sur la liste d'attente d'une session complète. |

## Cycle de vie d'une session

```
création (admin)            ouverture = début - 7 j          début - 48 h           début          fin
     │   « Bientôt »             │   « Réserver »                 │                   │              │
     │   compte à rebours        │   compteur de places           │ plus d'annulation │              │ présente / absente
     │                           │   annulation libre             │ en autonomie      │              │
─────┴───────────────────────────┴────────────────────────────────┴───────────────────┴──────────────┴──────────▶
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
- ville et adresse, lien « Voir sur la carte » (construit à partir de l'adresse) ;
- date, heure et durée (« Samedi 14 novembre · 14 h 00 – 16 h 00 ») ;
- état : compte à rebours avant l'ouverture, compteur de places une fois ouverte, ou « Complet ».

Le compte à rebours est calculé côté serveur (Mustache n'a pas de logique). Un petit script le fait défiler côté navigateur et recharge la carte via HTMX à l'heure d'ouverture. Le compteur de places est rafraîchi par HTMX (`hx-trigger="every 30s"` sur le fragment de la carte).

S'il n'y a aucune session à venir, un message invite à suivre `@eosa.bijoux` pour être prévenue. Les boutons Instagram/TikTok restent pour les demandes d'ateliers privés (entre amies).

### 2. Page de la session — `GET /workshops/sessions/{id}`

- En tête : **le lieu** (photo, nom, présentation courte, adresse, carte, lien vers le partenaire), puis date, heure, durée et compteur.
- Si les réservations sont ouvertes et qu'il reste de la place, un formulaire court :
  - prénom, nom ;
  - e-mail (confirmation de la place, récapitulatif, rappel, lien d'annulation) ;
  - téléphone (optionnel, pour prévenir en cas d'imprévu) ;
  - case obligatoire : acceptation des conditions (annulation libre jusqu'à 48 h avant ; en cas d'absence sans prévenir, plus de réservation en ligne possible).
- Texte explicatif : « Aucun paiement en ligne : l'atelier se règle sur place. Après l'envoi du formulaire, confirmez votre place grâce à l'e-mail que vous allez recevoir. »
- Si la session n'est pas encore ouverte : compte à rebours à la place du formulaire.
- Si elle est complète : formulaire de liste d'attente (voir plus bas).

Cette page reprend la photo du lieu comme image Open Graph, pour être partagée sur Instagram par EOSA et par le café.

### 3. Demande de réservation — `POST /workshops/sessions/{id}/bookings`

1. **Anti-robot** : si le champ caché (piège) est rempli, la demande est ignorée silencieusement (la page « Vérifiez votre boîte mail » s'affiche, rien n'est enregistré ni envoyé). Si l'adresse IP a dépassé la limite (5 demandes par heure), la demande est refusée avec un message.
2. Vérifier la saisie (prénom, nom, e-mail valide, conditions acceptées).
3. Vérifier que la session est réservable (`SCHEDULED`, réservations ouvertes, pas encore commencée).
4. Refuser si l'adresse e-mail a déjà été marquée **absente** (`NO_SHOW`) sur un atelier : « Contactez-nous sur Instagram pour réserver. »
5. Refuser si l'adresse a déjà une réservation active sur la session (une réservation `PENDING` de plus de 30 min ne compte plus : elle est passée en `EXPIRED` au passage).
6. Créer la réservation en `PENDING`, avec un **jeton aléatoire non devinable**. Une réservation `PENDING` **compte dans les places occupées** pendant 30 min.
7. Vérifier qu'il reste bien une place (voir « Règles métier » pour les demandes simultanées) ; sinon, la réservation passe en `EXPIRED` et la session est annoncée complète.
8. Envoyer l'e-mail « Confirmez votre place ». En cas d'échec d'envoi, la réservation passe en `EXPIRED` et un message invite à vérifier l'adresse.
9. Rediriger vers la page **« Vérifiez votre boîte mail »** : adresse utilisée, délai de 30 min, rappel de regarder les spams, lien pour recommencer en cas d'erreur d'adresse.

### 4. Confirmation de la place — `GET/POST /workshops/bookings/{token}/confirm`

Les messageries et antivirus ouvrent parfois automatiquement les liens des e-mails : la confirmation ne doit donc **jamais** se faire sur un simple `GET`.

- `GET` affiche le récapitulatif (lieu mis en avant, date, heure, prénom) et un bouton **« Confirmer ma place »**.
- `POST` :
  - réservation `PENDING` de moins de 30 min : elle passe en `CONFIRMED`, l'e-mail récapitulatif est envoyé et la page de confirmation s'affiche ;
  - réservation déjà `CONFIRMED` : la page de confirmation s'affiche simplement (rien n'est renvoyé) ;
  - réservation expirée (plus de 30 min) : message « Le délai est dépassé, la place a été libérée », avec un lien vers la session pour réserver à nouveau s'il reste de la place.
- La page de confirmation reprend le lieu (photo, nom, adresse, carte), la date, l'heure, le rappel « atelier réglé sur place », les conditions d'annulation et le lien « Annuler ma réservation ».

### 5. Annulation par la participante — `GET/POST /workshops/bookings/{token}/cancel`

Il n'y a pas de compte participante : le lien d'annulation contient le jeton de la réservation (jamais son identifiant).

- `GET` affiche le récapitulatif et un bouton « Confirmer l'annulation ».
- `POST` :
  - **plus de 48 h avant** le début : réservation → `CANCELLED`, place rendue disponible (et liste d'attente prévenue), e-mail de confirmation d'annulation ;
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
| Confirmez votre place | Demande de réservation | « Confirmez votre place » ; lieu, date, heure ; bouton « Confirmer ma place » ; délai de 30 min, au-delà la place est libérée ; « vous n'êtes pas à l'origine de cette demande ? ignorez cet e-mail ». |
| Récapitulatif | Place confirmée | « Votre place est réservée » ; lieu, date, heure, durée ; atelier réglé sur place (collier 33 €, bracelet 23 €, choix sur place) ; règle des 48 h ; lien d'annulation. |
| Rappel | La veille de la session (24 h avant) | « C'est demain ! » ; lieu, adresse, carte, heure ; rappel qu'il n'est plus possible d'annuler en ligne, contact Instagram en cas d'imprévu. |
| Annulation (participante) | Annulation par la participante | Confirmation de l'annulation. |
| Annulation (admin) | L'admin annule une réservation | Votre réservation a été annulée. |
| Session annulée | L'admin annule une session | La session chez {café} du {date} est annulée, lien vers les prochaines dates. |
| Place libérée | Une place se libère sur une session complète | Lien vers la session, « premier arrivé, premier servi », lien de désinscription. |

Envoi : l'e-mail est envoyé après l'enregistrement en base, et chaque envoi est tracé par une colonne `…_sent_at`.

- L'e-mail « Confirmez votre place » est envoyé pendant la demande : s'il échoue, la demande échoue (la participante le voit tout de suite).
- Les autres e-mails peuvent échouer sans bloquer la participante : l'erreur est journalisée et le CronJob `RetryWorkshopEmailsTask` retente l'envoi plus tard.

## Parcours administration

Toutes les routes sont sur le serveur admin, protégées par `EnsureIdentityHandler`, sur le même principe que `/jewels`, `/metal-colors` et `/charms`. La navigation admin a une rubrique « Ateliers » avec « Sessions » et « Lieux ».

### Lieux

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/workshop-venues` | Liste des lieux (actifs puis archivés). |
| POST | `/workshop-venues` | Créer un lieu : nom, adresse, code postal, ville ; capacité (8) et durée (120 min) par défaut. |
| GET | `/workshop-venues/{id}` | Détail et modification, une section par bloc comme sur les bijoux. |
| PATCH | `/workshop-venues/{id}` | Modifier les informations (nom, présentation, adresse, lien). |
| PATCH | `/workshop-venues/{id}/defaults` | Modifier la capacité et la durée par défaut. |
| POST | `/workshop-venues/{id}/image` | Ajouter ou remplacer la photo (réutilise le domaine `image/`). |
| POST | `/workshop-venues/{id}/archive` | Archiver le lieu : il n'est plus proposé pour de nouvelles sessions et les sessions existantes ne changent pas. Un lieu n'est jamais supprimé, il reste dans l'historique. |

### Sessions

| Méthode | Chemin | Description |
|---|---|---|
| GET | `/workshops` | Sessions à venir (puis passées) avec, pour chacune : lieu, date, **compteurs** (places occupées / capacité, inscrites en liste d'attente), état des réservations. |
| POST | `/workshops` | Créer une session : lieu, date, heure de début. Le choix du lieu **pré-remplit la capacité et la durée** avec ses valeurs par défaut, modifiables. |
| GET | `/workshops/{id}` | Détail : informations modifiables + liste des inscrites (nom, e-mail, téléphone, état, date de réservation) + liste d'attente. |
| PATCH | `/workshops/{id}` | Modifier une session. La capacité ne peut pas descendre sous le nombre de places occupées. Changer la date ou le lieu n'est possible que tant qu'il n'y a aucune réservation active. Sinon, il faut annuler et recréer la session. |
| POST | `/workshops/{id}/cancel` | Annuler la session : réservations actives → `CANCELLED`, e-mail « Session annulée » à chaque inscrite et à la liste d'attente. |
| POST | `/workshops/{id}/bookings/{bookingId}/cancel` | Annuler la réservation d'une participante, **sans contrainte de délai** (cas du « motif valable » reçu sur Instagram) : e-mail d'annulation, liste d'attente prévenue. |
| POST | `/workshops/{id}/bookings/{bookingId}/attended` | Dès le début de la séance : marquer présente. |
| POST | `/workshops/{id}/bookings/{bookingId}/no-show` | **À partir de début + 30 min** seulement (tolérance pour les retards) : marquer absente. L'adresse ne peut alors plus réserver en ligne. Avant ce délai, le bouton est désactivé et affiche l'heure à laquelle il sera disponible ; la route refuse aussi la demande. |
| POST | `/workshops/{id}/attended` | Raccourci : marquer présentes toutes les inscrites restées `CONFIRMED`. |

Les actions destructives (annulation, absence) demandent une confirmation dans l'interface. Une absence marquée par erreur peut être corrigée en « présente ».

## Tâches planifiées (CronJobs Kubernetes)

Il n'y a **pas de planificateur dans l'application**. Les traitements périodiques sont des **CronJobs Kubernetes** qui lancent l'image `eosa` en mode tâche « one-shot ». C'est le mécanisme déjà utilisé par `CreateDefaultAdministratorIdentityTask` : une variable d'environnement active la tâche, `Bootstrap` l'exécute puis quitte avec le code `0` (succès) ou `1` (échec), sans démarrer les serveurs.

| Tâche (`task/`) | Variable d'activation | Planification proposée | Rôle |
|---|---|---|---|
| `ExpirePendingWorkshopBookingsTask` | `EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK` | `*/5 * * * *` | Passe en `EXPIRED` les réservations `PENDING` non confirmées depuis plus de 30 min, et prévient la liste d'attente si une place se libère. |
| `SendWorkshopRemindersTask` | `EOSA_SEND_WORKSHOP_REMINDERS_TASK` | `*/15 * * * *` | Envoie le rappel aux inscrites confirmées des sessions qui commencent dans les 24 h et ne l'ont pas encore reçu. |
| `RetryWorkshopEmailsTask` | `EOSA_RETRY_WORKSHOP_EMAILS_TASK` | `*/15 * * * *` | Renvoie les e-mails (récapitulatif, annulation, place libérée) dont l'envoi a échoué. |

Chaque tâche délègue à un cas d'usage de `application/` (`ExpirePendingWorkshopBookings`, `SendWorkshopReminders`, `RetryWorkshopEmails`), testé unitairement. La classe de tâche ne fait que l'appeler et journaliser le résultat.

Manifeste `kubernetes/workshop-cronjobs.yaml` (ajouté à `kustomization.yaml`), un `CronJob` par tâche :

- même image, même `securityContext` et mêmes variables que le `Deployment` (secrets, ConfigMap et SMTP), puisque `Bootstrap` câble toutes les dépendances avant de lancer les tâches ; plus la variable d'activation de la tâche à `"true"` ;
- `timeZone: Europe/Paris` ;
- `concurrencyPolicy: Forbid` : une exécution ne démarre pas tant que la précédente tourne ;
- `startingDeadlineSeconds: 120`, `backoffLimit: 1`, `activeDeadlineSeconds: 240`, `restartPolicy: Never` ;
- `successfulJobsHistoryLimit: 1`, `failedJobsHistoryLimit: 3` pour garder les journaux des échecs ;
- mêmes ressources que l'`initContainer` existant, et volume `/tmp` en `emptyDir`.

Principes de robustesse :

- **Le bon fonctionnement de la réservation ne dépend pas de la cadence des CronJobs.** Une réservation `PENDING` ne compte dans les places occupées que pendant 30 min : la place se libère à temps même si un job est en retard. Le job ne fait que mettre l'état à jour et prévenir la liste d'attente.
- **Pas de double envoi en usage normal** : un e-mail n'est retenté que s'il n'est pas marqué envoyé et que l'événement date de plus de 5 min (le temps que l'envoi immédiat ait eu lieu). Le rappel n'est envoyé qu'aux réservations dont `reminder_email_sent_at` est vide.
- Au démarrage, chaque job exécute aussi les migrations Flyway, comme l'`initContainer`. Le verrou de Flyway évite les conflits. Les CronJobs doivent utiliser **la même version d'image** que le `Deployment`.

## Règles métier

- Places occupées = réservations `PENDING` de moins de 30 min + `CONFIRMED` (+ `ATTENDED`/`NO_SHOW` pour l'historique).
- Une session est réservable si elle est `SCHEDULED`, si `maintenant ≥ début − 7 j`, si `maintenant < début` et s'il reste des places.
- **Demandes simultanées sur la dernière place** : la réservation est d'abord enregistrée, puis les places occupées sont recomptées. Si la capacité est dépassée, les réservations les plus récentes (au-delà de la capacité) se retirent d'elles-mêmes (`EXPIRED`) et leur demande est refusée comme « complet ». Chaque demande faisant le même calcul, il ne peut pas y avoir plus d'inscrites que de places.
- Une même adresse e-mail (comparée sans tenir compte de la casse) ne peut avoir qu'une seule réservation active par session (garanti aussi par un index unique en base).
- Une adresse e-mail marquée `NO_SHOW` sur un atelier ne peut plus réserver en ligne.
- **Limite par adresse IP** : 5 demandes de réservation par heure (en mémoire, par instance ; l'adresse vient de l'en-tête `X-Forwarded-For` posé par l'ingress, sinon de la connexion).
- Une participante peut être marquée présente dès le début de la séance, et absente à partir de **début + 30 minutes** seulement.
- La limite des 48 h et l'ouverture à J-7 se calculent à partir du début de la session, dans le fuseau `Europe/Paris`. Les dates sont stockées en `timestamptz`.

## Persistance — migration `V4__workshops.sql`

- `workshop_venues` : `id`, `name`, `description`, `address`, `postal_code`, `city`, `link_url` (nullable), `image_id` (nullable), `default_capacity`, `default_duration_minutes`, `archived`, `created_at`.
- `workshop_sessions` : `id`, `venue_id` (FK), `starts_at`, `duration_minutes`, `capacity`, `state`, `created_at`, `cancelled_at` (nullable).
- `workshop_bookings` : `id`, `session_id` (FK), `first_name`, `last_name`, `email`, `phone` (nullable), `state`, `cancel_token` (unique : jeton des liens de confirmation et d'annulation), `created_at`, `confirmed_at`, `cancelled_at`, `confirmation_email_sent_at`, `reminder_email_sent_at`, `cancellation_email_sent_at` (tous nullable). Les colonnes `stripe_checkout_session_id`, `stripe_payment_intent_id` et `hold_capture_before` sont prévues pour la V2 et restent vides en V1.
- `workshop_waitlist_entries` : `id`, `session_id` (FK), `first_name`, `email`, `unsubscribe_token` (unique), `created_at`, `last_notified_at` (nullable), `removed_at` (nullable).
- Index uniques partiels sur `(session_id, lower(email))` pour les réservations actives et pour les inscriptions en attente.

## Organisation du code

Conformément à l'architecture existante :

- **Domaine `workshop/`** : classes `WorkshopVenue`, `WorkshopSession`, `WorkshopBooking`, `WorkshopWaitlistEntry`, enums d'état, interfaces `…Provider` limitées à `readIds(filtres…)`, `read` et `write` (comme `JewelProvider`) : toute la logique (capacité, doublons, expiration, envois) est dans les cas d'usage.
- **Domaine `mail/`** : record `Mail` (destinataire, sujet, gabarit, modèle) et interface `MailProvider`.
- **`application/`** : un cas d'usage par commande, chacun avec ses tests unitaires Mockito :
  - lieux : `CreateWorkshopVenue`, `UpdateWorkshopVenue`, `UpdateDefaultsOfWorkshopVenue`, `AddImageToWorkshopVenue`, `ArchiveWorkshopVenue`, `ReadWorkshopVenues`, `ReadWorkshopVenue` ;
  - sessions : `CreateWorkshopSession`, `UpdateWorkshopSession`, `CancelWorkshopSession`, `ReadWorkshopSessions`, `ReadWorkshopSession` ;
  - réservations : `RequestWorkshopBooking`, `ConfirmWorkshopBooking`, `ExpirePendingWorkshopBookings`, `CancelWorkshopBookingByParticipant`, `CancelWorkshopBookingByAdmin`, `MarkWorkshopBookingAttended`, `MarkWorkshopBookingNoShow` ;
  - liste d'attente : `JoinWorkshopWaitlist`, `LeaveWorkshopWaitlist`, `NotifyWorkshopWaitlist` ;
  - e-mails : `SendWorkshopBookingConfirmation`, `SendWorkshopReminders`, `RetryWorkshopEmails`.
- **`infrastructure/persistence/`** : `JdbcPersistedWorkshopVenues`, `JdbcPersistedWorkshopSessions`, `JdbcPersistedWorkshopBookings`, `JdbcPersistedWorkshopWaitlistEntries`.
- **`infrastructure/mail/`** : `SmtpSentMails` (Jakarta Mail, dépendance `org.eclipse.angus:angus-mail`) + rendu des templates `templates/mail/`.
- **`infrastructure/web/`** : handlers publics et admin ; limite de demandes par IP dans le handler de demande de réservation.
- **`task/`** : `ExpirePendingWorkshopBookingsTask`, `SendWorkshopRemindersTask`, `RetryWorkshopEmailsTask`, sur le modèle de `CreateDefaultAdministratorIdentityTask`, enregistrées dans le bloc `// TASK` de `Bootstrap`.
- **`kubernetes/`** : `workshop-cronjobs.yaml`, plus les clés SMTP dans `secret.yaml.example` et `config.yaml.example`, et dans le `Deployment`.
- **Templates** : section dans `workshops.mustache`, `workshop-session.mustache`, `workshop-booking-requested.mustache`, `workshop-booking-confirm.mustache`, `workshop-booking-cancel.mustache`, `workshop-waitlist-leave.mustache`, `admin/workshops.mustache`, `admin/workshop.mustache`, `admin/workshop-venues.mustache`, `admin/workshop-venue.mustache`, `mail/*.mustache`. Tous responsives (390 / 768 / 1280 px).
- **`robots.txt`** : exclure `/workshops/bookings/` et `/workshops/waitlist/`.

## Configuration

Variables d'environnement (dans `.env.example` et `docs/deployment.md`) :

| Variable | Requise | Production (Infomaniak) | Local (Mailpit) |
|---|---|---|---|
| `EOSA_SMTP_HOST` | Oui | `mail.infomaniak.com` | `mailpit` (dans compose) ou `localhost` |
| `EOSA_SMTP_PORT` | Non (défaut `465`) | `465` | `1025` |
| `EOSA_SMTP_SECURITY` | Non (défaut `ssl`) | `ssl` (ou `starttls` avec le port `587`) | `none` |
| `EOSA_SMTP_USERNAME` | Non | `contact@eosa.me` | absente |
| `EOSA_SMTP_PASSWORD` | Non | mot de passe de la boîte, ou mot de passe d'application si la double authentification est active | absente |
| `EOSA_MAIL_FROM` | Non (défaut `contact@eosa.me`) | `contact@eosa.me` | `contact@eosa.me` |

Sans `EOSA_SMTP_USERNAME`, `SmtpSentMails` se connecte sans authentification, ce qui est le cas de Mailpit. `EOSA_SMTP_SECURITY=none` n'est fait que pour le local.

Les variables d'activation des tâches (`EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK`, `EOSA_SEND_WORKSHOP_REMINDERS_TASK`, `EOSA_RETRY_WORKSHOP_EMAILS_TASK`, `false` par défaut) ne sont définies qu'à `"true"`, et uniquement dans leur CronJob. Elles ne doivent jamais l'être dans le `Deployment`.

`EOSA_SEND_TEST_EMAIL_TO=<adresse>` lance la tâche one-shot `SendTestEmailTask` : elle envoie un e-mail de test à cette adresse puis quitte. En local : `docker compose run --rm send-test-email` (vers Mailpit, sauf si les variables SMTP sont surchargées dans `.env`).

Côté Kubernetes : `EOSA_SMTP_USERNAME` et `EOSA_SMTP_PASSWORD` vont dans le secret `eosa-secrets` (clés `smtp-username`, `smtp-password`) ; `EOSA_SMTP_HOST`, `EOSA_SMTP_PORT`, `EOSA_SMTP_SECURITY` et `EOSA_MAIL_FROM` vont dans la ConfigMap `eosa-configs`.

## Tests en local

### E-mails : Mailpit

Le service **Mailpit** de `compose.yaml` capture tous les e-mails envoyés par l'application, sans jamais les délivrer, et les affiche sur `http://localhost:8025`. On peut ainsi vérifier le contenu et le rendu HTML (mobile compris), et cliquer sur les liens de confirmation, sans risquer d'écrire à une vraie participante.

- Le port SMTP `1025` n'est pas publié : l'application le joint par le réseau compose.
- Dans `x-eosa-environment`, les valeurs par défaut pointent vers Mailpit. `docker compose up` fonctionne donc sans aucune configuration.
- En lançant l'application depuis l'IDE (hors compose), on démarre seulement `docker compose up -d postgres mailpit`, avec `EOSA_SMTP_HOST=localhost`. Le port `1025` doit alors être publié sur `127.0.0.1`.
- Pour tester la délivrabilité réelle (spam, DKIM), il faut passer temporairement par le vrai SMTP Infomaniak.

### Tâches planifiées

Les CronJobs n'existent pas en local. Chaque tâche est déclarée dans `compose.yaml` comme un service dans le profil `jobs`, sur le modèle de `create-admin`. Ces services ne démarrent pas avec `docker compose up` et se lancent à la demande :

```bash
docker compose run --rm expire-pending-workshop-bookings
docker compose run --rm send-workshop-reminders
docker compose run --rm retry-workshop-emails
```

## Étapes de mise en place

Chaque étape donne lieu à un commit Conventional Commits sur la branche `feat/workshop-booking`. L'ordre est pensé pour que rien de visible ne soit publié côté boutique avant l'étape 5.

### Étape 0 — Prérequis (hors code)

- [ ] Infomaniak : vérifier que la boîte `contact@eosa.me` existe et récupérer le mot de passe SMTP (mot de passe d'application si la double authentification est active).
- [ ] DNS `eosa.me` : vérifier les enregistrements SPF, DKIM et DMARC fournis par Infomaniak, pour que les e-mails n'arrivent pas en spam.
- [ ] Rassembler les informations des cafés partenaires : nom, adresse, photo, lien, capacité.
- [ ] Rédiger avec EOSA les textes des CGV/FAQ sur la réservation, l'annulation et les absences.

### Étape 1 — Lieux (admin) ✅

- Migration `V4__workshops.sql` (toutes les tables, pour n'avoir qu'une migration).
- Domaine `WorkshopVenue`, persistance JDBC, CRUD admin des lieux avec photo et archivage.

### Étape 2 — Sessions (admin) ✅

- Domaine `WorkshopSession`, CRUD admin, valeurs par défaut reprises du lieu, annulation d'une session.
- Liste des sessions avec compteurs et état des réservations.

### Étape 3 — Brique e-mail ✅

- `MailProvider` + `SmtpSentMails`, variables `EOSA_SMTP_*`, Mailpit dans `compose.yaml`, gabarit commun des e-mails, tâche d'e-mail de test.

### Étape 4 — Demande et confirmation de réservation ✅

- `RequestWorkshopBooking`, `ConfirmWorkshopBooking`, `SendWorkshopBookingConfirmation` ; page de session, formulaire avec piège à robots et limite par IP, page « Vérifiez votre boîte mail », page de confirmation ; e-mails « Confirmez votre place » et récapitulatif.
- Premiers CronJobs : `ExpirePendingWorkshopBookingsTask` et `RetryWorkshopEmailsTask`, avec `kubernetes/workshop-cronjobs.yaml` et leurs services équivalents (profil `jobs`) dans `compose.yaml`.
- Les inscrites sont visibles dans le détail admin de la session.
- **Terminé quand** : une réservation complète fonctionne de bout en bout avec Mailpit (demande, e-mail, confirmation, récapitulatif) ; un simple `GET` sur le lien ne confirme rien ; une réservation non confirmée libère la place au bout de 30 min, même sans passage du job ; deux demandes simultanées sur la dernière place n'en gardent qu'une ; le champ piège et la limite par IP bloquent les envois automatiques.

### Étape 5 — Affichage public ✅

Le compte à rebours est rafraîchi avec la carte (toutes les 30 s, à la minute près), sans script dédié.

- Section « Prochains ateliers » sur `/workshops`, avec les cartes centrées sur le lieu, le compte à rebours et le compteur de places rafraîchi par HTMX.
- Lien Open Graph de la page de session (photo du lieu).
- **Terminé quand** : les cas « bientôt », « ouvert », « dernière place », « complet » et « aucune session » s'affichent correctement à 390, 768 et 1280 px.

### Étape 6 — Annulations ✅

Faite avant l'étape 5 : le lien d'annulation de l'e-mail récapitulatif devait fonctionner.

- Annulation par la participante via le lien à jeton, avec la règle des 48 h.
- Annulation d'une réservation et d'une session par l'admin.
- E-mails d'annulation.
- **Terminé quand** : l'annulation fonctionne à 49 h du début et est refusée à 47 h (en renvoyant vers Instagram).

### Étape 7 — Liste d'attente ✅

- Inscription quand la session est complète, notification quand une place se libère, désinscription, retrait automatique quand la personne réserve.
- Compteur de la liste d'attente dans l'admin.
- **Terminé quand** : une annulation sur une session complète envoie l'e-mail « Place libérée » à toutes les inscrites, et la première qui réserve obtient la place.

### Étape 8 — Rappel de la veille ✅

Une place confirmée moins de 24 h avant le début ne reçoit pas de rappel : le récapitulatif vient de partir.

- `SendWorkshopReminders`, l'e-mail de rappel et le CronJob `SendWorkshopRemindersTask`.
- **Terminé quand** : chaque inscrite confirmée reçoit un seul rappel, environ 24 h avant, même si le job est relancé à la main.

### Étape 9 — Après la session ✅

- Actions « présente », « absente » et « tout le monde présent » dans le détail admin.
- **Terminé quand** : « absente » est refusée avant début + 30 min ; une adresse marquée absente ne peut plus réserver en ligne ; une absence corrigée en « présente » lève le blocage.

### Étape 10 — Mise en ligne (partie code ✅)

Fait : CGV (article 11 — Ateliers), FAQ, page `/workshops` (bouton « Voir les prochaines dates », Instagram/TikTok pour les ateliers privés), `CLAUDE.md`. Reste, hors code : étape 0, test réel avec Infomaniak, clés SMTP en production, création des vrais lieux et d'une première session.

- Mise à jour de `cgv.mustache` (réservation, annulation, absences) et de `faq.mustache` (« Comment réserver ? », « Je n'ai pas reçu l'e-mail de confirmation », « Puis-je annuler ? »).
- Remplacement, sur `/workshops`, du texte « envoyez-nous un message pour connaître les dates » par la réservation en ligne (Instagram/TikTok restent pour les ateliers privés).
- Mise à jour de `CLAUDE.md` (routes et variables d'environnement).
- Création des vrais lieux et d'une première session.
- **Terminé quand** : une vraie réservation de test en production (puis annulée) fonctionne de bout en bout.

## V2 — Empreinte bancaire (reportée)

Si les absences restent fréquentes malgré la V1, une **empreinte bancaire Stripe de 10 €** pourra être ajoutée : prise à la réservation (Stripe Checkout en capture manuelle) à la place de la confirmation par e-mail, rendue après l'atelier, débitée seulement en cas d'absence non excusée (à partir de début + 30 min). Points déjà identifiés :

- une empreinte n'est valable qu'environ 7 jours chez Stripe : avec une ouverture à J-7, une empreinte prise dès l'ouverture peut expirer avant le moment où l'absence peut être débitée (ouvrir à J-6 laisserait une marge) ;
- les CGV et la FAQ devront décrire l'empreinte ;
- les colonnes `stripe_checkout_session_id`, `stripe_payment_intent_id` et `hold_capture_before` de `workshop_bookings` sont déjà prévues.

## Points ouverts

1. **Capacité par défaut et durée par défaut** : ce document prend 8 places et 2 h. À confirmer.
2. **Heure de fermeture des réservations** : ce document prend le début de la session. On peut préférer une fermeture plus tôt (par exemple 2 h avant) pour préparer le matériel.
3. **Levée du blocage après une absence** : en V1, seule une correction en « présente » par l'admin lève le blocage. Faut-il une levée automatique au bout d'un certain temps ?
