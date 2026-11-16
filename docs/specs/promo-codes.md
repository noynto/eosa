# Spécifications : codes de promotion

## Contexte

Le site ne propose aucune réduction. Les codes de promotion servent à lancer une offre (bienvenue, newsletter, partenariat, remerciement) sans toucher aux prix des bijoux.

Les codes sont **conçus dans EOSA** : ils vivent dans la base du site et se gèrent depuis l'administration. Stripe n'a aucune notion de code : il ne reçoit que **le montant de la remise** à déduire, et affiche une ligne « remise » sur sa page de paiement.

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Source de vérité | Le code, ses règles et ses usages sont dans la base d'EOSA. Rien n'est créé à l'avance chez Stripe. |
| 2 | Types | **Pourcentage** (ex. 10 %) ou **montant fixe** (ex. 15 €). Un montant fixe supérieur aux bijoux est plafonné à leur total. |
| 3 | Assiette | La remise porte sur **les bijoux seulement**, jamais sur les frais de livraison. |
| 4 | Limites | Toutes facultatives : **date de début**, **date de fin**, **nombre d'usages total**, **montant minimum de bijoux** (avant remise). La limite d'un usage par adresse e-mail est écartée de cette version (voir Hors périmètre). |
| 5 | Saisie | La cliente saisit son code **dans le panier** ; le panier montre la remise et le total. Un seul code par panier. Le code se retire d'un clic. |
| 6 | Calcul | Toujours fait par le site, sur le panier courant : changer une quantité ou retirer un bijou recalcule la remise. Si le minimum n'est plus atteint, le code reste affiché avec la raison et ne remise rien. Le prix est **revérifié à la création de la session Stripe**. |
| 7 | Envoi à Stripe | À la création de la session, le site crée chez Stripe un **coupon à usage unique du montant de la remise** (`amount_off`, `max_redemptions` 1, durée `once`) et le rattache à la session (`discounts[0][coupon]`). Les lignes gardent leur prix catalogue. |
| 8 | Comptage des usages | Un usage est compté **quand la commande est enregistrée** (paiement confirmé), jamais à la saisie : un code n'est pas bloqué par un panier abandonné. Le compte est celui des commandes qui portent le code, sans table de plus. |
| 9 | Commande annulée | Elle **ne libère pas** l'usage du code (le remboursement se fait dans Stripe, la remise a bien été accordée). |
| 10 | Dépassement | Si deux clientes paient en même temps le dernier usage, les deux commandes sont **créées** (l'argent est déjà encaissé) : le dépassement est visible dans l'administration, jamais une commande refusée. |
| 11 | Total minimal | Stripe refuse un total **entre 0 et 0,50 €** (il accepte 0 €, vérifié en mode test). Une remise qui amènerait le total (bijoux remisés + livraison) en dessous de 0,50 € est **plafonnée** pour laisser 0,50 €. Le cas du panier entièrement couvert relève de la carte cadeau, pas de ce sujet. |
| 12 | Codes | Saisis en **majuscules sans accent ni espace** (le site normalise la saisie), uniques, 3 à 30 caractères (lettres, chiffres, tiret). |
| 13 | Désactivation | Un code se **désactive** sans être supprimé ; il ne se supprime que s'il n'a jamais servi (sinon l'historique des commandes perdrait son sens). |

## Modèle

| Élément | Rôle |
|---|---|
| `PromoCode` | Code, type (`PERCENT` ou `AMOUNT`), valeur, début, fin, usages maximum, minimum de bijoux, actif, date de création. |
| `PromoCodeProvider` | `readIds(filtres)`, `read`, `write`, `delete`, comme les autres ports. La recherche par code est un filtre de `readIds`. |
| `PromoDiscounts` (service statique, `service/`) | Règle croisée panier × code : dit si le code s'applique à un panier (dates, minimum, usages) et calcule la remise plafonnée. Il n'a pas de provider : le cas d'usage lui donne le code, le panier et le nombre d'usages déjà comptés. |
| Panier | Garde le **code appliqué** (colonne `promo_code_id` sur le panier) ; la remise n'est jamais stockée, elle se recalcule. |
| Commande | Garde `promo_code`, `promo_code_id` et `discount_amount` : la remise réellement accordée, photographiée comme les prix. |
| `CheckoutProvider.write` | Reçoit le montant de la remise ; l'adaptateur Stripe crée le coupon puis la session. |

Migration : `V13__promo_codes.sql` (table `promo_codes`, colonne du panier, colonnes de la commande).

## Parcours

1. **Administration** : liste des codes (état, usages, dates), création, modification, désactivation. Une page d'un code affiche ses usages (commandes concernées, remise totale accordée).
2. **Panier** : un champ « Code de promotion » (htmx). Un code invalide donne un message précis (inconnu ou expiré, minimum non atteint, usages épuisés) sans révéler les codes existants au-delà de ce qu'il faut.
3. **Paiement** : `InitiateCheckout` recalcule la remise, crée le coupon Stripe puis la session. Si le code n'est plus valable, le paiement est refusé avec un message, jamais accordé en silence.
4. **Commande** : `OrdersFromCheckouts` lit la remise réelle dans la session Stripe (`total_details.amount_discount`), l'enregistre avec le code. Le total reste celui payé.
5. **Affichage** : la remise apparaît dans la commande (administration), l'e-mail de confirmation et l'e-mail à EOSA, entre le sous-total et le total.

## Étapes

| # | Étape | Contenu |
|---|---|---|
| 1 | Codes et administration | Migration, `PromoCode`, provider JDBC, cas d'usage de création, modification, désactivation, lecture, pages d'administration. **Fait** : le cas d'usage de suppression arrive à l'étape 2, avec le comptage des usages. Le code n'est pas modifiable après sa création ; type, valeur et limites le sont. |
| 2 | Commande | Colonnes de la commande, enregistrement de la remise, comptage des usages, affichage dans l'administration et les e-mails. Passée avant le panier : sans remise, elle ne change rien, et aucune commande remisée n'est enregistrée sans son code. **Fait** : la remise est lue chez Stripe (`total_details.amount_discount`) et le code dans les métadonnées de la session (`promo_code_id`, `promo_code`, posées à l'étape 3). Le prix d'une ligne est lu **avant remise** (`amount_subtotal`) : `amount_total` est déjà remisé, la remise serait déduite deux fois. Un code n'est gardé sur la commande que s'il a remisé quelque chose. `OrderProvider.readIds` reçoit un filtre `promoCodeId` (usages comptés sur toutes les commandes, annulées comprises). Un code qui n'a jamais servi se supprime ; un code utilisé seulement se désactive (clé étrangère). |
| 3 | Panier et paiement | Code appliqué au panier, `PromoDiscounts`, remise à l'écran, coupon Stripe. **Fait** : vérifié chez Stripe en mode test, le coupon se répartit sur les lignes (`amount_discount` par ligne), la livraison n'est pas remisée, `amount_subtotal` reste avant remise et les métadonnées de la session reviennent à la lecture. La gratuité de livraison reste calculée sur les bijoux **avant remise** (migration `V15` : `carts.promo_code_id`, détaché si le code est supprimé). Le code reste attaché au panier même quand il ne s'applique plus (minimum non atteint, usages épuisés) : le panier affiche alors la raison et il se réapplique si le panier redevient éligible ; `InitiateCheckout` revérifie et, si le code est refusé, ouvre le paiement **sans remise**, comme l'a montré le panier (la page Stripe affiche le total réellement dû). Le code est détaché du panier au paiement confirmé. Un coupon Stripe est créé à chaque ouverture de paiement avec un code, qu'il soit payé ou non : les coupons non utilisés s'accumulent dans le tableau de bord Stripe, sans effet. |
| 4 | Textes | Mention dans les CGV (remise non cumulable, ni échangeable ni remboursable en argent, remboursement du montant réellement payé, code non rétabli après annulation) et question dans la FAQ. **Fait** : paragraphe « Codes de promotion » dans l'article 3 des CGV, qui précise aussi que le seuil de livraison offerte s'apprécie avant remise ; la politique de confidentialité ne change pas (aucune donnée de plus n'est stockée, la limite par e-mail étant écartée). Textes à relire par EOSA. |

## Sujets juridiques à valider

- **Conditions générales** : un code n'est ni échangeable ni cumulable avec un autre ; en cas de remboursement, la cliente récupère le montant réellement payé (remise déduite).
- **Annonce de prix** : EOSA n'affiche pas de prix barré, la remise ne s'annonce que par un code. Pas d'impact sur les règles d'annonce de réduction de prix.

## Hors périmètre

- Un usage par adresse e-mail : l'e-mail n'est connu que chez Stripe, après le panier ; à reprendre plus tard en le demandant dans le panier pour les codes concernés.
- Plusieurs codes sur un même panier.
- Codes limités à un bijou ou à une catégorie.
- Livraison offerte (une règle de livraison gratuite existe déjà dans le panier).
- Génération de codes en masse et export.
- Panier entièrement couvert : carte cadeau.
- Cumul avec une carte cadeau : un panier porte un code de promotion ou une carte cadeau, jamais les deux (voir [Carte cadeau](gift-card.md), décision 10).
