# Web : serveurs, routes, templates, htmx

`Bootstrap` démarre deux serveurs Javalin : la boutique publique sur `EOSA_PUBLIC_SERVER_PORT` (8080 par défaut) et l'administration sur `EOSA_ADMIN_SERVER_PORT` (18080), qui doit rester hors du réseau public. Les pages sont rendues côté serveur avec Mustache, et htmx fait les mises à jour partielles.

## Routes de la boutique (8080)

| Méthode | Chemin | Rôle |
|---|---|---|
| GET | `/` | Accueil |
| GET | `/jewels` | Liste des bijoux |
| GET | `/jewels/{id}` | Fiche d'un bijou |
| GET | `/jewels/{id}/card` | Fragment : carte d'un bijou |
| GET | `/jewels/latest` | Fragment : les derniers bijoux publiés, chargé par l'accueil ; l'ordre est celui de la date de première publication (`jewels.published_at`), la plus récente d'abord |
| GET | `/jewels/{id}/related` | Fragment : la section « D'autres bijoux » de la fiche |
| GET | `/customization/metal-colors` | Fragment : le choix de la couleur du métal, dans le formulaire d'ajout au panier |
| GET | `/customization/charms` | Fragment : le choix des breloques, dans le formulaire d'ajout au panier |
| GET | `/jewels/{id}/selection` | Fragment : « Votre sélection » (métal, breloques) et le prix total, recalculé à chaque changement du formulaire (`metalColorId`, `charmIds`) |
| GET | `/images/{id}` | Téléchargement d'une image |
| GET | `/videos/{id}` | Vidéo, en entier ou par plage (`Range` : 206, `Content-Range`, 416 hors limites), lue par tranches de 256 Kio |
| GET | `/about` | Notre histoire |
| GET | `/cart` | Panier, avec la remise de son code de promotion |
| POST, DELETE | `/cart/promo-code` | Applique le code saisi (refus en fragment, 422) ou le retire ; `HX-Redirect` vers le panier |
| POST, DELETE | `/cart/gift-card` | Applique le code de carte cadeau saisi (refus en fragment, 422 ; 10 essais par heure et par IP ; jamais avec un code de promotion) ou la retire ; `HX-Redirect` vers le panier |
| POST | `/checkout` | Paiement Stripe |
| GET, POST | `/gift-cards` | Achat d'une carte cadeau : page (non indexée tant que les textes ne sont pas écrits) ; le `POST` (champ piège + limite par IP) crée la carte en attente et la session Stripe puis redirige vers le paiement ; un refus réaffiche la page (422) |
| GET | `/gift-cards/success` | Retour de Stripe : active la carte payée et envoie l'e-mail ; aucun code n'est affiché |
| GET | `/gift-cards/{token}` | Page imprimable d'une carte, ouverte par son jeton (jamais par son code) |
| GET | `/workshops` | Page des ateliers et prochaines sessions |
| GET | `/workshops/sessions/{id}` | Page d'une session (lieu, réservation, liste d'attente) |
| GET | `/workshops/upcoming` | Fragment : la section « Prochains ateliers » de l'accueil (les trois prochaines sessions programmées, dont chaque carte se charge ensuite par `.../card`) ; vide, donc sans section, quand aucune session n'est à venir |
| GET | `/workshops/sessions/{id}/card` | Fragment : carte d'une session (rafraîchie toutes les 30 s) |
| POST | `/workshops/sessions/{id}/bookings` | Demande de réservation (champ piège + limite par IP) |
| POST | `/workshops/sessions/{id}/waitlist` | Inscription en liste d'attente (avant l'ouverture ou session complète) |
| GET/POST | `/workshops/bookings/{token}/confirm` | Confirmation depuis le lien de l'e-mail |
| GET/POST | `/workshops/bookings/{token}/cancel` | Annulation depuis le lien de l'e-mail (jusqu'à 48 h avant) |
| GET/POST | `/workshops/waitlist/{token}/leave` | Sortie de la liste d'attente depuis le lien de l'e-mail |

Les pages d'aide (`/faq`, `/terms`, `/privacy`, `/returns`, `/contact`, `/legal`), `robots.txt` et `sitemap.xml` sont déclarés directement dans `Bootstrap`.

## Routes de l'administration (18080)

`EnsureIdentityHandler` protège toutes les routes sauf `/sign-in`, `/images/*` et les fichiers statiques. Les routes n'ont pas de préfixe `/admin`. Les templates reçoivent `publicUrl` (l'adresse de la boutique) par un `FileRenderer` qui l'ajoute à chaque modèle.

| Méthode | Chemin | Rôle |
|---|---|---|
| GET/POST | `/sign-in` | Connexion (pose le cookie `identity-session-id`) |
| GET | `/` | Tableau de bord : une coquille dont chaque bloc se charge par htmx, avec son propre cas d'usage |
| GET | `/dashboard/alerts`, `/dashboard/todo`, `/dashboard/month`, `/dashboard/orders`, `/dashboard/workshops`, `/dashboard/offers` | Blocs du tableau de bord (fragments htmx) : e-mails non partis (204 quand tout va bien), à faire (fabriquer, expédier, suivre), chiffres du mois (livraison comprise, part de carte cadeau comprise, annulées exclues), dernières commandes, prochains ateliers, cartes cadeau, codes de promotion et catalogue |
| * | `/jewels*`, `/metal-colors*`, `/charms*` | Catalogue ; `PATCH /jewels/{id}/published-at` corrige la date de publication d'un bijou déjà publié (un jour, jamais dans le futur) |
| GET | `/orders`, `/orders/{id}`, `/orders/badge` | Commandes : liste filtrée (état, recherche), détail, pastille « à fabriquer » du menu (fragment htmx) |
| POST | `/orders/{id}/craft` | « Pièce fabriquée » : crée l'étiquette Mondial Relay (poids et dimensions du colis saisis) et passe la commande à fabriquée ; en cas d'échec, message en fragment (422) et la commande reste à fabriquer |
| POST | `/orders/{id}/craft-hand-delivery`, `/orders/{id}/hand-over` | Remise en main propre : « Pièce fabriquée, remise en main propre » (fabriquée sans étiquette ni e-mail) puis « Remise effectuée » (fabriquée → livrée, sans expédition) ; en cas de refus, message en fragment (422) |
| POST | `/orders/{id}/ship`, `/orders/{id}/deliver` | « Colis remis » (fabriquée → expédiée, e-mail de suivi à la cliente) et « Livrée » (expédiée → livrée) ; en cas de refus, message en fragment (422) |
| POST | `/orders/{id}/cancel` | Annule une commande à fabriquer, fabriquée ou expédiée, avec un motif ; ni e-mail, ni remboursement |
| GET | `/orders/{id}/label` | PDF de l'étiquette (redirection vers l'adresse protégée du transporteur), commande fabriquée ou expédiée ; ne modifie rien |
| * | `/promo-codes*` | Codes de promotion : liste, création, modification des règles (`PATCH`), activation et désactivation (`POST .../activate`, `.../deactivate`), suppression d'un code jamais utilisé (`DELETE`) ; la page d'un code liste les commandes qui l'ont utilisé |
| * | `/collections*` | Collections : liste, création, modification (`PATCH`), vidéo et affiche (`POST .../media`, multipart, MP4 de 8 Mo au plus), bijoux (`PATCH .../jewels`), publication et archivage (`POST .../publish`, `.../archive` ; la publication publie les bijoux en brouillon et est refusée si l'un est incomplet), suppression d'une collection non publiée (`DELETE`) ; `GET .../jewels` est le fragment htmx de la liste à cocher |
| GET | `/videos/{id}` | Vidéo d'une collection, par plage (même handler que la boutique), pour l'aperçu de la fiche |
| * | `/gift-cards*` | Cartes cadeau : liste, émission d'une carte offerte (`POST`), détail avec journal, recrédit (`POST .../restore`) et annulation (`POST .../void`) |
| * | `/workshop-venues*` | Lieux des ateliers : création, modification, photo, archivage |
| * | `/workshops*` | Sessions : création, modification, annulation ; réservations (annulation, présente, absente) et liste d'attente |
| GET | `/workshops/new` | Fragment : le formulaire « Nouvelle session », chargé à l'ouverture de la fenêtre |
| GET | `/workshops/{id}/bookings` | Fragment : la section « Inscrites » de la page d'une session |
| GET | `/workshops/{id}/waitlist` | Fragment : la section « Liste d'attente » de la page d'une session |
| POST | `/workshops/{id}/waitlist/remind` | Relance de la liste d'attente : e-mail « Il reste des places » aux inscrites, refusé si la session est annulée, commencée, pas encore ouverte ou complète ; les personnes prévenues il y a moins de 6 h sont ignorées |
| GET | `/workshop-venues/options` | Fragment : les lieux de la liste de choix du lieu (paramètre `selected` : lieu actuel) |
| GET | `/images/{id}` | Images (miniatures) |

L'administration est une PWA installable : son service worker (`public/sw.js`) ne met jamais les pages en cache, seulement les fichiers statiques et les images.

## Templates Mustache

- Les modèles passés à `ctx.render(...)` sont des `Map<String, Object>` construites à la main dans chaque handler, sans classe de vue.
- Mustache n'a pas de logique : tout ce qui se calcule (dates en français, comparaisons, pluriels, classes CSS conditionnelles) est préparé en Java avant le rendu.
- Un `{{> partial}}` se résout depuis le dossier du template qui l'inclut : un template de `admin/` écrit `{{> ../partials/header-admin}}`.
- Chaque page inclut son propre en-tête et pied de page (`partials/header-main`, `footer-main`, `header-admin`, `footer-admin`), sans gabarit englobant.
- Les templates de la boutique reçoivent `currentYear` (l'année de l'horloge, fuseau de Paris) pour le pied de page, ajoutée à chaque modèle par le `FileRenderer` de `Bootstrap`.
- Les prix de la boutique (fiche, carte, panier, breloques, livraison) se préparent par `PriceFormat.price` : « 42 » pour un montant rond, « 4,90 » sinon (virgule, deux décimales).
- Les styles sont en Tailwind, chargé dans la page (`public/tailwind.min.js` + `tailwind.config.js` pour les couleurs et polices de la marque).

## htmx

- **Fragments** : une liste charge chaque carte par `hx-get=".../card" hx-trigger="load"` ; une carte peut se recharger d'elle-même (`hx-trigger="every 30s"`, cartes des sessions).
- **Réponses de l'administration** : succès → `HX-Redirect` (aller sur une page) ou `HX-Refresh` (recharger) ; refus métier → statut 422 avec le fragment `partials/inline-error` rendu par `InlineError.render(ctx, e)`, affiché dans la cible (`hx-target`). Jamais de message concaténé dans du HTML en Java (voir [sécurité](security.md)). La configuration `htmx.config.responseHandling` de `header-admin` laisse afficher les réponses 422.
- **Appels complémentaires** : le handler d'une page n'appelle qu'un cas d'usage ; les informations d'un autre cas d'usage se chargent par un fragment htmx (voir [architecture](architecture.md#pages-composées-avec-htmx)).
- **Confirmations** : `hx-confirm` ouvre la boîte de dialogue maison de `footer-admin` au lieu du `confirm()` du navigateur.
- **Formulaires publics** (réservation et liste d'attente d'une session) : envoyés par htmx (`hx-post`, `hx-target` sur leur bloc, `hx-swap="outerHTML"`). Un refus s'affiche sous le formulaire (`#form-error`, statut 422 et en-têtes `HX-Retarget` / `HX-Reswap` posés par `WorkshopFormResponse`), et ce qui a été saisi reste en place ; un succès remplace le formulaire par sa confirmation. Le site a besoin de JavaScript pour s'afficher (Tailwind est compilé dans le navigateur), mais le formulaire garde `method="post"` : sans htmx, la même réponse arrive sur une page simple (`workshop-form-result`) avec un lien de retour. La page d'une session déclare `htmx.config.responseHandling` pour accepter les 422, comme l'administration.

## Liens envoyés par e-mail

Les messageries et antivirus ouvrent parfois les liens d'eux-mêmes. Un lien d'e-mail n'agit donc jamais sur un simple `GET` : il affiche une page avec un bouton, et c'est le `POST` du bouton qui confirme, annule ou désinscrit, puis redirige vers la page (`303`). Les liens portent un jeton aléatoire de 32 octets, jamais un identifiant.

## Responsive

Toute modification de template gère les trois tailles :

| Taille | Largeur | Appareils |
|---|---|---|
| Mobile | < 640 px | Téléphones (cible minimale : 390 px, iPhone 15) |
| Tablette | 640–1024 px | Tablettes en portrait, grands téléphones en paysage |
| Ordinateur | ≥ 1024 px | Tablettes en paysage, ordinateurs |

L'en-tête de la boutique garde le bouton « Menu » jusqu'à 1024 px : cinq liens ne tiennent pas en ligne à 768 px.

Le parcours principal se vérifie à 390, 768 et 1280 px avant de considérer un travail d'interface terminé.

Une grille à colonnes proportionnelles qui contient une image s'écrit `md:grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)]`, pas `[1.2fr_1fr]` : une colonne `fr` ne descend pas sous la largeur naturelle de son contenu, et la photo d'un bijou élargissait la fiche à 987 px sur un écran de 768 px. La colonne qui porte l'image reçoit aussi `min-w-0`.
