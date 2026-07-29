# Migration JTE → Mustache

## Objectif

Remplacer JTE (moteur de templates Java, compilé) par Mustache (`io.javalin:javalin-rendering-mustache`), dans un but de minimalisme du projet.

## Contraintes actées avec l'utilisateur

1. **Modèles de vue = `Map<String, Object>`**, pas de classes custom pour le moment. Les vues sont déjà découpées en fragments HTMX (`hx-get` par carte produit, par ligne admin, etc.) — si un template s'avère porter trop de logique parce qu'il n'est *pas* déjà fragmenté, il faut le redécouper en composants HTMX plutôt que de complexifier le `Map`.
2. **Pas de bricolage** : on suit la doc/sémantique standard de Mustache (`{{ }}`, `{{{ }}}`, `{{#section}}`, `{{^section}}`, `{{> partial}}`). Pas d'équivalent direct au `Content` param de JTE pour composer un layout — chaque page inclut explicitement ses partials d'en-tête/pied de page.
3. **Ce document d'abord, le code ensuite.**

## État des lieux

### Bibliothèque

`io.javalin:javalin-rendering-mustache:7.2.2` (gérée par `javalin-bom`) embarque `com.github.spullara.mustache.java:compiler` (JMustache — implémentation standard). Classe d'intégration : `io.javalin.rendering.template.JavalinMustache`, API :

```kotlin
JavalinMustache()                          // MustacheFactory par défaut
JavalinMustache(mustacheFactory: MustacheFactory)   // pour piloter la racine de résolution des templates/partials
```

Contrairement à `TemplateEngine.createPrecompiled(...)` de JTE, les templates Mustache ne sont **pas compilés** — ce sont des fichiers texte lus au runtime. On les place dans `src/main/resources/templates/` (inclus tel quel sur le classpath par Maven), avec une `DefaultMustacheFactory("templates")` pour que les chemins passés à `ctx.render(...)` et les `{{> partial}}` soient résolus relativement à ce dossier.

### Inventaire des 22 fichiers `.jte` et de leur usage réel

En traçant tous les appels `ctx.render(...)` (13 handlers + 4 lambdas inline dans `Bootstrap.java`, 16 sites au total), trois templates n'ont **aucune route qui les atteint** :

| Template | Statut |
|---|---|
| `payment.jte` | Aucune route ne le référence — maquette morte |
| `checkout/physical-address.jte` | Aucune route ne le référence — maquette morte |
| `checkout/physical-address-suggestions.jte` | `GetPhysicalAddressSuggestionsHandler` existe et appelle `ctx.render(...)`, mais ce handler n'est jamais instancié dans `Bootstrap.java` — code mort |

**Question ouverte à trancher avant de commencer** : migre-t-on ces 3 templates par souci de parité (au cas où le câblage arrive plus tard), ou les laisse-t-on de côté (cohérent avec l'objectif de minimalisme, vu qu'ils sont inatteignables aujourd'hui) ? Le plan ci-dessous les traite comme "hors périmètre" par défaut — à confirmer.

`layouts/checkout.jte` n'est utilisé que par ces deux templates morts (`payment.jte`, `checkout/physical-address.jte`) : si on les laisse de côté, ce layout n'a pas besoin d'équivalent Mustache.

### Les 19 templates réellement en jeu, groupés par layout

| Layout JTE | → Partials Mustache | Pages qui l'utilisent |
|---|---|---|
| `layouts/main.jte` | `partials/header-main.mustache` + `partials/footer-main.mustache` | `index`, `product`, `products`, `cart`, `checkout/success`, `legal`, `cgv`, `privacy`, `sign-in` |
| `layouts/admin.jte` | `partials/header-admin.mustache` + `partials/footer-admin.mustache` | `admin/product`, `admin/products` |
| — (fragments sans layout) | — | `partials/product-card`, `shipping-banner`, `shipping-info`, `admin/partials/product-row`, `admin/partials/product-images` |

Chaque page "pleine" inclura explicitement son header/footer, ex. :

```mustache
{{> partials/header-main}}
... contenu ...
{{> partials/footer-main}}
```

Le `title` de la page (aujourd'hui passé en paramètre à la layout JTE) devient une simple clé du `Map` passée au partial header (`{{title}}`), calculée côté handler.

## Ce qui doit migrer côté Java (logique retirée des templates)

Mustache ne pouvant évaluer ni condition composée, ni ternaire, ni appel de méthode, ni comparaison d'enum, ni arithmétique, tout ce qui suit doit être précalculé dans le handler avant `ctx.render(...)` et posé à plat dans le `Map` :

- **Formatage de prix** : `price.stripTrailingZeros().toPlainString()` → une `String` déjà formatée par entrée (`"38"`, jamais un `BigDecimal` brut).
- **Pluriel** (`count > 1 ? "s" : ""`) → soit une chaîne déjà pluralisée (`"3 pièces"`), soit un booléen `plural` consommé par une section Mustache.
- **États "vide/non vide"** (`imageIds.isEmpty()`, `cart.getItems().isEmpty()`) → Mustache gère nativement via section (`{{#items}}...{{/items}}`) + section inversée (`{{^items}}...{{/items}}`), pas besoin de booléen dédié dans la plupart des cas.
- **Image principale vs miniatures** (`product.jte` fait `imageIds.getFirst()` puis `subList(1, size)`) → le handler construit deux clés : `mainImageId` et `thumbnailImageIds` (liste déjà privée du premier élément).
- **Comparaisons d'enum pour `selected`** (catégorie, état produit) → booléens précalculés : `categoryNecklaceSelected`, `categoryBraceletSelected`, `stateDraftedSelected`, `statePublishedSelected`, `stateArchivedSelected`.
- **Classe CSS conditionnelle** (badge d'état à 3 couleurs dans `product-row.jte`) → une seule clé `stateBadgeClass` déjà résolue côté Java.
- **Arithmétique pour les boutons +/- du panier** (`item.quantity() - 1` / `+ 1`) → `decrementedQuantity` / `incrementedQuantity` précalculés par item.
- **Calcul du reste avant livraison gratuite** (`cart.jte`) → `remainingForFreeShipping` (déjà formaté, `null`/absent si non applicable) calculé dans `GetCartHandler`.
- **Destructuration avec valeurs par défaut** (`physical-address-suggestions.jte`, `!{var city = ... != null ? ... : ...}`) → si ce template est repris, le handler construit directement les chaînes finales, plus de `!{var}` dans le template.

Aucun de ces templates ne nécessite de refragmentation HTMX supplémentaire : les boucles réellement coûteuses (grilles de produits, lignes admin) sont *déjà* chargées via `hx-get` par petits fragments (squelette + un appel par carte/ligne) — seule leur logique de squelette (liste d'ids à boucler) doit migrer, ce qui est trivial en Mustache (`{{#productIds}}...{{/productIds}}`). Le panier (`cart.jte`) reste un rendu complet côté serveur (pas de fragmentation supplémentaire proposée) : ses données sont déjà toutes en mémoire dans le `Cart` chargé par le handler, fragmenter n'apporterait qu'des allers-retours réseau en plus pour une liste généralement courte — à confirmer si un avis différent.

## Plan détaillé, template par template

| Fichier `.jte` | → Fichier(s) `.mustache` | Handler(s) à modifier | Notes |
|---|---|---|---|
| `layouts/main.jte` | `partials/header-main.mustache`, `partials/footer-main.mustache` | tous ceux de la colonne "layout main" ci-dessus | `title` devient une clé de `Map` |
| `layouts/admin.jte` | `partials/header-admin.mustache`, `partials/footer-admin.mustache` | `GetAdminProductHandler`, `GetAdminProductsHandler` | idem |
| `index.jte` | `index.mustache` | `GetIndexHandler` | stats colliers/bracelets : count + pluriel + prix min déjà formaté |
| `products.jte` | `products.mustache` | `GetProductsHandler` | titre + nombre de pièces déjà pluralisé |
| `product.jte` | `product.mustache` | `GetProductHandler` | `mainImageId`/`thumbnailImageIds`, prix formaté ×2, `relatedIds` (déjà fragmenté) |
| `partials/product-card.jte` | `partials/product-card.mustache` | `GetProductCardHandler` | `hasImage`, prix formaté, `hasTagline` |
| `cart.jte` | `cart.mustache` | `GetCartHandler` | voir section précédente — liste `items` à plat, `remainingForFreeShipping`, totaux formatés |
| `checkout/success.jte` | `checkout/success.mustache` | `GetCheckoutSuccessHandler` | `isCompleted`, `isExpired`, `hasItems`, items déjà formatés |
| `sign-in.jte` | `sign-in.mustache` | lambda inline `Bootstrap.java` (`/sign-in`) | `hasError` (déjà booléen aujourd'hui) |
| `legal.jte`, `cgv.jte`, `privacy.jte` | `legal.mustache`, `cgv.mustache`, `privacy.mustache` | lambdas inline `Bootstrap.java` | 100% statique, aucun `Map` |
| `shipping-banner.jte` | `shipping-banner.mustache` | `GetShippingBannerHandler` | seuil déjà formaté |
| `shipping-info.jte` | `shipping-info.mustache` | `GetShippingInfoHandler` | idem |
| `admin/products.jte` | `admin/products.mustache` | `GetAdminProductsHandler` | `hasProducts` (section inversée pour l'état vide), liste d'ids déjà fragmentée |
| `admin/product.jte` | `admin/product.mustache` | `GetAdminProductHandler` | booléens `selected` (catégorie/état) listés plus haut ; inclut `admin/partials/product-images` via `{{> }}` |
| `admin/partials/product-row.jte` | `admin/partials/product-row.mustache` | `GetAdminProductRowHandler` | `stateBadgeClass`, `stateLabel`, `hasCategory`/`categoryLabel` |
| `admin/partials/product-images.jte` | `admin/partials/product-images.mustache` | `AddImagesToProductHandler` **et** inclus par `admin/product.mustache` | `hasImages` + liste `imageIds` |
| `payment.jte`, `checkout/physical-address.jte`, `checkout/physical-address-suggestions.jte`, `layouts/checkout.jte` | — | — | **hors périmètre par défaut** (voir question ouverte) |

## Changements de build

`pom.xml` :
- Retirer le plugin `gg.jte:jte-maven-plugin` (phase `generate-sources`) et la dépendance `io.javalin:javalin-rendering-jte`.
- Ajouter la dépendance `io.javalin:javalin-rendering-mustache` (version gérée par `javalin-bom`, déjà en `dependencyManagement`).

`Bootstrap.java` :
- Retirer `Path targetDirectory = Path.of("jte-classes")` et `TemplateEngine.createPrecompiled(...)`.
- Remplacer `javalinConfig.fileRenderer(new JavalinJte(templateEngine))` par :
  ```java
  javalinConfig.fileRenderer(new JavalinMustache(new DefaultMustacheFactory("templates")));
  ```
- Chaque `ctx.render("x.jte", ...)` devient `ctx.render("x.mustache", ...)` avec un chemin relatif à `src/main/resources/templates/`.

`Dockerfile` : aucun changement attendu — il n'y a pas d'étape spécifique aux classes JTE précompilées à retirer (le build passe déjà par `mvn package`).

`src/main/jte/` est supprimé une fois la migration terminée et vérifiée.

## Ordre d'exécution proposé

1. Build : dépendances pom.xml + wiring `Bootstrap.java` (renderer Mustache actif, aucun template migré pour l'instant — l'app ne démarre pas tant que rien n'est en place, donc cette étape est faite en même temps que le premier template).
2. Partials de layout (`header-main`/`footer-main`, `header-admin`/`footer-admin`) + un template simple (`legal.mustache`) pour valider la chaîne de bout en bout.
3. Fragments simples : `shipping-banner`, `shipping-info`, `partials/product-card`, `admin/partials/product-row`, `admin/partials/product-images`.
4. Pages statiques restantes : `cgv`, `privacy`, `sign-in`.
5. Pages avec logique modérée : `products`, `index`, `admin/products`.
6. Pages complexes : `product`, `admin/product`, `cart`, `checkout/success`.
7. Suppression de `src/main/jte/`, du plugin JTE et de sa dépendance.
8. Mise à jour de `CLAUDE.md` (stack, structure du projet).

## Vérification

- `./mvnw clean verify` (tests existants) après chaque étape.
- Lancement local (`./mvnw compile` + run) et parcours manuel des pages migrées à chaque étape, y compris les interactions HTMX (recherche produit, panier, admin CRUD) pour vérifier que les fragments `hx-swap`/`hx-target` continuent de recevoir le bon HTML.
- Vérifier au préalable que la clause 3 breakpoints (mobile/tablette/desktop) du CLAUDE.md reste respectée — la migration ne touche pas au HTML/CSS lui-même, seulement à la syntaxe de templating, donc aucun changement de mise en page n'est attendu.
