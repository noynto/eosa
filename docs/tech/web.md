# Web : serveurs, routes, templates, htmx

`Bootstrap` démarre deux serveurs Javalin : la boutique publique sur `EOSA_PUBLIC_SERVER_PORT` (8080 par défaut) et l'administration sur `EOSA_ADMIN_SERVER_PORT` (18080), qui doit rester hors du réseau public. Les pages sont rendues côté serveur avec Mustache, et htmx fait les mises à jour partielles.

## Routes de la boutique (8080)

| Méthode | Chemin | Rôle |
|---|---|---|
| GET | `/` | Accueil |
| GET | `/jewels` | Liste des bijoux |
| GET | `/jewels/{id}` | Fiche d'un bijou |
| GET | `/jewels/{id}/card` | Fragment : carte d'un bijou |
| GET | `/images/{id}` | Téléchargement d'une image |
| GET | `/about` | Notre histoire |
| GET | `/cart` | Panier |
| POST | `/checkout` | Paiement Stripe |
| GET | `/workshops` | Page des ateliers et prochaines sessions |
| GET | `/workshops/sessions/{id}` | Page d'une session (lieu, réservation, liste d'attente) |
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
| GET | `/` | Redirection vers `/jewels` |
| * | `/jewels*`, `/metal-colors*`, `/charms*` | Catalogue |
| * | `/workshop-venues*` | Lieux des ateliers : création, modification, photo, archivage |
| * | `/workshops*` | Sessions : création, modification, annulation ; réservations (annulation, présente, absente) et liste d'attente |
| GET | `/images/{id}` | Images (miniatures) |

L'administration est une PWA installable : son service worker (`public/sw.js`) ne met jamais les pages en cache, seulement les fichiers statiques et les images.

## Templates Mustache

- Les modèles passés à `ctx.render(...)` sont des `Map<String, Object>` construites à la main dans chaque handler, sans classe de vue.
- Mustache n'a pas de logique : tout ce qui se calcule (dates en français, comparaisons, pluriels, classes CSS conditionnelles) est préparé en Java avant le rendu.
- Un `{{> partial}}` se résout depuis le dossier du template qui l'inclut : un template de `admin/` écrit `{{> ../partials/header-admin}}`.
- Chaque page inclut son propre en-tête et pied de page (`partials/header-main`, `footer-main`, `header-admin`, `footer-admin`), sans gabarit englobant.
- Les styles sont en Tailwind, chargé dans la page (`public/tailwind.min.js` + `tailwind.config.js` pour les couleurs et polices de la marque).

## htmx

- **Fragments** : une liste charge chaque carte par `hx-get=".../card" hx-trigger="load"` ; une carte peut se recharger d'elle-même (`hx-trigger="every 30s"`, cartes des sessions).
- **Réponses de l'administration** : succès → `HX-Redirect` (aller sur une page) ou `HX-Refresh` (recharger) ; refus métier → statut 422 avec un fragment HTML du message, affiché dans la cible (`hx-target`). La configuration `htmx.config.responseHandling` de `header-admin` laisse afficher les réponses 422.
- **Confirmations** : `hx-confirm` ouvre la boîte de dialogue maison de `footer-admin` au lieu du `confirm()` du navigateur.
- **Formulaires publics** : ce sont des formulaires HTML classiques (`method="post"`), qui marchent sans JavaScript ; une erreur réaffiche la page avec le message et les valeurs saisies.

## Liens envoyés par e-mail

Les messageries et antivirus ouvrent parfois les liens d'eux-mêmes. Un lien d'e-mail n'agit donc jamais sur un simple `GET` : il affiche une page avec un bouton, et c'est le `POST` du bouton qui confirme, annule ou désinscrit, puis redirige vers la page (`303`). Les liens portent un jeton aléatoire de 32 octets, jamais un identifiant.

## Responsive

Toute modification de template gère les trois tailles :

| Taille | Largeur | Appareils |
|---|---|---|
| Mobile | < 640 px | Téléphones (cible minimale : 390 px, iPhone 15) |
| Tablette | 640–1024 px | Tablettes en portrait, grands téléphones en paysage |
| Ordinateur | ≥ 1024 px | Tablettes en paysage, ordinateurs |

Le parcours principal se vérifie à 390, 768 et 1280 px avant de considérer un travail d'interface terminé.
