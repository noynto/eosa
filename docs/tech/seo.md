# Référencement (SEO) et trafic

Ce que le site fait pour être bien compris par les moteurs de recherche, les règles à suivre sur chaque page publique, et les actions (sur le site et en dehors) qui amènent du trafic, par ordre d'impact attendu. Point fait le 3 novembre 2026.

## En place

| Élément | Où | Détail |
|---|---|---|
| `robots.txt` | `Bootstrap` | Exclut le panier, le paiement et les pages des liens d'e-mail ; indique le sitemap |
| `sitemap.xml` | `GetSitemapHandler`, cas d'usage `ReadSitemap` | Pages fixes, bijoux publiés, sessions d'atelier pas encore terminées |
| Titre, description, URL canonique | `partials/header-main` | Renseignés par chaque handler (`title`, `description`, `canonicalUrl`) |
| Partage (Open Graph, Twitter) | `partials/header-main` | Image (`ogImageUrl`), langue `fr_FR`, grande image sur X |
| Données structurées | `partials/header-main`, `JsonLd` | Voir ci-dessous |
| Pages privées non indexées | `noindex` | Liens d'e-mail, page « demande envoyée », sessions terminées, connexion |

### Données structurées (schema.org)

Écrites en JSON-LD dans le `<head>` quand le modèle contient `jsonLd`, construit en Java par `JsonLd.of(...)` :

- **Accueil** : la marque (`Organization` : EOSA, Nancy, logo, e-mail, Instagram) et le site (`WebSite`).
- **Fiche bijou** : `Product` avec toutes ses photos, la marque, le prix et l'offre. Permet l'affichage du prix dans les résultats et dans l'onglet Shopping de Google.
- **Session d'atelier** : `Event` (date et heure de Paris, lieu et adresse, organisateur, deux offres collier 33 € / bracelet 23 €, disponibilité : prévente avant l'ouverture, places disponibles, complet). Une session annulée reste indexée avec le statut `EventCancelled` jusqu'à sa date. Ces pages peuvent apparaître dans les résultats d'évènements de Google (« atelier bijoux Nancy »).

Le JSON est sérialisé par Gson, qui échappe `<`, `>`, `&`, `'` : c'est le seul `{{{…}}}` autorisé dans un template HTML (voir [sécurité](security.md)).

## Règles pour une page publique

- Un seul `<h1>`, qui dit ce qu'est la page.
- `title` de la forme `Eosa — <sujet>` et une `description` propre à la page (150 caractères environ), qui donne envie de cliquer.
- `canonicalUrl` absolue (`baseUrl + chemin`) et `ogImageUrl` absolue.
- Une page utile aux recherches s'ajoute au sitemap (liste fixe de `GetSitemapHandler` ou `ReadSitemap`) ; une page privée ou de passage reçoit `noindex`.
- Une image a un `alt` qui décrit le bijou : nom, pierre, couleur.

## Actions à mener, par ordre d'impact

### Hors du site (le plus rentable pour une marque locale)

1. **Google Search Console** : déclarer eosa.me, envoyer `https://eosa.me/sitemap.xml`, puis vérifier une fiche bijou et une session avec le [test des résultats enrichis](https://search.google.com/test/rich-results). C'est là qu'on voit les recherches qui amènent sur le site.
2. **Fiche Google Business Profile** : « Créatrice de bijoux » à Nancy, en zone desservie si pas de boutique ouverte au public ; photos, lien vers le site, et un post pour chaque atelier. C'est ce qui fait apparaître EOSA dans Google Maps et les recherches « bijoux Nancy ».
3. **Avis Google** : demander un avis aux participantes après chaque atelier (un lien dans l'e-mail de rappel ou en fin d'atelier).
4. **Cafés partenaires** : un lien vers la page de leur session depuis leur site, leur Instagram et leur fiche Google ; les liens venant de sites locaux pèsent beaucoup.
5. **Instagram** : lien du site en bio, et lien de chaque session en story à l'ouverture des réservations.
6. **Pinterest** : très utilisé pour chercher des bijoux ; épingler les photos des bijoux avec le lien de leur fiche.
7. **Agendas et presse locale** : agenda de Nancy Tourisme, groupes et évènements Facebook de Nancy, L'Est Républicain pour le lancement des ateliers.

### Sur le site

1. **Contenu des fiches bijou** : aujourd'hui une accroche seulement. Une vraie description (pierres, signification, dimensions, entretien) et des `alt` qui nomment la pierre répondent aux recherches comme « collier améthyste fait main ».
2. **Pages par pierre** : une page par pierre utilisée (signification, bijoux qui la portent). Les recherches sur la signification des pierres sont nombreuses et mènent naturellement aux bijoux.
3. **Adresses lisibles** : les bijoux ont une URL en identifiant (`/jewels/<uuid>`) ; une adresse avec le nom (`/jewels/collier-amethyste`) se lit et se partage mieux.
4. **Vitesse d'affichage** : Tailwind est compilé dans le navigateur (`tailwind.min.js`), ce qui retarde l'affichage, pénalisé par Google (Core Web Vitals). Le compiler au build en un fichier CSS est le plus gros gain technique, et permettrait aussi une `Content-Security-Policy`.
5. **Bandeau « Site en cours de développement »** : à retirer dès que le site est prêt ; il dessert la confiance des visiteurs.
