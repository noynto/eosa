# Spécifications : collection

Statut : **à valider** (écrite le 18 novembre 2026). Aucun code n'est écrit ; ce document fixe le fonctionnement, ses dépendances et l'ordre de mise en place. Les points marqués « proposé » attendent la validation de l'administratrice.

## Contexte

Une collection regroupe des bijoux et porte une **vidéo** qui s'affiche en tête de la page d'accueil (le « hero »). Elle est nommée avec la saison et l'année (« Été 2027 »). Elle a trois états comme un bijou : brouillon, publiée, archivée. **Publier une collection publie les bijoux qu'elle contient.**

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Hero de l'accueil | La **collection publiée la plus récente** (date de publication) fournit la vidéo. Quand aucune collection n'est publiée, ou qu'elle n'a pas de vidéo, la page garde `hero.webp`. |
| 2 | États | `DRAFTED` → `PUBLISHED` → `ARCHIVED`, comme `JewelState`. Une collection en brouillon n'est visible que dans l'administration. |
| 3 | Publication | Publier une collection passe en `PUBLISHED` ses bijoux qui sont en `DRAFTED`. Un bijou `ARCHIVED` n'est pas touché (l'administratrice l'a retiré exprès). Une collection **sans vidéo ne peut pas être publiée** (proposé : le hero en dépend). |
| 4 | Dépublication et archivage | **Archiver une collection n'archive pas ses bijoux** : ils restent en vente, seule la vidéo quitte l'accueil. Le lien entre la collection et ses bijoux est conservé. |
| 5 | Bijoux et collections (proposé) | **Un bijou peut appartenir à plusieurs collections** (un classique reconduit d'une saison à l'autre), par une table de liaison. Sans collection, un bijou reste publiable seul, comme aujourd'hui. |
| 6 | Stockage de la vidéo | Une colonne **`BYTEA`** dans la base, **sans large object**, réglée en stockage `EXTERNAL` (`ALTER COLUMN content SET STORAGE EXTERNAL`) : pas de compression inutile sur du H.264, lecture d'une plage par `substring(content FROM ? FOR ?)`. |
| 7 | Mesure préalable | **Mesuré le 18 novembre 2026** (PostgreSQL 17, 8 Mio de données incompressibles) : une tranche de 256 Kio lit environ 1 ms et 37 pages de 8 Ko (la taille de la tranche, pas du fichier), au début, au milieu et à la fin ; soit environ 32 ms de base pour toute la vidéo. Le repli sur une table `video_chunks` n'est donc pas nécessaire. De bout en bout (JDBC et handler), la vidéo entière sort en 0,25 s en local. |
| 8 | Envoi au navigateur | Le handler gère **`Range`** (réponse `206`, `Content-Range`, `Accept-Ranges: bytes`, `Content-Length` de la plage), car Safari et iOS l'exigent. Il lit la base par **tranches de 256 Kio** (une requête `substring` par tranche) et les écrit au fur et à mesure. Sans `Range`, réponse `200` complète. `Cache-Control` long, l'URL contenant l'identifiant de la vidéo. |
| 9 | Format et taille | MP4 H.264, 1080p maximum, 5 à 10 secondes, **sans piste audio**, **8 Mo maximum** (refusé au-delà, avec un message). Lecture en `autoplay muted loop playsinline`. |
| 10 | Image de repli | Chaque vidéo est envoyée avec une **image d'affiche** (`poster`), affichée pendant le chargement et quand le visiteur demande moins de mouvement (`prefers-reduced-motion`) : dans ce cas la vidéo ne se lance pas. |
| 11 | Hors périmètre | Plusieurs vidéos par collection, vidéo avec son, transcodage dans l'application, page publique d'une collection, planification de la publication à une date. |
| 12 | Hero plein écran | Le hero prend **environ 85 % de la hauteur visible** (`min-h-[85svh]`), vidéo en fond (`object-cover`, recadrée au centre : **une seule vidéo**, pas de version verticale), texte en HTML par-dessus, boutons existants. Un **voile** de la couleur sombre de la marque à 70 % en bas, qui s'efface vers le haut, garantit le contraste du texte (≥ 4,5:1, même sur une image blanche). Le bouton lecture / pause reste (en haut à droite sur mobile pour ne pas gêner le texte). Prototype validé le 19 novembre 2026. |
| 13 | Texte de la collection | Une collection porte un **titre d'accroche** (80 caractères maximum) et un **court paragraphe** (240 caractères maximum), saisis dans l'administration. Ils sont facultatifs : sans eux le hero affiche le texte actuel (« Bijoux qui rayonnent. »). L'étiquette au-dessus du titre est « Collection » suivi du nom de la collection. |
| 14 | Sans collection publiée | Même plein écran avec `hero.webp` en fond, le voile et le texte actuel (étiquette « Nouvelle collection »). |

## Ce que le code actuel apporte et impose

1. Les **images** sont déjà en base (`images(id, name, format, content BYTEA)`) et servies par `GET /images/{id}` (`GetImageHandler`, cas d'usage `DownloadImage`). La vidéo suit le même principe dans sa propre table, avec en plus les lectures par plage.
2. `hero.webp` est un fichier statique de `src/main/resources/public` ; il reste le repli (décision 1).
3. Un cas d'usage n'en injecte pas un autre (README) : « publier une collection » modifie la collection et ses bijoux **dans un seul cas d'usage**, qui injecte les deux providers.
4. La page d'accueil ne charge que ce qui la concerne : le hero est un cas d'usage de lecture appelé par `GetIndexHandler`, ou un fragment htmx si l'on veut garder le handler à un seul cas d'usage.
5. Le disque de PostgreSQL en production est de 1 Gi : à 8 Mo par vidéo, quelques collections par an restent négligeables, mais l'archivage doit permettre de **supprimer la vidéo** d'une collection archivée.

## Vocabulaire

| Terme | Sens |
|---|---|
| Collection (`Collection`) | Un nom, une saison, une année, une vidéo, une image d'affiche, un état, une date de publication et la liste de ses bijoux. |
| Vidéo (`Video`) | Le fichier MP4 stocké en base : nom, format, taille, contenu. |
| Hero | Le bandeau de tête de la page d'accueil. |

## Parcours

### Administration

- **Liste** `GET /collections` : nom, état, nombre de bijoux, date de publication.
- **Fiche** `GET /collections/{id}` : nom, saison (Printemps, Été, Automne, Hiver) et année, vidéo et affiche à envoyer, bijoux à cocher, boutons **Publier**, **Archiver**, **Repasser en brouillon**.
- **Envoi de la vidéo** : un formulaire `multipart`, contrôle du type (`video/mp4`) et de la taille (8 Mo), message d'erreur en ligne (`InlineError`).
- Les actions sont des `POST`, avec confirmation (`hx-confirm`) pour la publication, qui publie les bijoux.

### Boutique

- **Accueil** `GET /` : le hero joue la vidéo de la collection publiée la plus récente, sinon montre `hero.webp`. Le texte et les boutons du hero ne changent pas.
- **Vidéo** `GET /videos/{id}` : handler public à gestion de `Range` (décision 8).
- La page d'accueil reste lisible sans vidéo (réseau lent, `prefers-reduced-motion`) : l'affiche remplace le mouvement, le contraste du texte sur l'image est vérifié.

## Règles métier

- Le nom est obligatoire ; l'année est comprise entre 2024 et 2100 ; une même saison et année ne sont pas utilisées par deux collections.
- Publier : la collection doit avoir une vidéo et une affiche ; elle passe `PUBLISHED` avec la date du jour (`Clock`), ses bijoux `DRAFTED` passent `PUBLISHED` avec leur date de publication.
- Archiver : permis depuis `PUBLISHED` ; les bijoux ne changent pas.
- Une collection `PUBLISHED` sans bijou est permise (la vidéo seule).
- Supprimer une collection supprime la liaison, jamais les bijoux.

## Persistance — migrations

- `V20__videos.sql` (faite) : `videos(id, name, format, size, content BYTEA)` et `ALTER TABLE videos ALTER COLUMN content SET STORAGE EXTERNAL`.
- `V21__collections.sql` (faite) : `collections(id, name, season, year, state, published_at, video_id, poster_image_id)`, unique sur `(season, year)`, la vidéo et l'affiche en `ON DELETE SET NULL` ; `collection_jewels(collection_id, jewel_id)` (clé primaire composée, suppression en cascade des deux côtés).

## Organisation du code

- Domaine : `collection/Collection`, `CollectionState`, `CollectionProvider` ; `video/Video`, `VideoProvider` (avec une lecture de plage).
- Cas d'usage : `CreateCollection`, `UpdateCollection`, `UploadCollectionVideo`, `PublishCollection`, `ArchiveCollection`, `ReadCollections`, `ReadHero`, `DownloadVideoRange`.
- Handlers de l'administration et de la boutique, un par cas d'usage.
- Implémentations : `JdbcPersistedCollections`, `JdbcPersistedVideos`.

## Sujets à traiter

- **Poids de la page d'accueil** : 8 Mo de vidéo sur mobile ; ne charger la vidéo qu'après l'affiche (`preload="metadata"`) et pas du tout en mode économie de données ou `prefers-reduced-motion`.
- **SEO** : le hero ne doit pas devenir l'image Open Graph à lui seul ; `hero.webp` reste `ogImageUrl` ([SEO](../tech/seo.md)).
- **Droits** : la vidéo ne doit contenir que des images et musiques dont EOSA détient les droits (pas de piste audio, donc pas de musique).

## Étapes de mise en place

Une branche et une pull request par étape.

### Étape 1 — Vidéo : stockage, lecture par plage, mesure (faite)

Table `videos` (`V20`), `Video` (métadonnées seulement, le contenu ne se lit que par plages) et `VideoProvider` (`write`, `read`, `readRange`, `delete`), `JdbcPersistedVideos`, cas d'usage `DownloadVideoRange` (calcule la plage et rend le contenu par tranches de 256 Kio, chacune par une requête) et `GET /videos/{id}`. La mesure est en décision 7. Tests : plage fermée, ouverte (`bytes=100-`), suffixe (`bytes=-500`), à cheval sur deux tranches, hors limites (`416`), plage illisible (vidéo entière). Pas encore d'envoi de vidéo : il vient avec l'étape 2.

### Étape 2 — Collection, administration

Deux pull requests, sans effet sur la boutique.

- **2a, domaine et persistance (faite)** : migration `V21`, `Collection`, `CollectionState`, `CollectionSeason`, `CollectionProvider` (`readIds(états)`, `read`, `write`, `delete`), `JdbcPersistedCollections`, cas d'usage `CreateCollection` (brouillon ; nom, saison et année requis ; une collection par saison et année), `UpdateCollection` (nom, saison, année, bijoux ; ne touche ni l'état ni la vidéo), `ReadCollection`, `ReadCollections`. Le câblage dans `Bootstrap` vient avec les handlers.
- **2b, administration (faite)** : liste `/collections` et fiche `/collections/{id}` avec trois sections indépendantes (informations, vidéo et affiche, bijoux) et la suppression.
  - `UpdateCollection` ne touche plus aux bijoux : `UpdateJewelsOfCollection` les remplace, et le bloc des bijoux de la fiche est un fragment htmx (`ReadCollectionJewelChoices` : tous les bijoux, cochés ou non), pour qu'enregistrer le nom ne vide jamais la liste.
  - `UploadCollectionMedia` envoie la vidéo et/ou l'affiche (au moins une) : MP4 de 8 Mo au plus (refusé sans lire plus de 8 Mo + 1 octet en mémoire), affiche JPEG, PNG, WebP ou AVIF. Chaque média remplace le précédent, supprimé une fois la collection enregistrée.
  - `DeleteCollection` supprime la collection, sa vidéo et son affiche, jamais ses bijoux ; **une collection publiée ne se supprime pas** (on l'archive d'abord, étape 3).
  - Testé de bout en bout en local (création, doublon, modification, vidéo de 8 Mio acceptée et de 8 Mio + 1 refusée, remplacement, suppression) et vu à 390, 768 et 1280 px. L'aperçu vidéo n'a pas été vu avec un vrai MP4.

### Étape 3 — Publication et archivage (faite)

`PublishCollection` et `ArchiveCollection`, section « Publication » de la fiche avec confirmation (`POST /collections/{id}/publish` et `/archive`).

- **Publier** (depuis un brouillon ou une collection archivée) : il faut une vidéo et une affiche. Les bijoux `DRAFTED` passent `PUBLISHED` (avec leur date de première publication, conservée s'ils en ont déjà une) ; les bijoux `ARCHIVED` et `PUBLISHED` ne changent pas. **Si un bijou en brouillon est incomplet** (nom, catégorie, accroche, prix, une image, les règles de `UpdateStateOfJewel`), la publication est **refusée avec un message qui nomme chaque bijou et ce qui lui manque**, et rien n'est écrit. La collection reçoit la date du jour : republier une collection archivée la remet donc en tête de l'accueil.
- **Archiver** : seulement depuis `PUBLISHED` ; ni les bijoux ni la date de publication ne changent. Une collection publiée n'est ni repassée en brouillon ni supprimée.
- Testé de bout en bout en local : refus sans vidéo, refus avec un bijou incomplet (rien d'écrit), publication (collection et bijou publiés et datés), refus de republier, de supprimer ou d'archiver hors des états permis, archivage (le bijou reste publié), republication.
- Les bijoux sont écrits avant la collection : si l'écriture de la collection échoue, republier la collection ne change plus rien aux bijoux (pas de transaction entre deux providers).

### Étape 4 — Hero de l'accueil (faite)

- **Cas d'usage** : `ReadCategoryStats` devient `ReadHome` (mêmes statistiques par catégorie, plus le hero). Le hero est le premier élément de la page : le charger par htmx l'aurait fait apparaître après coup, avec l'image fixe d'abord. `GetIndexHandler` garde donc un seul cas d'usage, qui lit avec deux providers (comme `ReadWorkshopSession`). Le hero est la collection **publiée la plus récente qui a sa vidéo et son affiche** ; sans elle, l'accueil garde `hero.webp`.
- **Page** : la vidéo prend la place de l'image à gauche du hero (le texte et les boutons ne changent pas : pas de texte sur la vidéo, donc pas de contraste à vérifier). Elle est muette, en boucle, `playsinline`, avec l'affiche pour `poster`.
- **Animations réduites et économie de données** : si le visiteur demande moins de mouvement (`prefers-reduced-motion`) ou économise ses données (`saveData`), la vidéo n'est ni chargée ni lancée ; l'affiche reste et le bouton permet de la lire.
- **Bouton lecture / pause** (44 px, `aria-label` qui suit l'état) : une animation de plus de 5 secondes doit pouvoir se mettre en pause (WCAG 2.2.2).
- **Pas de changement** : `og:image` reste `hero.webp` ; le nom de la collection n'est pas affiché (`aria-label` de la vidéo seulement).
- Testé en local avec une vraie vidéo : lecture à 390, 768 et 1280 px, affiche seule avec le bouton « Lire » en animations réduites, brouillon et collection archivée sans effet sur l'accueil, retour à `hero.webp` après archivage.

### Étape 4b — Hero plein écran (à faire)

- **Migration** `V22__collection_text.sql` : `headline` et `blurb` (texte, nulls permis) sur `collections`.
- **Domaine et cas d'usage** : `Collection` porte `headline` et `blurb` ; `UpdateCollection` les valide (longueurs, vides = absents) ; `ReadCollection` les renvoie ; `ReadHome.Hero` ajoute collection, titre et paragraphe.
- **Administration** : deux champs dans la section d'informations de la fiche.
- **Page** : `index.mustache` passe à un hero plein écran (décisions 12 à 14) ; les deux cas (vidéo, `hero.webp`) partagent la même mise en page.
- **Vérification** : contraste mesuré sur une capture de la vidéo, 390, 768 et 1280 px, animations réduites.

### Étape 5 — Documentation

Mettre à jour [Web](../tech/web.md) (routes), [Architecture](../tech/architecture.md) et ce document (statut).
