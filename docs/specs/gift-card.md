# Spécifications : carte cadeau

Statut : **à valider** (écrite le 7 novembre 2026, révisée le 16 novembre 2026 après la mise en place des commandes et des codes de promotion). Aucun code n'est écrit ; ce document fixe le fonctionnement, ses dépendances et l'ordre de mise en place.

## Contexte

Une carte cadeau est un montant à dépenser dans la boutique EOSA. La personne qui l'achète la paie en ligne et reçoit **le code et une page imprimable**. Ce qu'elle en fait ensuite (l'offrir en main propre, l'imprimer, la transmettre) est de son côté : **le site ne connaît pas la destinataire**. La personne qui détient le code l'utilise au panier.

Les deux sujets dont elle dépendait existent : les **commandes** sont enregistrées au paiement (spécification [Commandes](orders.md)) et les **codes de promotion** passent par un coupon Stripe (spécification [Codes de promotion](promo-codes.md)). La carte cadeau s'appuie sur les deux (voir « Ce que le code actuel apporte et impose »).

## Décisions

| # | Sujet | Décision |
|---|---|---|
| 1 | Où l'utiliser | **Boutique en ligne seulement** : un code saisi au panier, déduit du paiement. Pas d'usage pour les ateliers (ils se règlent sur place, sans paiement en ligne). |
| 2 | Montant | **Montants proposés et montant libre** : des boutons (25, 50, 100 €) et un champ libre entre un minimum et un maximum. |
| 3 | Solde | **Utilisable en plusieurs fois** : une carte de 50 € dont on dépense 30 € garde 20 €. |
| 4 | Remise | **Par e-mail à la personne qui achète**, avec le code et le lien d'une page imprimable. Pas de destinataire dans le site : ni son nom, ni son e-mail, ni petit mot, ni envoi à une date choisie. Pas de carte physique. |
| 5 | Carte au porteur | Celle ou celui qui a le code peut l'utiliser. Le code perdu ou transmis par erreur est de la responsabilité de la personne qui l'a reçu (à écrire dans les CGV). |
| 6 | Validité (proposée) | **1 an** après l'achat, indiquée sur la carte et dans les CGV. À confirmer. |
| 7 | Bornes du montant libre (proposées) | De **10 à 200 €**. À confirmer. |
| 8 | Hors périmètre | Carte physique, destinataire et petit mot, envoi à date choisie, achat d'une carte avec une carte, usage pour les ateliers, remboursement en argent, transfert de solde entre cartes, rappel d'expiration (la seule adresse connue est celle de l'acheteuse), plusieurs cartes sur une même commande. |
| 9 | Confirmation du paiement | **Pas de webhook Stripe**, comme pour les commandes. La carte est activée au retour de la personne sur le site (le site interroge Stripe), et une **tâche planifiée toutes les 5 minutes** (`SettleGiftCards`, sur le modèle de `SettleOrders`) rattrape celles dont la personne n'est pas revenue. |
| 10 | Pas de cumul avec un code de promotion ; bijoux seulement, livraison exclue | **Un panier porte un code de promotion ou une carte cadeau, jamais les deux** (décision du 16 novembre 2026, qui remplace le cumul proposé d'abord). Ajouter l'un quand l'autre est présent est **refusé avec un message** (« Retirez d'abord le code de promotion pour utiliser une carte cadeau », et l'inverse) : rien n'est remplacé en silence. La carte paie **les bijoux seulement, jamais la livraison** : `min(solde, bijoux)`. La livraison reste à payer par carte bancaire chez Stripe. La carte ne change pas le seuil de livraison offerte (calculé sur les bijoux, comme aujourd'hui). |
| 11 | Un seul coupon chez Stripe | Une session Checkout n'accepte **qu'une remise** (vérifié en mode test : « Array discounts exceeded maximum 1 allowed elements »). Sans cumul, cette limite ne gêne plus : le site crée **un coupon à usage unique** du montant de la carte, comme il le fait pour un code de promotion, avec les métadonnées `used_gift_card_id` et `used_gift_card_amount` pour la retrouver à la commande (`gift_card_id` désigne la carte qu'une session **achète**). |
| 12 | Reste à payer : 0 € ou au moins 0,50 € | Stripe **accepte un total de 0 €** (la session se crée) mais **refuse un total entre 0 et 0,50 €** (vérifié en mode test : `amount_too_small`). Quand la carte laisserait moins de 0,50 € à payer, elle déduit moins pour que **0,50 €** restent à payer chez Stripe, et garde ces centimes sur son solde. Le cas ne se présente que si la livraison est offerte (sinon la livraison, qui dépasse 0,50 €, suffit à laisser un reste valable). |
| 13 | Panier entièrement couvert | Il ne l'est que si la carte couvre tous les bijoux **et** que la livraison est offerte (sinon la livraison reste à payer). **Par Stripe aussi, à 0 €.** Pas de chemin de commande sans Stripe : la session porte un coupon de 100 % et se conclut sans paiement (`payment_status` : `no_payment_required`, à lire comme payé ; aujourd'hui seul `paid` l'est). La cliente passe donc par la page Stripe, qui recueille son adresse et son téléphone comme pour toute commande. **À vérifier de bout en bout en mode test** (parcours dans un navigateur avec une carte de 100 % de la commande) avant de s'y fier. La première version de cette spécification supposait que Stripe refusait 0 € : c'est inexact. |
| 14 | Ce que la commande garde | Le **montant payé par carte** et la carte (`gift_card_amount`, `gift_card_id`), et **jamais en même temps qu'une remise de code de promotion** (`discount_amount` vaut zéro quand une carte a payé, décision 10) : le total reste ce que Stripe a encaissé. La commande, l'administration, l'e-mail de confirmation et l'e-mail à EOSA montrent une ligne « Carte cadeau ». |
| 15 | Commande annulée (proposé) | Le remboursement se fait dans Stripe pour **la part réellement payée** ; la part de la carte n'est pas rendue automatiquement. L'administratrice **recrédite la carte à la main** depuis sa page (mouvement « recrédité », avec un motif). La page d'annulation d'une commande payée en partie par carte le rappelle. |
| 16 | Aucune commande pour l'achat d'une carte | Une carte cadeau n'est pas une commande de bijoux (décision 12 des commandes). `SettleOrders` crée aujourd'hui une commande pour **toute session payée qui porte un panier** : la session d'achat d'une carte **n'a donc pas de `client_reference_id`** et se reconnaît à une métadonnée `gift_card_id`. Elle ne demande ni adresse ni téléphone (une méthode de création de session à part). |

## Ce que le code actuel apporte et impose

1. **Les commandes sont enregistrées.** `ConfirmCheckoutSession` crée la commande au retour de la cliente et `SettleOrders` rattrape les autres. La carte cadeau reprend le même schéma, avec ses propres cas d'usage : `ConfirmGiftCardPayment` et `SettleGiftCards`.
2. **La remise d'un code de promotion passe déjà par un coupon.** `StripeCheckoutSessionResource.post(…, Discount)` crée un coupon `amount_off` à usage unique puis le rattache à la session. La carte réutilise ce mécanisme, sans le cumuler : le coupon porte la part de la carte, avec les métadonnées `used_gift_card_id` et `used_gift_card_amount` (au lieu de `promo_code_id` et `promo_code`). À la lecture, une session qui porte `used_gift_card_id` a `total_details.amount_discount` pour montant de carte et aucune remise de code de promotion ; sinon c'est une remise de code, comme aujourd'hui.
3. **Le prix d'une ligne se lit avant remise** (`amount_subtotal`), déjà fait pour les codes de promotion ; rien à changer.
4. **Le panier sait déjà porter un code** (`Cart.promoCodeId`, `ON DELETE SET NULL`) : la carte s'ajoute de la même façon, avec son propre attribut. Les deux attributs ne sont jamais renseignés ensemble : `ApplyPromoCode` refuse un code quand une carte est attachée, et `ApplyGiftCardToCart` refuse une carte quand un code est attaché (décision 10).
5. **Une session de commande avec carte doit expirer en 30 minutes** (`expires_at`, le minimum de Stripe) : le solde est réservé pour ce temps. Sans carte, la session garde son expiration par défaut (24 h).
6. **`SettleOrders` ne doit jamais prendre une session d'achat de carte pour une commande** (décision 16) ; la tâche ignore déjà les sessions sans panier, ce que l'absence de `client_reference_id` garantit.
7. **`StripeFetchedCheckouts` ne lit que `paid`** : `no_payment_required` doit passer pour payé (décision 13).

## Vocabulaire

| Terme | Sens |
|---|---|
| Carte cadeau (`GiftCard`) | Un montant acheté, avec un code, un solde, une date de validité et un état. |
| Code | Texte aléatoire de 12 caractères (alphabet sans caractères ambigus), saisi au panier. Il n'est jamais dans une URL de page publique. |
| Jeton | Texte aléatoire de 32 octets, qui ouvre la page imprimable de la carte (comme les liens de réservation d'atelier). |
| Mouvement | Une ligne du journal d'une carte : émission, réservation du solde, utilisation, libération, annulation, recrédit. Le solde est la somme des mouvements. |

## États d'une carte

`PENDING_PAYMENT` (créée, en attente du paiement) → `ACTIVE` (payée, utilisable) → `EXPIRED` (au bout d'un an) ou `VOID` (annulée par l'administration). Une carte `PENDING_PAYMENT` qui n'est jamais payée est supprimée par une tâche.

## Parcours : acheter une carte

### 1. Page d'achat — `GET /gift-cards`

Montants proposés en boutons et champ libre (10 à 200 €) ; prénom et e-mail de la personne qui achète (l'e-mail où arrivera la carte). Le texte rappelle la validité d'un an et que la carte s'utilise dans la boutique en ligne. Page indexable, ajoutée au sitemap.

### 2. Paiement — `POST /gift-cards`

Crée la carte en `PENDING_PAYMENT` et une session Stripe Checkout pour son montant, qui **expire au bout de 30 minutes** (le minimum de Stripe) pour qu'une session abandonnée se ferme d'elle-même, puis redirige vers Stripe. L'identifiant de la session est gardé sur la carte ; la session porte la métadonnée `gift_card_id`, **pas de `client_reference_id`** (décision 16), ni adresse ni téléphone à recueillir. Le champ piège à robots et la limite par IP des formulaires de réservation s'appliquent.

### 3. Retour de la personne — `GET /gift-cards/success`

Le site interroge Stripe avec l'identifiant de la session. Si le paiement est confirmé, la carte passe en `ACTIVE`, son code est généré, le premier mouvement (« émission », montant initial) est écrit et l'e-mail est envoyé. La page affiche « Merci, votre carte arrive par e-mail » ; si Stripe n'a pas encore confirmé, elle le dit et la tâche de rattrapage prend le relais. Aucun code n'est affiché ici. Le traitement est **idempotent** : recharger la page ne crée rien de plus et n'envoie pas deux e-mails.

### 4. Rattrapage — tâche `SettleGiftCards` (toutes les 5 minutes)

Pour les cartes `PENDING_PAYMENT` créées depuis moins de 24 h, la tâche interroge Stripe et active celles dont la session est payée (même traitement qu'au retour de la personne). Elle renvoie aussi les e-mails de cartes actives qui ne sont pas partis, libère le solde réservé par une session de commande expirée et supprime les cartes jamais payées passé 24 h. Une personne qui ferme l'onglet après avoir payé reçoit donc son code au plus tard 5 minutes plus tard. Un CronJob et un service du profil `jobs`, comme les trois tâches des commandes.

### 5. Remise — e-mail et page imprimable

- **E-mail « Votre carte cadeau »** à la personne qui a acheté, envoyé dès l'activation : montant, code, validité, lien vers la page imprimable. Il sert aussi de reçu. Un envoi qui échoue est retenté par la tâche `SettleGiftCards`, comme les e-mails de commande.
- **Page imprimable** — `GET /gift-cards/{token}` : montant, code, validité, solde restant, avec une feuille de style d'impression. Pas de PDF au départ.

## Parcours : utiliser une carte

### Au panier — `POST /cart/gift-card`, `DELETE /cart/gift-card`

La personne saisit son code, sous celui des codes de promotion. Si le panier porte un code de promotion, la saisie est refusée avec le message de la décision 10 (et un code de promotion est refusé de la même façon quand une carte est attachée). Le site vérifie qu'il est valide (carte `ACTIVE`, non expirée, solde positif) et affiche le montant déduit et le reste à payer. Un code faux ou expiré donne le même message (« Ce code n'est pas valable »), sans révéler lequel. Les essais sont limités par IP (10 par heure) : un code de 12 caractères aléatoires n'est pas devinable, mais la limite empêche d'essayer.

Le montant déduit suit les décisions 10 et 12 : `min(solde, bijoux)`, la livraison n'est jamais déduite, ajusté pour laisser 0 € ou au moins 0,50 € à payer. Une seule carte par commande. Comme le code de promotion, la carte reste attachée au panier même quand elle ne s'applique plus, avec la raison affichée ; elle est détachée au paiement confirmé.

### Au paiement

1. **Réservation du solde** : à la création de la session Stripe, le montant déduit est réservé sur la carte (mouvement « réservé »), pour 30 minutes, et la session expire en 30 minutes. Deux paiements en même temps avec la même carte ne peuvent pas dépasser le solde.
2. **Stripe reçoit le reste à payer** : un coupon à usage unique du montant de la carte (décision 11).
3. **À la confirmation du paiement** (`ConfirmCheckoutSession` au retour de la cliente, `SettleOrders` par la tâche), la commande est créée avec sa part de carte et la réservation devient une utilisation (mouvement « utilisé »). Chacun de ces deux cas d'usage l'écrit lui-même (un cas d'usage n'en injecte pas un autre) ; la règle est une méthode du domaine, `GiftCard`.
4. **Session abandonnée ou expirée** : la réservation est libérée (mouvement « libéré ») par la tâche `SettleGiftCards`.

### Panier entièrement couvert

Décision 13 : quand la carte couvre tous les bijoux et que la livraison est offerte, la session Stripe est créée avec un coupon qui couvre tout (total 0 €) et se conclut sans paiement. La commande est enregistrée comme les autres, la carte est débitée à la confirmation. Il n'y a **aucun chemin de commande sans Stripe** à écrire. La seule condition est de lire `no_payment_required` comme un paiement terminé.

## Administration

- **Liste des cartes** : code (masqué sauf les 4 derniers caractères), montant initial, solde, état, date d'achat, validité, e-mail de l'acheteuse.
- **Détail** : journal des mouvements, bouton « Annuler la carte » (état `VOID`, avec confirmation), bouton « Renvoyer l'e-mail », bouton **« Recréditer »** (montant et motif, mouvement « recrédité », décision 15) et les commandes qui ont utilisé la carte.
- **Émission manuelle** : créer une carte offerte sans paiement (geste commercial, lot pour un concours), avec un motif qui s'écrit dans le journal, et l'e-mail de la personne qui la reçoit.
- **Commande** : la ligne « Carte cadeau » avec un lien vers la carte, et le rappel de recréditer à l'annulation.

## E-mails

| E-mail | Destinataire | Quand |
|---|---|---|
| Votre carte cadeau (reçu, code, lien imprimable) | Personne qui a acheté | À l'activation de la carte |
| Confirmation de commande et nouvelle commande | Cliente, EOSA | Existants : une ligne « Carte cadeau » s'ajoute quand la carte a payé une part |

Mêmes règles d'écriture que les autres e-mails ([e-mails](../tech/emails.md)). Le code apparaît dans l'e-mail ; la page imprimable passe par le jeton.

## Règles métier

- Le montant se calcule en centimes entiers côté serveur ; le montant d'une demande n'est jamais pris tel quel d'un champ caché.
- Le solde ne peut jamais être négatif, et jamais supérieur au montant initial plus les recrédits.
- Une carte expirée ou annulée ne se réactive pas. Pas de prolongation automatique.
- La carte est au porteur : qui saisit un code valable l'utilise.
- Les cartes ne s'échangent contre rien d'autre que des bijoux : ni espèces, ni atelier.
- Une commande annulée ne rend pas la part de la carte toute seule : l'administratrice recrédite (décision 15).
- La carte ne paie jamais la livraison (décision 10).
- Le reste à payer chez Stripe vaut 0 € ou au moins 0,50 € (décision 12).
- Les dates sont des `Instant` (UTC) ; la validité compte en jours calendaires (`Europe/Paris`).

## Persistance — migrations suivantes (`V17` et au-delà, noms à confirmer)

- `V17__gift_cards.sql` : `gift_cards` : `id`, `code` (unique), `token` (unique), `state`, `initial_amount`, `buyer_first_name`, `buyer_email`, `email_sent_at`, `expires_at`, `stripe_session_id`, `created_at` ; `gift_card_movements` : `id`, `gift_card_id`, `kind` (`ISSUED`, `RESERVED`, `USED`, `RELEASED`, `VOIDED`, `RESTORED`), `amount`, `checkout_session_id`, `reason`, `created_at`.
- À l'étape 4 : `carts.gift_card_id` (détaché si la carte est supprimée) et `orders.gift_card_id`, `orders.gift_card_amount` (`NUMERIC(10,2)`, zéro par défaut).
- Un montant est un `NUMERIC(10,2)`, jamais un flottant.

## Organisation du code

Même architecture que les ateliers et les commandes ([architecture](../tech/architecture.md)) : un `GiftCardProvider` limité à `readIds(filtres…)`, `read`, `write`, `delete` (le journal est un enfant de la carte, écrit avec elle, comme les lignes d'un panier) ; un cas d'usage par action, avec son `Result` ; un service statique pour ce qui croise la carte, le panier et le code de promotion (montant à déduire, comme `PromoDiscounts`) ; un seul cas d'usage par handler.

| Cas d'usage | Rôle |
|---|---|
| `PurchaseGiftCard` | Crée la carte `PENDING_PAYMENT` et la session Stripe |
| `ConfirmGiftCardPayment` | Au retour de la personne : interroge Stripe, active la carte, écrit l'émission, envoie l'e-mail |
| `SettleGiftCards` | Tâche : active les cartes payées dont la personne n'est pas revenue, renvoie les e-mails non partis, libère les soldes réservés par une session expirée, supprime les cartes jamais payées |
| `ReadGiftCard` | Page imprimable, par jeton |
| `ApplyGiftCardToCart` | Valide un code, calcule la déduction |
| `RemoveGiftCardFromCart` | Retire la carte du panier |
| `ListGiftCards`, `ReadGiftCardMovements`, `VoidGiftCard`, `IssueGiftCardByAdmin`, `ResendGiftCard`, `RestoreGiftCardBalance` | Administration |

Cas d'usage existants modifiés à l'étape 4 : `ApplyPromoCode` (refus quand une carte est attachée), `ReadCart` et `InitiateCheckout` (carte, déduction, réservation, coupon), `ConfirmCheckoutSession` et `SettleOrders` (part de carte sur la commande, utilisation du solde), `CancelOrder` (rappel).

L'activation d'une carte est écrite dans `ConfirmGiftCardPayment` **et** dans `SettleGiftCards` : un cas d'usage n'en injecte pas un autre. Ce qui se partage est le contenu de l'e-mail, dans un service statique (`GiftCardMails`). Pour interroger Stripe, la piste est de réutiliser `CheckoutProvider` (une ligne « Carte cadeau » sans bijou) plutôt que d'ajouter un provider ; à confirmer à l'étape 2.

## Configuration

| Variable | Rôle |
|---|---|
| `EOSA_SETTLE_GIFT_CARDS_TASK` | Active la tâche `SettleGiftCards`, lancée toutes les 5 minutes par un CronJob |
| `EOSA_GIFT_CARD_AMOUNTS` | Montants proposés, séparés par des virgules (`25,50,100`) |
| `EOSA_GIFT_CARD_MIN_AMOUNT`, `EOSA_GIFT_CARD_MAX_AMOUNT` | Bornes du montant libre (`10`, `200`) |
| `EOSA_GIFT_CARD_VALIDITY_DAYS` | Validité en jours (`365`) |

À ajouter à `.env.example`, `compose.yaml`, aux manifestes Kubernetes (`kubernetes/order-cronjobs.yaml` ou un fichier voisin) et au [déploiement](../ops/deployment.md).

## Sujets juridiques et fiscaux à traiter

À faire valider avant la mise en ligne (conseil juridique ou comptable) ; ce document ne tranche aucun de ces points.

- [ ] **Validité** : durée à afficher sur la carte et dans les CGV ; règles applicables aux cartes cadeau pour un consommateur.
- [x] **TVA** : EOSA est en franchise en base de TVA (« TVA non applicable, art. 293 B du CGI », déjà dans les mentions légales) : aucune TVA n'est due ni à la vente de la carte, ni à son utilisation, et la distinction bon à usage unique ou à usages multiples ne s'applique pas (réponse du 17 novembre 2026). Reste, pour la **facture** : la mention de franchise à y porter, et la façon d'y faire apparaître la part payée par carte (moyen de paiement) ; le site n'émet aujourd'hui aucune facture.
- [ ] **Droit de rétractation** : application de la rétractation de 14 jours à l'achat d'une carte à distance.
- [ ] **CGV** : un article « Cartes cadeau » (validité, non remboursable en espèces, solde, carte au porteur, perte ou transmission du code, usage de la boutique en ligne uniquement, pas de cumul avec un code de promotion, bijoux seulement (jamais la livraison), commande annulée : la part de la carte est recréditée par EOSA).
- [ ] **Données personnelles** : seule l'acheteuse est connue (prénom, e-mail) ; durée de conservation (proposée : cartes soldées ou expirées supprimées 5 ans après, pour la comptabilité) ; mention dans la politique de confidentialité et le registre des traitements.
- [ ] **Comptabilité** : suivi des cartes vendues non encore utilisées (dette envers les porteurs) ; export de la liste.

## Tests en local

- **Paiement** : Stripe en mode test, avec la clé de test et la carte de test `4242 4242 4242 4242`.
- **Total à 0 €** : une carte de solde supérieur aux bijoux, avec la livraison offerte, parcours complet dans le navigateur ; la session doit se conclure sans carte bancaire et la commande se créer.
- **E-mails** : Mailpit.
- **Rattrapage** : payer puis fermer l'onglet avant le retour, puis `docker compose run --rm <job>` pour lancer la tâche `SettleGiftCards` à la main.

## Étapes de mise en place

Chaque étape est une pull request qui garde le site fonctionnel.

### Étape 0 — Prérequis (hors code)

- [x] Valider ce document : recrédit manuel à l'annulation (validé le 16 novembre 2026), carte limitée aux bijoux et **pas de cumul avec un code de promotion** (demandés le 16 novembre 2026 ; le cumul validé d'abord est abandonné).
- [ ] Confirmer la validité (1 an) et les bornes (10 à 200 €).
- [ ] Lancer les points juridiques et fiscaux ci-dessus.

Rien à régler du côté de Stripe : la clé secrète déjà utilisée pour le panier suffit.

### Étape 1 — Carte, journal et administration

Domaine, persistance (`V17`), `GiftCardProvider`, cas d'usage d'administration (liste, détail, émission manuelle, annulation, recrédit). Aucun achat en ligne. Permet de vérifier le modèle et le journal.

**Fait** : le solde est la somme du journal ; les montants du journal sont positifs et le type donne le sens (`ISSUED`, `RELEASED`, `RESTORED` ajoutent ; `RESERVED`, `VOIDED` retirent ; `USED` ne change rien, le solde ayant baissé à la réservation). L'expiration n'est pas un état enregistré : elle se déduit de `expires_at` (états `PENDING_PAYMENT`, `ACTIVE`, `VOID`). L'émission manuelle borne le montant à 1 000 € et demande un motif ; le recrédit n'est possible que sur une carte active et non expirée. Le détail affiche le code en clair (la liste le masque). Deux éléments passent à l'étape suivante : « Renvoyer l'e-mail » (l'e-mail de la carte arrive à l'étape 2) et les commandes qui ont utilisé la carte (elles ne portent pas encore la carte, `V19`, étape 4). `EOSA_GIFT_CARD_VALIDITY_DAYS` (365 par défaut) fixe la validité.

### Étape 2 — Achat d'une carte

Page d'achat, session Stripe (expiration à 30 minutes, sans panier ni adresse), activation au retour de la personne, e-mail « Votre carte cadeau », page de retour, page imprimable. Vérifier que `SettleOrders` ignore la session.

**Fait** : `PurchaseGiftCard`, `ConfirmGiftCardPayment` et `ReadGiftCardByToken`, les pages `GET`/`POST /gift-cards`, `/gift-cards/success` et `/gift-cards/{token}`, l'e-mail `gift-card` (service `GiftCardMails`). La session passe par `CheckoutProvider` (`Checkout.giftCardId`, `Checkout.expiresAt`) et une méthode de création à part chez Stripe (`postGiftCard`), sans `client_reference_id` : `SettleOrders` l'ignore comme toute session sans panier. Vérifié chez Stripe en mode test : une ligne de 50 €, métadonnée `gift_card_id`, ni livraison, ni adresse, ni téléphone. Écarts et choix :
- la session expire à **31 minutes** (Stripe exige au moins 30 minutes après la création, la minute de marge couvre la latence) ;
- le code et le jeton sont générés **à la création** de la carte en attente (colonnes uniques non nulles) et ne sont révélés qu'à l'activation ; la validité compte depuis le **paiement confirmé** (`expires_at` est recalculée à l'activation) ;
- le montant libre est un **nombre entier d'euros** ; la carte n'est activée que si le montant payé chez Stripe égale celui de la carte et si la session est bien celle enregistrée sur elle ;
- la page d'achat est **non indexée, hors sitemap et sans lien** depuis le site tant que les textes (étape 6) et les points juridiques ne sont pas validés ;
- la page imprimable porte aussi les en-têtes du site à l'impression ; une feuille de style d'impression dédiée reste à faire si elle gêne.
Le paiement abouti n'est pas détecté si la personne ferme l'onglet avant le retour : c'est l'étape 3. En attendant, la carte reste en attente.

### Étape 3 — Rattrapage

Tâche `SettleGiftCards` (cartes payées sans retour, e-mails non partis, cartes jamais payées), CronJob Kubernetes toutes les 5 minutes et service du profil `jobs` de `compose.yaml`. La libération des soldes réservés arrive avec l'étape 4.

**Fait** (`SettleGiftCards`, `SettleGiftCardsTask`, `kubernetes/gift-card-cronjobs.yaml`, service compose `settle-gift-cards`) : le passage parcourt les cartes. Une carte en attente est interrogée chez Stripe : payée, elle est activée (même traitement qu'au retour de la personne) et son e-mail part tout de suite ; non payée et de plus de 24 h, elle est supprimée ; une session que Stripe ne rend pas ne supprime rien (une panne ne doit pas faire perdre une carte payée) et compte comme un échec repris au passage suivant. Une carte active dont l'e-mail n'est pas parti (y compris une carte offerte par l'administration, qui n'envoyait rien jusque-là) est renvoyée quand elle a plus de 5 minutes et moins de 7 jours. Migration `V18` : un index unique n'autorise qu'un mouvement « émission » par carte, pour que l'activation lancée en même temps par le retour de la personne et par la tâche ne double pas le solde (le second est refusé). La migration de l'étape 4 devient donc `V19`.

### Étape 4 — Utilisation au panier

Saisie du code, déduction (refus quand un code de promotion est attaché, règle des 0,50 €), réservation du solde, coupon, session de 30 minutes, part de carte sur la commande (`V19`), utilisation à la confirmation, libération à l'expiration, ligne « Carte cadeau » dans l'administration et les e-mails.

Cette étape est livrée en deux pull requests. **Première partie, faite** (panier, paiement, commande) :
- `POST`/`DELETE /cart/gift-card` (10 essais par heure et par IP, même message pour un code faux, expiré, annulé ou soldé), `ApplyGiftCardToCart`, `RemoveGiftCardFromCart`, et le refus réciproque avec un code de promotion (`ApplyPromoCode` refuse quand une carte est attachée). Le panier montre la ligne « Carte cadeau », le code masqué (jamais en clair) et le solde restant après la commande. La saisie ignore espaces, tirets et casse.
- `GiftCardDeductions` (service statique) : `min(solde, bijoux)`, et la règle des 0,50 € quand la livraison est offerte. `InitiateCheckout` réserve le solde (mouvement « réservé », relu pour détecter un dépassement par un paiement simultané : la réservation est alors rendue et le paiement s'ouvre sans carte), crée **un coupon à usage unique** du montant de la carte, ouvre une session qui **expire à 31 minutes** puis rattache la session à la réservation (seule écriture autorisée sur une ligne existante du journal). Si Stripe refuse, la réservation est rendue.
- Les métadonnées de la session sont **`used_gift_card_id`** et `used_gift_card_amount` (et non `gift_card_id`, qui reste réservé à l'**achat** d'une carte : `ConfirmGiftCardPayment` ne doit jamais prendre un panier pour un achat de carte). À la lecture, la part de la carte est la remise de la session, sans remise de code de promotion (`Checkout.usedGiftCardId`, `usedGiftCardAmount`).
- `V19` : `carts.gift_card_id` (détaché si la carte est supprimée), `orders.gift_card_id` et `orders.gift_card_amount`. La commande garde la part de la carte ; le total reste ce que Stripe a encaissé. `ConfirmCheckoutSession` et `SettleOrders` enregistrent l'utilisation (`GiftCard.markUsed`, idempotente, **avant** de créer la commande : une carte débitée dont la commande se crée au passage suivant vaut mieux qu'une commande sans sa carte débitée) et `ConfirmCheckoutSession` détache la carte du panier.
- `SettleGiftCards` rend au solde les réservations des paiements abandonnés : réservation de plus de 40 minutes dont la session n'est pas payée. Une session payée n'est jamais rendue par cette tâche (c'est `SettleOrders` qui la transforme en utilisation) ; une session que Stripe ne rend pas ne libère rien.
- Limite connue : le solde réservé par un paiement abandonné ne revient qu'au bout d'environ 40 minutes ; pendant ce temps, le panier de la même personne affiche un solde disponible réduit.

**Seconde partie, faite** (affichage, sans changer le comportement de la première) : une ligne « Carte cadeau −X € » entre le sous-total et la livraison dans l'e-mail de confirmation (HTML et texte), une mention dans l'e-mail à EOSA (le total est ce que la cliente a réglé par carte bancaire), une ligne avec le lien vers la carte dans l'administration d'une commande, et le rappel de recréditer la carte à la main : dans la section d'annulation d'une commande payée en partie par carte et sur une commande déjà annulée (`CancelOrder` n'envoie ni e-mail ni remboursement, il n'y a donc rien à y changer). La page d'une carte liste les commandes qu'elle a payées en partie (retrouvées par la session de paiement de chaque utilisation du journal : `ReadGiftCard` lit aussi les commandes).

### Étape 5 — Panier entièrement couvert

Lire `no_payment_required` comme payé : **fait avec l'étape 4** (`StripeFetchedCheckouts`, couvert par un test sur une réponse d'exemple). Le parcours à 0 € a été essayé de bout en bout en mode test dans un navigateur le 17 novembre 2026 (carte de solde supérieur aux bijoux, livraison offerte) : les commandes se créent avec un total de 0,00 €, la part de la carte et le solde débité. Reste à vérifier l'e-mail de confirmation d'une commande à 0 € (aucun paiement chez Stripe, pas de reçu Stripe) et à ajuster ce qui manque. Pas de commande sans Stripe.

### Étape 6 — Textes

CGV, FAQ, politique de confidentialité, mention dans le sitemap et les données structurées de la page d'achat.

**Fait, sans interrupteur** (décision du 17 novembre 2026 : l'offre est publique dès la mise en ligne ; un interrupteur `EOSA_GIFT_CARDS_ENABLED` avait été écrit puis retiré). La page d'achat est indexable et dans le sitemap, et trois textes apparaissent : l'**article 12 des CGV** (nature, achat, validité, utilisation bijoux seulement, carte au porteur, ni échange ni remboursement, commande annulée : part de la carte recréditée par EOSA, rétractation de 14 jours pour la part non utilisée), une **question de la FAQ** (avec le lien d'achat) et les **mentions de confidentialité** (prénom et e-mail de l'acheteuse, finalité, base contractuelle, conservation de 10 ans comme les commandes : aucune purge des cartes n'est écrite, le texte reflète donc ce que fait le site). Les montants et la validité du texte viennent de la configuration. L'article des CGV, la question et les mentions sont des **textes à relire** par EOSA et par un conseil juridique : les points juridiques et fiscaux ci-dessus ne sont pas tous tranchés. La page d'achat est liée depuis le menu (« Carte cadeau », à partir de 1024 px : la colonne de gauche de l'en-tête manque de place à 768 px), le menu mobile, le pied de page (rubrique « Boutique ») et la FAQ.
