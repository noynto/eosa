# Sécurité : injections et requêtes forgées

Audit fait le 3 novembre 2026 sur les deux serveurs ; ce document dit comment l'application se protège et les règles à suivre pour que cela reste vrai.

## Injection SQL

**Protégé.** Toutes les requêtes passent par des `PreparedStatement` : une valeur venant de l'utilisateur est toujours un paramètre `?`, jamais concaténée dans le SQL. Les filtres de `readIds(…)` construisent leur requête avec un `StringBuilder`, mais n'y ajoutent que des fragments fixes (`" AND state = ANY (?)"`) ; les valeurs suivent en paramètres.

Règles :

- Jamais de valeur concaténée ou formatée (`+`, `formatted`, `String.format`) dans une chaîne SQL, même un identifiant ou un nom de colonne : un tri ou un filtre variable se choisit parmi des fragments écrits en dur.
- Les listes passent en tableau (`= ANY (?)` avec `createArrayOf`) ou en autant de `?` que de valeurs.

## Injection de JavaScript (XSS)

**Protégé** après correction (novembre 2026).

- **Templates** : Mustache échappe tout `{{valeur}}`. Les triples accolades `{{{valeur}}}` (sans échappement) ne servent que dans les e-mails en texte brut (`*.txt.mustache`), jamais dans du HTML.
- **Messages d'erreur htmx** : ils étaient concaténés dans du HTML en Java (`"<span>" + e.getMessage() + "</span>"`), alors qu'ils citent souvent une valeur de la requête (« Le produit `<id de l'URL>` n'existe pas »). Un lien piégé vers l'administration pouvait donc exécuter du script. Ils passent maintenant par `InlineError.render(ctx, e)`, qui rend `partials/inline-error.mustache` et échappe le message.
- **Images envoyées** : le type déclaré par le navigateur était enregistré et renvoyé tel quel ; un SVG ou un HTML servi depuis eosa.me aurait pu exécuter du script. Les cas d'usage d'ajout d'image n'acceptent plus que `Image.ACCEPTED_FORMATS` (JPEG, PNG, WebP, AVIF), et `/images/{id}` répond avec `Content-Security-Policy: sandbox` et `nosniff`.
- **Liens saisis dans l'administration** (lien d'un lieu) : seuls `http` et `https` sont acceptés, un `javascript:` est refusé.

Règles :

- Aucun HTML construit par concaténation en Java : un fragment se rend avec un template Mustache.
- Pas de `{{{…}}}` dans un template HTML.
- Une URL saisie par quelqu'un est vérifiée (`http`/`https`) avant d'aller dans un `href`.

## Requêtes forgées (CSRF)

**Protégé.** Une requête forgée est un formulaire posé sur un autre site qui fait agir le navigateur d'une personne connectée à son insu.

- Le cookie de session de l'administration est `HttpOnly`, `SameSite=Strict` et `Secure` en HTTPS. Mais eosa.me et admin.eosa.me sont le même « site » pour le navigateur : `SameSite` ne protège pas l'un de l'autre.
- `SecureRequestHandler`, enregistré en premier sur chaque serveur, refuse (403) toute requête `POST`, `PATCH`, `PUT` ou `DELETE` dont l'en-tête `Origin` n'est pas l'adresse du serveur (`EOSA_PUBLIC_BASE_URL` ou `EOSA_ADMIN_BASE_URL`), ou dont `Sec-Fetch-Site` indique un autre site. Pas de jeton à glisser dans chaque formulaire : les navigateurs actuels envoient toujours ces en-têtes.
- Les liens envoyés par e-mail agissent sur un `POST` depuis la page du site (voir [web](web.md#liens-envoyés-par-e-mail)) : ils passent ce contrôle.

Règles :

- Une action qui modifie des données n'est jamais un `GET`.
- L'adresse publique de chaque serveur doit être exactement celle que voit le navigateur (schéma, domaine, port) : sinon tous les formulaires sont refusés. En local, ouvrir `localhost`, pas `127.0.0.1`.

## En-têtes de sécurité

`SecureRequestHandler` ajoute à chaque réponse :

| En-tête | Valeur | Effet |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | Le navigateur ne devine pas le type d'un fichier |
| `X-Frame-Options` | `DENY` | Aucune page ne peut être affichée dans le cadre d'un autre site (clic piégé) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Un lien sortant ne transmet que le domaine, jamais le chemin (ni un jeton qu'il contiendrait) |

Pas encore de `Content-Security-Policy` sur les pages : Tailwind, chargé dans la page, génère des styles en ligne, et les templates ont quelques scripts en ligne. La poser demandera de compiler Tailwind au build et de sortir ces scripts dans des fichiers.

## Points restants

- Le code Mondial Relay (`infrastructure/fetch/mondialrelay`), non branché, construit sa requête SOAP par `formatted` : à échapper (XML) avant de le brancher.
- Le cookie du panier n'a ni `HttpOnly` ni `SameSite` explicite ; l'identifiant du panier suffit à le lire.
