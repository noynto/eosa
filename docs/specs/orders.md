# Spécifications : commandes

## Contexte

Une commande de bijoux n'existe aujourd'hui **que chez Stripe**. Le site fabrique une session de paiement à partir du panier, y envoie la cliente, puis, quand elle revient sur `/checkout/success`, lit l'état du paiement et vide le panier. Rien n'est enregistré dans la base :

- l'administration n'a **aucune liste de commandes** : EOSA les découvre dans le tableau de bord Stripe ;
- la cliente ne reçoit **aucun e-mail du site** (seulement un éventuel reçu Stripe, selon son réglage) ;
- les textes du site promettent pourtant un e-mail avec un lien de suivi dès l'expédition (page Livraison, FAQ), ce qui n'existe pas ;
- si la cliente ferme l'onglet après avoir payé sans revenir sur le site, son panier reste plein et le site ne sait même pas qu'elle a payé ;
- l'adresse de livraison et le téléphone sont demandés par Stripe, mais le site ne les lit pas.

L'objectif est de **modéliser la commande** : l'enregistrer au paiement, la montrer à l'administration, suivre sa préparation et son expédition, et prévenir la cliente. Ce document est un préalable à la carte cadeau (voir la spécification de la carte cadeau, étape « panier entièrement couvert »), à l'étiquette Mondial Relay et au suivi de livraison, trois sujets de `TODO.md` qui supposent qu'une commande existe.

## Décisions proposées

| # | Sujet | Décision |
|---|---|---|
| 1 | Source de vérité | La **commande est enregistrée dans la base** du site. Stripe reste le moyen de paiement ; la commande garde l'identifiant de la session Stripe. |
| 2 | Création | À la **confirmation du paiement**, jamais avant : pas de commande « en attente » dans la base. La session Stripe reste l'objet d'attente. |
| 3 | Pas de webhook | Comme pour la carte cadeau : la commande est créée au retour de la cliente sur le site (le site interroge Stripe) et **rattrapée par la tâche `SettleOrders`** pour les paiements sans retour. Rien à configurer chez Stripe. |
| 4 | Idempotence | Une session Stripe donne **au plus une commande** (contrainte d'unicité sur `stripe_session_id`). Recharger la page de succès ou relancer la tâche ne crée rien de plus. |
| 5 | Photographie | La commande **copie** ce qui a été acheté : nom, prix unitaire, quantité, image, montant de livraison. Modifier ou archiver un bijou n'altère jamais une commande passée. |
| 6 | Numéro | Chaque commande a un **numéro lisible** (`EOSA-2026-0001`), montré à la cliente et dans l'administration. L'identifiant interne reste un UUID. |
| 7 | États | `PAID` (à fabriquer) → `BUILT` (fabriquée, prête à expédier) → `SHIPPED` → `DELIVERED`, ou `CANCELLED`. Chaque pièce est **fabriquée à la commande** : `PAID` couvre l'attente et la fabrication, `BUILT` marque la pièce terminée. Voir le cycle de vie. |
| 8 | Expédition | L'administratrice marque la commande « expédiée » en saisissant le **numéro de suivi** Mondial Relay. La cliente reçoit alors un e-mail avec le lien de suivi. L'étiquette est créée sur le site de Mondial Relay au début, puis par l'API plus tard (étape 5). |
| 9 | E-mails | À la cliente : **confirmation de commande** puis **expédition avec suivi**. À EOSA (`contact@eosa.me`) : **nouvelle commande à fabriquer**. Envoyés depuis `contact@eosa.me`. |
| 10 | Remboursement | Fait **dans le tableau de bord Stripe**. L'administratrice marque ensuite la commande « annulée » avec un motif libre. Pas de remboursement depuis le site en V1. |
| 11 | Compte cliente | Toujours **aucun compte** : la commande est liée à l'e-mail saisi chez Stripe. La cliente suit son colis par le lien de l'e-mail. |
| 12 | Périmètre | Les ateliers (sans paiement en ligne) et les cartes cadeaux (leur propre parcours) ne créent **pas** de commande de bijoux. |

## Ce que le code actuel impose

1. **Le paiement n'est lu qu'au retour de la cliente.** `ConfirmCheckoutSession` lit la session Stripe et vide le panier, sans rien écrire. Créer la commande à cet endroit couvre le cas normal. Les paiements dont la cliente n'est pas revenue (onglet fermé, coupure réseau) sont rattrapés par la tâche `SettleOrders`, qui **liste les sessions Stripe récemment payées** (nouveauté pour `CheckoutProvider`, qui ne sait aujourd'hui lire qu'une session connue). À vérifier à l'implémentation : le filtre de statut et de date de l'API de liste des sessions de Stripe.
2. **Stripe renvoie plus que ce que le site lit.** Le site lit l'e-mail et le nom (`customer_details`) mais ni le téléphone, ni l'adresse de livraison (`shipping_details`), ni le total (`amount_total`), ni le montant de la livraison. Il faut élargir la lecture.
3. **Le panier n'est vidé qu'au retour.** La tâche de rattrapage **ne touche pas au panier** : la cliente a pu le remplir de nouveau, on ne le vide pas à sa place. Limite connue : une cliente qui paie, ne revient pas et garde un panier plein peut payer deux fois ; la deuxième commande est alors une vraie commande.
4. **Aucun stock.** Un bijou n'a pas de quantité disponible : le site vend sans limite. À confirmer (voir points ouverts).
5. **Les règles croisent des domaines** (panier, paiement, commande) : la construction d'une commande à partir d'un paiement confirmé est un **service statique** (`service/`), et les deux cas d'usage qui créent une commande (retour de la cliente, tâche) l'appellent, puisqu'un cas d'usage n'en injecte jamais un autre.

## Vocabulaire

| Terme | Rôle |
|---|---|
| `Order` | Commande payée : numéro, état, cliente (nom, e-mail, téléphone), adresse de livraison, lignes, montants, dates. |
| `OrderItem` | Ligne copiée au paiement : nom complet (avec couleur de métal et breloques), prix unitaire, quantité, bijou et image d'origine (facultatifs). |
| `OrderState` | `PAID` \| `BUILT` \| `SHIPPED` \| `DELIVERED` \| `CANCELLED`. |
| `OrderShipment` | Expédition : transporteur (Mondial Relay), numéro de suivi, date d'expédition. |
| `OrderProvider` | `readIds(filtres)` (état, recherche par numéro ou e-mail), `read`, `write`, `delete`. |

## Cycle de vie

```
paiement Stripe     confirmation          pièce terminée (admin)   expédition (admin)       livraison
      │             (retour ou tâche)              │                       │                      │
      │                    │                       ▼                       ▼                      ▼
      └──────────────────  PAID  ───────────────  BUILT  ───────────────  SHIPPED  ───────────  DELIVERED
                             │                       │                     │
                             └───────────────────────┴──── CANCELLED ◄─────┘   (remboursée dans Stripe, motif noté)
```

- `PAID → BUILT` : l'administratrice clique « Pièce fabriquée » quand la pièce est terminée. Aucun e-mail à la cliente : le suivant part à l'expédition.
- `BUILT → SHIPPED` : la pièce est emballée ; l'administratrice saisit le numéro de suivi. Pas de retour en arrière en V1 ; une erreur de numéro se corrige sur la commande expédiée.
- `SHIPPED → DELIVERED` : à la main en V1 (bouton), automatique avec le suivi Mondial Relay à l'étape 7.
- `CANCELLED` : depuis `PAID`, `BUILT` ou `SHIPPED`, avec un motif. Une commande annulée n'envoie plus d'e-mail.
- Une commande de plusieurs pièces reste une seule commande : elle passe `BUILT` puis `SHIPPED` d'un bloc (`BUILT` quand toutes les pièces sont terminées), un seul colis. Suivre la fabrication pièce par pièce n'est pas prévu.

## Parcours : payer et être enregistrée

1. La cliente paie chez Stripe et revient sur `GET /checkout/success?session_id=…`.
2. Le site lit la session. Si le paiement est confirmé, il **crée la commande** (si elle n'existe pas déjà pour cette session), vide le panier, envoie les e-mails, et affiche la page de succès avec le **numéro de commande** et le récapitulatif (aujourd'hui sans numéro).
3. Si le paiement n'est pas confirmé (en attente, abandonné, expiré), la page le dit, sans créer de commande.
4. La tâche `SettleOrders` (toutes les 5 minutes) fait le même travail pour les sessions payées dont personne n'est revenu, et **renvoie les e-mails qui ne sont pas partis**. Elle ne regarde que les sessions des dernières 48 heures.

Le numéro de commande est attribué par une séquence en base au moment de l'écriture, pour qu'il n'y ait ni doublon ni trou dû à la course entre le retour de la cliente et la tâche.

## Parcours : préparer et expédier (administration)

| Écran | Contenu |
|---|---|
| `GET /orders` (liste) | Commandes, les plus récentes d'abord, filtre par état (par défaut : **à fabriquer** `PAID` et **fabriquées** `BUILT`), recherche par numéro ou e-mail. Pastille du nombre de commandes `PAID` (à fabriquer) dans le menu. |
| `GET /orders/{id}` (détail) | Numéro, date, cliente (nom, e-mail, téléphone), adresse de livraison **copiable** pour l'étiquette, lignes avec image, sous-total, livraison, total, état, expédition, journal des e-mails. |
| `POST /orders/{id}/build` | Passage à `BUILT`. Le bouton ne s'affiche que pour une commande `PAID`. |
| `POST /orders/{id}/ship` | Saisie du numéro de suivi, passage à `SHIPPED`, e-mail d'expédition. Le bouton ne s'affiche que pour une commande `BUILT`. |
| `POST /orders/{id}/deliver` | Passage à `DELIVERED`. |
| `POST /orders/{id}/cancel` | Annulation avec motif, sans e-mail automatique. |

Les écrans suivent les composants de l'administration et se chargent en fragments htmx (la liste, le détail et ses actions sont des appels séparés), comme pour les ateliers.

## E-mails

| E-mail | Destinataire | Quand | Contenu |
|---|---|---|---|
| Confirmation de commande | Cliente | À la création | Numéro, lignes, montants, adresse de livraison, **délai de fabrication puis d'expédition** (à fixer, voir points ouverts), coordonnées de contact. |
| Nouvelle commande | `contact@eosa.me` | À la création | Numéro, nombre d'articles, total, lien vers la commande dans l'administration. |
| Commande expédiée | Cliente | Au passage à `SHIPPED` | Numéro, **lien de suivi** Mondial Relay, adresse. |

Les e-mails suivent les règles de [`docs/tech/emails.md`](../tech/emails.md). Chaque envoi est tracé sur la commande (`confirmation_sent_at`, `notification_sent_at`, `shipping_sent_at`) ; un envoi qui échoue est repris par `SettleOrders` sans jamais être doublé. Le lien de suivi se construit à partir du numéro de suivi (adresse publique de suivi de Mondial Relay, à vérifier à l'implémentation) : le site ne stocke pas l'adresse complète.

## Règles métier

- Une commande n'est créée que pour une session Stripe dont le paiement est **confirmé** (`paid`).
- Le total de la commande est celui **payé chez Stripe** (`amount_total`), pas un recalcul : si les règles de livraison changent, les commandes passées ne bougent pas.
- Les lignes sont des copies ; supprimer un bijou ne supprime pas sa ligne (`jewel_id` devient `null`, l'image reste si elle existe encore).
- L'adresse de livraison vient de Stripe (pays autorisés : FR, BE, CH, LU, comme dans la session de paiement).
- Une commande `SHIPPED` a toujours un numéro de suivi ; une commande `PAID` ou `BUILT` n'en a jamais.
- Les transitions inverses ne sont pas permises ; seules `CANCELLED` et la correction du numéro de suivi sortent du flux.
- Les montants sont des `BigDecimal` en euros à deux décimales, jamais des `double`.

## Persistance (migration à numéroter à l'implémentation)

Les migrations Flyway du dépôt s'arrêtent à `V5`. Trois sujets en attente veulent la suivante (commandes, carte cadeau, ordre des nouveautés) : on prend le numéro libre au moment d'écrire la migration.

| Table | Colonnes |
|---|---|
| `orders` | `id` (UUID), `number` (texte unique, issu d'une séquence), `state`, `stripe_session_id` (unique), `customer_name`, `customer_email`, `customer_phone`, `shipping_name`, `shipping_line1`, `shipping_line2`, `shipping_postal_code`, `shipping_city`, `shipping_country`, `items_amount`, `shipping_amount`, `total_amount`, `carrier`, `tracking_number`, `cancel_reason`, `paid_at`, `built_at`, `shipped_at`, `delivered_at`, `cancelled_at`, `confirmation_sent_at`, `notification_sent_at`, `shipping_sent_at`, `created_at` |
| `order_items` | `id`, `order_id` (clé étrangère, suppression en cascade), `position`, `name`, `unit_price`, `quantity`, `jewel_id` (peut être nul), `image_id` (peut être nul) |

Index sur `state` et `created_at` pour la liste, sur `customer_email` pour la recherche.

## Organisation du code

| Élément | Rôle |
|---|---|
| `order/` (domaine) | `Order`, `OrderItem`, `OrderState`, `OrderProvider`, identifiants dans `shared/`. |
| `JdbcPersistedOrders` | Implémentation JDBC du provider. |
| `CheckoutProvider` | Élargi : lecture du téléphone, de l'adresse de livraison, du total ; `readIds` accepte une borne de date pour lister les sessions payées récentes. |
| `service/OrdersFromCheckouts` | Service statique : construit une `Order` à partir d'un `Checkout` payé (copie des lignes, adresse, montants), sans provider. |
| `service/OrderMails` | Service statique : contenu des trois e-mails. |
| `ConfirmCheckoutSession` | Existant : crée la commande si elle n'existe pas, vide le panier, envoie les e-mails. `Result` avec le numéro. |
| `SettleOrders` | Nouvelle tâche : rattrape les paiements sans retour, renvoie les e-mails non partis. Même règle de création que `ConfirmCheckoutSession` (dupliquée, via le service statique). |
| `ReadOrders`, `ReadOrder` | Lecture pour l'administration (filtres, détail). |
| `BuildOrder`, `ShipOrder`, `DeliverOrder`, `CancelOrder` | Actions de l'administration, un cas d'usage chacune. |
| Handlers admin | Un handler par route, un seul cas d'usage chacun. |

## Configuration

| Variable | Rôle |
|---|---|
| `EOSA_SETTLE_ORDERS_TASK` | Active la tâche `SettleOrders` (CronJob toutes les 5 minutes, service compose `settle-orders` du profil `jobs`). |

La clé secrète Stripe existante suffit. Le CronJob s'ajoute dans `kubernetes/` à côté de ceux des ateliers, et la tâche dans [`docs/tech/scheduled-tasks.md`](../tech/scheduled-tasks.md).

## Tests en local

Un vrai paiement n'est pas testable en local sans clé de test Stripe. Les tests automatiques couvrent : création idempotente (deux appels, une commande), session non payée sans commande, copie des lignes, numérotation, transitions d'état valides et refusées, rattrapage par la tâche, e-mail non doublé après un échec. Une vérification manuelle avec une clé `sk_test_` et la carte de test Stripe reste à faire une fois : payer, fermer l'onglet avant le retour, lancer la tâche, vérifier la commande et les e-mails dans Mailpit.

## Points ouverts

1. **Pièces uniques ou reproductibles ?** Sans stock, deux clientes peuvent acheter le « même » bijou. Si certaines pièces sont uniques, il faut une quantité disponible, décrémentée à la commande (et gérée entre l'ajout au panier et le paiement). Hors périmètre ici ; à trancher.
2. **Facture.** Selon le régime d'EOSA, une facture ou un justificatif doit être remis. Le reçu Stripe n'est pas une facture. À confirmer avant l'étape 4.
3. **Transporteur et mode de livraison.** Le site parle de livraison à domicile Mondial Relay ; l'API de Mondial Relay (création d'étiquette) demande un contrat et des identifiants à obtenir (étape 0).
4. **Commandes à l'étranger.** Les pays autorisés sont FR, BE, CH et LU ; la Suisse est hors Union européenne (douane). À confirmer.
6. **Délai annoncé.** Avec une fabrication à la commande, quel délai afficher (par exemple « fabriqué sous X jours ouvrés, expédié sous 48 h ») ? Il remplace le « expédié sous 48h » actuel dans la page Livraison, la FAQ, le bandeau, les CGV et l'e-mail de confirmation. Il change aussi le point 1 : si tout est fabriqué à la commande, il n'y a pas de stock à gérer, seulement une charge de travail.
7. **Annulation par la cliente.** Pas de parcours en V1 : elle écrit à EOSA, qui rembourse dans Stripe et marque la commande annulée. Une pièce déjà fabriquée (`BUILT`) ou en cours de fabrication peut ne plus être annulable librement (pièce personnalisée) : à ajouter aux CGV.

## Sujets juridiques et fiscaux à valider

- [ ] **Durée de conservation des commandes** : les pièces comptables se conservent 10 ans (Code de commerce), mais l'adresse et le téléphone n'ont pas besoin de rester aussi longtemps ; proposition à valider : garder les montants et les lignes, effacer l'adresse et le téléphone après un délai à fixer (par exemple 1 an après la livraison, pour couvrir retours et garantie), par une tâche de purge comme pour les ateliers.
- [ ] **Politique de confidentialité** : ajouter le traitement « commandes » (données, finalités, base légale : exécution du contrat, destinataires : Stripe, Mondial Relay, hébergeur, Infomaniak).
- [ ] **Registre des traitements** : la fiche « commandes » existe en brouillon ; la compléter avec les nouvelles données (adresse, téléphone, numéro de suivi).
- [ ] **CGV et pages Livraison, Retours, FAQ** : vérifier qu'elles décrivent bien les e-mails réellement envoyés (confirmation, expédition avec suivi) et le délai. **Le texte actuel promet une expédition sous 48 heures ouvrées** (page Livraison, FAQ, bandeau « expédié sous 48h ») : il n'est plus vrai si chaque pièce est fabriquée après la commande. À remplacer par un délai de fabrication plus d'expédition (voir point ouvert 6).
- [ ] **Droit de rétractation** : 14 jours après réception ; vérifier le parcours de retour et de remboursement avec le suivi de livraison (la date de réception vient de `DELIVERED`).
- [ ] **Facture** : voir point ouvert 2.

## Étapes

Chaque étape est une pull request ; la boutique reste utilisable entre deux.

| # | Étape | Contenu |
|---|---|---|
| 0 | Prérequis (hors code) | Trancher les points ouverts 1 à 4 et 6 ; compte et contrat Mondial Relay si l'étiquette est automatisée. |
| 1 | Commande enregistrée | Migration, `Order`, `OrderProvider`, lecture élargie de Stripe, création dans `ConfirmCheckoutSession`, numéro affiché sur la page de succès. Aucune administration encore. |
| 2 | Administration | `ReadOrders`, `ReadOrder`, écrans liste et détail, pastille « à préparer ». |
| 3 | Rattrapage | `SettleOrders`, liste des sessions payées dans `CheckoutProvider`, CronJob, service compose, e-mails renvoyés. |
| 4 | E-mails | Confirmation à la cliente, nouvelle commande à EOSA (envoyés à la création et par la tâche). |
| 5 | Fabrication et expédition | `BuildOrder`, `ShipOrder` avec numéro de suivi, e-mail avec lien de suivi, `DeliverOrder`, `CancelOrder`. |
| 6 | Étiquette Mondial Relay | Création de l'étiquette depuis la commande par l'API, numéro de suivi rempli automatiquement. |
| 7 | Suivi de livraison | Lecture du statut chez Mondial Relay, passage automatique à `DELIVERED`. |
| 8 | Textes | Politique de confidentialité, CGV, Livraison, FAQ. |
