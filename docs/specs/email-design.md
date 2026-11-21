# Spécifications : design des e-mails

Statut : **à valider** (écrite le 21 novembre 2026). Aucun code n'est écrit ; ce document fixe les choix de design, leurs limites et l'ordre de mise en place. Il remplace la règle « e-mail à l'ancienne » de [E-mails](../tech/emails.md) (pas de `border-radius`, pas de `font-weight`, logo en Georgia) par un design qui suit les [fondations](../design/foundations.md) et Material 3 Expressive, **quitte à perdre un peu de compatibilité** avec les vieilles messageries.

## Contexte

12 gabarits HTML (`templates/mail/`) partagent un en-tête et un pied (`partials/header.html.mustache`, `footer.html.mustache`). Aujourd'hui :

- mise en page en tableaux, couleurs en `bgcolor`, ni `border-radius` ni `font-weight` ;
- le logo « eosa. » est écrit en **Georgia**, alors que celui du site est en **Bagel Fat One** ; les titres sont en Georgia au lieu de Cormorant Garamond ;
- un seul thème clair (`color-scheme: light`) ;
- le contrôle de compatibilité HTML de Mailpit est vers 95 %.

Gmail et Outlook pour Windows ne chargent pas les polices web : aucune écriture en CSS ne rendra Bagel Fat One partout.

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Logo | **Image PNG** du logo en Bagel Fat One, servie par le site (`/mail/logo.png`, fichier statique de `src/main/resources/public/mail/`), en deux fois la taille affichée (netteté sur écran Retina), avec un **texte alternatif « eosa. »** stylé en Georgia comme repli quand les images sont bloquées (Outlook). Décidé le 21 novembre 2026. |
| 2 | Fond du logo | Le PNG est **opaque, à la couleur de la carte** (`#FDFBF7`), pas transparent : certaines messageries (Gmail en mode sombre) inversent les fonds mais pas les images, et un logo rouge sombre transparent y deviendrait illisible sur fond sombre. |
| 3 | Mode sombre | **Non** : thème clair seulement (`color-scheme: light`), comme le site. Décidé le 21 novembre 2026. |
| 4 | Polices | Titres en **Cormorant Garamond**, texte en **DM Sans**, chargés par `@font-face` (fichiers déjà servis par le site, `/fonts/…`) pour Apple Mail, iOS Mail et les clients qui les gèrent ; **Georgia et Helvetica** restent les familles de secours dans Gmail et Outlook. Seules les graisses 400 et 500, comme le site. |
| 5 | Formes | Carte principale en **24 px** de rayon (jeton « Extra large »), encadrés en **16 px**, boutons en **pilule**. Outlook pour Windows ignore les rayons : il affiche des angles droits, sans casser la mise en page (pas de rustine VML). |
| 6 | Couleurs | Les hexadécimaux des jetons ([fondations](../design/foundations.md#couleurs)) : fond `bg-soft` `#F4EFE5`, carte `bg` `#FDFBF7`, contour `bg-border` `#F0E6D6`, titres `primary` `#601414`, texte `primary-dark` `#3D0A0A`, texte secondaire `secondary` `#775546`, texte discret `muted` `#7B6A4E`. |
| 7 | Bouton | Comme sur la boutique : fond `secondary` `#775546`, texte `bg` `#FDFBF7` (6,4:1), **48 px de haut au moins**, en pilule, texte en graisse 500. Une seule action principale par e-mail. |
| 8 | Hiérarchie | Un titre (Headline), un texte d'introduction, un encadré `bg-soft` pour les informations clés (lieu, date, récapitulatif), une action, une mention discrète. Surtitre en petites capitales espacées au-dessus du titre, comme sur le site (« ATELIERS »). |
| 9 | Accessibilité | Contrastes AA mesurés ; liens soulignés dans le texte ; `lang="fr"` ; `role="presentation"` sur les tableaux de mise en page ; textes alternatifs sur les images ; taille de texte courant de 15 px au moins, 12 px au plus petit. |
| 10 | Compatibilité | Cible : le contrôle de Mailpit reste **au moins vers 90 %** (il était vers 95 %). Le rendu est vérifié dans Mailpit à 390 et 600 px ; il n'y a pas de test sur Gmail ni Outlook réels (à faire à la main après la mise en ligne, voir « Sujets à traiter »). |
| 11 | Textes | Les contenus ne changent pas dans cette livraison (versions texte inchangées) : seul l'habillage change. |
| 12 | Découpage | **3 pull requests** : fondations, e-mails des ateliers, e-mails des commandes et de la carte cadeau. Décidé le 21 novembre 2026. |

## Ce que le code actuel apporte et impose

1. `MailTemplates` est créé sans paramètre (`new MailTemplates()`) et rend `templates/mail/<nom>.html.mustache` : il ne connaît pas l'adresse du site. Il doit recevoir l'adresse publique (`properties.publicBaseUrl()`) et ajouter à chaque modèle les clés `baseUrl` et `logoUrl`, sans que chaque `WorkshopMails` ou autre service d'e-mail ait à les fournir. `Mail.model()` peut être non modifiable : `MailTemplates` en fait une copie.
2. Les fichiers de `src/main/resources/public/` sont servis à la racine par la boutique (`/fonts/…`, `/hero.webp`) : le logo et les polices sont donc joignables par `https://eosa.me/…` en production et `http://localhost:8080/…` en local.
3. Le texte (`*.txt.mustache`) n'a pas de logo ni de mise en forme : il ne change pas.
4. L'e-mail `order-notification` est un e-mail **interne** (pour EOSA) et `test` sert aux essais : ils reprennent l'en-tête et le pied, mais n'ont pas besoin de la mise en forme complète.

## Règles de design (à reprendre dans `docs/design/emails.md`)

| Élément | Règle |
|---|---|
| Fond de page | `#F4EFE5`. Marge de 16 px sur mobile. |
| Carte | Largeur 560 px au plus, fond `#FDFBF7`, contour de 1 px `#F0E6D6`, rayon 24 px, logo en tête. |
| Logo | PNG 224 × 72 px affiché en 112 × 36 px, `alt="eosa."`, lien vers le site. |
| Surtitre | DM Sans 12 px, espacement des lettres 0,16 em, majuscules, `#775546`. |
| Titre | Cormorant Garamond 30 px, interligne 34 px, graisse 400, `#601414`. |
| Texte | DM Sans 15 px, interligne 24 px, `#3D0A0A`. |
| Encadré | Fond `#F4EFE5`, rayon 16 px, 20 px de marge intérieure ; le lieu en Cormorant Garamond 20 px. |
| Bouton | Fond `#775546`, texte `#FDFBF7` 15 px graisse 500, rayon 24 px, 48 px de haut au moins, 28 px de marge intérieure horizontale. |
| Mention | DM Sans 12 px, `#7B6A4E`, 4,5:1 sur la carte. |

## Étapes de mise en place

### Étape 1 — Fondations (à faire)

- **Logo** : générer `src/main/resources/public/mail/logo.png` (224 × 72 px, fond `#FDFBF7`, « eosa. » en Bagel Fat One `#601414`) à partir du fichier de police déjà dans `public/fonts/`, avec un Chromium sans interface ; documenter la commande dans `docs/tech/emails.md` pour pouvoir le regénérer.
- **Code** : `MailTemplates(publicBaseUrl)` ajoute `baseUrl` et `logoUrl` à chaque modèle ; `Bootstrap` lui passe l'adresse publique ; test dans `MailTemplatesTest`.
- **Partials** : `header.html.mustache` (`@font-face`, carte arrondie, logo en image) et `footer.html.mustache` ; nouvelles règles dans `docs/tech/emails.md` ; `docs/design/emails.md` (règles ci-dessus), référencé dans le README.
- **Essai** : le gabarit `test` reprend le nouveau design avec un bouton, un encadré et un titre, pour les vérifier dans Mailpit (390 et 600 px, contrôle de compatibilité).
- **Terminé quand** : `docker compose run --rm send-test-email` produit dans Mailpit un e-mail avec le logo en Bagel Fat One, une carte arrondie et un bouton en pilule, et le contrôle de compatibilité reste vers 90 % ou plus.

### Étape 2 — E-mails des ateliers (à faire)

Les 7 gabarits : `workshop-booking-request`, `workshop-booking-confirmation`, `workshop-booking-reminder`, `workshop-booking-cancellation`, `workshop-waitlist-booking-open`, `workshop-waitlist-place-available`, `workshop-waitlist-reminder`. Surtitre « ATELIER EOSA », encadré lieu/date/heure, bouton principal en pilule.

- **Terminé quand** : les 7 e-mails sont vérifiés dans Mailpit, avec leurs captures à 390 et 600 px et le score de compatibilité de chacun.

### Étape 3 — E-mails des commandes et de la carte cadeau (à faire)

`order-confirmation`, `order-shipped`, `gift-card`, `order-notification` (interne, plus sobre) et `test`. Le récapitulatif de commande (lignes, remise, livraison, total) devient un encadré arrondi ; le code de la carte cadeau est mis en avant.

- **Terminé quand** : les 5 e-mails sont vérifiés dans Mailpit comme à l'étape 2, et le `TODO` ne contient plus la ligne sur les e-mails.

## Sujets à traiter

- **Essai dans de vraies messageries** après la mise en ligne : Gmail (web et application), Outlook (Windows et web), Apple Mail et iOS Mail. Les rayons et les polices web y varient ; seul le logo en image est censé être identique partout.
- **Images bloquées** (Outlook) : le texte alternatif « eosa. » doit rester lisible ; à vérifier avec les images désactivées.
- **Poids** : le logo fait quelques Ko ; chaque e-mail ne charge que lui (et les polices dans les clients qui les gèrent).
- **Mode sombre** : écarté pour l'instant (décision 3). À reprendre si le site en reçoit un.
