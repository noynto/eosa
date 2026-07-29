# Conception : renommage Jewel + customisation (matériaux, charms, parcours client)

## Contexte

Le projet `eosa` ne vend que des bijoux — le terme générique `Product` ne reflète pas le langage métier réel. Par ailleurs, le besoin business a évolué : un bijou doit pouvoir être vendu tel quel, ou personnalisé par le client (couleur de métal, couleur(s) de perles, longueur, charms) avant ajout au panier.

Ce document fixe le vocabulaire cible et découpe le travail en étapes livrables indépendamment, dans l'ordre convenu :

1. Renommage `Product` → `Jewel`
2. `JewelCategory` : passage d'enum à entité persistée + CRUD admin
3. `JewelMaterial` : nouveau concept + CRUD admin
4. `Charm` : CRUD admin (compléter l'existant)
5. `JewelFactorySession` + parcours de customisation client

## Vocabulaire cible

| Terme | Rôle |
|---|---|
| `Jewel` (ex-`Product`) | Le bijou catalogue : nom, tagline, prix, images, état, catégorie. Structure inchangée dans l'immédiat — seul le nom change en phase 1. |
| `JewelCategory` (ex-`ProductCategory`) | Catégorie du bijou (Necklace/Bracelet). Porte l'intervalle de longueur valide (min/max) — devient une entité persistée en phase 2, un enum en phase 1. |
| `JewelMaterial` | Matière première réutilisable : `type` (enum `METAL` \| `STONE` — `STONE` couvre aussi bien les perles que les pierres, pas de distinction) + couleur. Métal → choix unique (ex. Argenté/Doré). Stone → choix multiple, ensemble non ordonné de couleurs. |
| `Charm` | Catalogue global de charms (existant, à compléter côté CRUD admin). |
| `JewelFactorySession` | Session de customisation en cours, persistée en Mongo, référencée par cookie (comme `Cart`/`IdentitySession`). Contient le snapshot des choix en cours + `startedAt`. Pas d'expiration pour l'instant. |

`StripeProductResource` (client HTTP Stripe) **ne change pas** — `Product` y désigne l'objet de l'API Stripe elle-même, pas notre domaine.

## Parcours de customisation (client)

Sur la page d'un `Jewel`, deux actions possibles :
- **Ajouter au panier** directement (comportement actuel, inchangé)
- **Customiser** : lance une `JewelFactorySession` initialisée avec les paramètres par défaut du bijou de départ

### Principe UX : une histoire en 6 temps, pas un formulaire

Objectif : charge cognitive minimale. Une question à la fois (révélée progressivement via HTMX, comme le reste du site), jamais un gros formulaire à choix multiples affiché d'un coup. La sélection sur les choix uniques avance automatiquement à l'étape suivante (pas de bouton "suivant" à chercher) ; les choix multiples gardent un bouton "Continuer" explicite. Chaque étape affiche sa position (ex. "3/5") et permet de revenir en arrière sans perdre les choix précédents.

1. **Point de départ rassurant** — le bijou de départ est déjà affiché tel quel (jamais un champ vide) : "Voici {{nom du bijou}}, personnalise-le à ta façon."
2. **Métal** — "Quelle teinte de métal ?" — choix unique parmi les `JewelMaterial` de type `METAL`, sélection = avance automatique.
3. **Perle(s)/pierre(s)** — "Quelles perles veux-tu ?" — choix multiple parmi les `JewelMaterial` de type `STONE`, ensemble non ordonné, bouton "Continuer".
4. **Longueur** — "Quelle longueur pour toi ?" — valeur dans l'intervalle porté par la catégorie du bijou.
5. **Charms — explicitement optionnel** — "Une touche en plus ?" avec un lien "Passer cette étape" visible au même niveau que les choix, pour ne pas donner l'impression d'un champ obligatoire de plus.
6. **Résolution** — récapitulatif visuel du bijou final + prix recalculé, un seul bouton "Ajouter au panier".

Le résultat final est capturé en snapshot dans `CartItem`/`CheckoutItem` à la validation — pas de nouvelle entité catalogue type "Variant".

## Interface admin

Chaque étape (2 à 5) est livrée avec son propre CRUD admin, sur le même principe que `admin/jewel.mustache` aujourd'hui : une section indépendante par paramètre, avec son propre formulaire et son propre enregistrement (pas un unique gros formulaire).

## Phase 1 — Renommage Product → Jewel

Renommage complet : classes/packages Java, routes publiques (`/products` → `/jewels`), collection Mongo (`products` → `jewels`), templates, libellés visibles.

### Domaine (`product/` → `jewel/`)

| Actuel | Cible |
|---|---|
| `product/Product.java` | `jewel/Jewel.java` |
| `product/ProductCategory.java` | `jewel/JewelCategory.java` (reste un enum en phase 1) |
| `product/ProductProvider.java` | `jewel/JewelProvider.java` |
| `product/ProductState.java` | `jewel/JewelState.java` |
| `shared/ProductId.java` | `shared/JewelId.java` |

### Application

| Actuel | Cible |
|---|---|
| `AddImagesToProduct` | `AddImagesToJewel` |
| `AddProductToCart` | `AddJewelToCart` |
| `CreateProduct` | `CreateJewel` |
| `ReadProduct` | `ReadJewel` |
| `ReadProductIds` | `ReadJewelIds` |
| `RemoveProductFromCart` | `RemoveJewelFromCart` |
| `UpdateCategoryOfProduct` | `UpdateCategoryOfJewel` |
| `UpdatePriceOfProduct` | `UpdatePriceOfJewel` |
| `UpdateStateOfProduct` | `UpdateStateOfJewel` |
| `UpdateTaglineOfProduct` | `UpdateTaglineOfJewel` |
| `ReadCategoryStats`, `UpdateCartItemQuantity`, `InitiateCheckout` | inchangés en nom (référencent `Jewel` en interne seulement) |

### Persistence

| Actuel | Cible |
|---|---|
| `MongoPersistedProducts` | `MongoPersistedJewels` |
| `mongo/MongoConfiguredProducts` (collection `"products"`) | `mongo/MongoConfiguredJewels` (collection `"jewels"`) |
| `MongoPersistedCarts` | inchangé en nom |

### Web (handlers + routes)

| Actuel | Cible |
|---|---|
| `AddImagesToProductHandler` | `AddImagesToJewelHandler` |
| `CreateProductHandler` | `CreateJewelHandler` |
| `GetAdminProductHandler` | `GetAdminJewelHandler` |
| `GetAdminProductRowHandler` | `GetAdminJewelRowHandler` |
| `GetAdminProductsHandler` | `GetAdminJewelsHandler` |
| `GetProductCardHandler` | `GetJewelCardHandler` |
| `GetProductHandler` | `GetJewelHandler` |
| `GetProductsHandler` | `GetJewelsHandler` |
| `PatchCategoryOfProductHandler` | `PatchCategoryOfJewelHandler` |
| `PatchPriceOfProductHandler` | `PatchPriceOfJewelHandler` |
| `PatchStateOfProductHandler` | `PatchStateOfJewelHandler` |
| `PatchTaglineOfProductHandler` | `PatchTaglineOfJewelHandler` |

Routes (`Bootstrap.java`) : `/products*` → `/jewels*`, `/admin/products*` → `/admin/jewels*`, paramètre de chemin `{product-id}` → `{jewel-id}`.

### Templates (`src/main/resources/templates/`)

| Actuel | Cible |
|---|---|
| `product.mustache` | `jewel.mustache` |
| `products.mustache` | `jewels.mustache` |
| `partials/product-card.mustache` | `partials/jewel-card.mustache` |
| `admin/product.mustache` | `admin/jewel.mustache` |
| `admin/products.mustache` | `admin/jewels.mustache` |
| `admin/partials/product-row.mustache` | `admin/partials/jewel-row.mustache` |
| `admin/partials/product-images.mustache` | `admin/partials/jewel-images.mustache` |

Toutes les références internes (`hx-get`, liens `<a href>`, `{{> partial}}`) dans `cart.mustache`, `checkout/success.mustache`, `index.mustache`, `partials/header-main.mustache`, `partials/footer-main.mustache`, `partials/header-admin.mustache` sont mises à jour en conséquence.

### Reste du sweep

`CLAUDE.md` (table des routes, structure du projet), `docs/deployment.md`, `kubernetes/`, `requests/*.http` : toute occurrence de `/products` ou "produit" à but technique est passée en `/jewels`/`Jewel` — fait en dernière étape de la phase, une fois le code stabilisé (même approche que pour la migration Mustache).

## Vérification

- `./mvnw clean verify` après le renommage — aucun test ne doit référencer les anciens noms.
- Test manuel end-to-end (comme pour la migration Mustache) : navigation catalogue, fiche bijou, panier, admin CRUD bijou, avec une base Mongo locale fraîche (nouvelle collection `jewels`).
