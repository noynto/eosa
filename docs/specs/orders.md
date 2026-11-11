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
| 7 | États | `PAID` (à fabriquer) → `CRAFTED` (fabriquée, prête à expédier) → `SHIPPED` → `DELIVERED`, ou `CANCELLED`. Chaque pièce est **fabriquée à la commande** : `PAID` couvre l'attente et la fabrication, `CRAFTED` marque la pièce terminée. Voir le cycle de vie. |
| 8 | Étiquette et expédition | **Passer une commande à `CRAFTED` génère l'étiquette Mondial Relay par l'API** : c'est un concept métier à part entière, l'étiquette d'expédition (`ShippingLabel`). Elle porte le **numéro de suivi** et le PDF à imprimer. Si Mondial Relay refuse ou ne répond pas, la commande **reste `PAID`** et l'erreur s'affiche : jamais de commande `CRAFTED` sans étiquette. L'administratrice imprime l'étiquette, colle le colis et le dépose (point relais ou **consigne Mondial Relay**). **Une tâche récurrente interroge l'API de Mondial Relay** : dès que le transporteur considère le colis comme déposé, la commande passe **toute seule** à `SHIPPED` et la cliente reçoit l'e-mail avec le lien de suivi. Le bouton « Colis remis » reste en secours (décision 14). |
| 9 | E-mails | À la cliente : **confirmation de commande** puis **expédition avec suivi**. À EOSA (`contact@eosa.me`) : **nouvelle commande à fabriquer**. Envoyés depuis `contact@eosa.me`. |
| 10 | Remboursement | Fait **dans le tableau de bord Stripe**. L'administratrice marque ensuite la commande « annulée » avec un motif libre. Pas de remboursement depuis le site en V1. |
| 11 | Compte cliente | Toujours **aucun compte** : la commande est liée à l'e-mail saisi chez Stripe. La cliente suit son colis par le lien de l'e-mail. |
| 12 | Périmètre | Les ateliers (sans paiement en ligne) et les cartes cadeaux (leur propre parcours) ne créent **pas** de commande de bijoux. |
| 13 | Délais annoncés | **Fabrication : 48 h au maximum** après le paiement (`PAID → CRAFTED`), puis **expédition : 24 h** (`CRAFTED → SHIPPED`), soit **72 h au plus** entre le paiement et l'envoi du colis. **Heures ouvrées** (confirmé le 11 novembre 2026), comme le texte actuel : les week-ends et les jours fériés français ne comptent pas. Une commande payée le vendredi soir est donc à fabriquer au plus tard le mardi et à expédier le mercredi (la durée ne s'écoule que du lundi au vendredi, 24 h par jour ouvré). Ces délais remplacent le « expédié sous 48h » partout. |
| 14 | Suivi automatique | La tâche **`TrackOrderShipments`** (toutes les 15 minutes) lit le statut de chaque étiquette chez Mondial Relay pour les commandes `CRAFTED` et `SHIPPED` : colis **déposé** → `SHIPPED` (date du dépôt prise chez le transporteur, e-mail d'expédition), colis **livré** → `DELIVERED`. Les boutons manuels (« Colis remis », « Livrée ») restent pour le jour où l'API ne répond pas ou tarde. |

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
| `OrderState` | `PAID` \| `CRAFTED` \| `SHIPPED` \| `DELIVERED` \| `CANCELLED`. |
| `ShippingLabel` | Étiquette d'expédition : transporteur (Mondial Relay), **numéro de suivi**, poids du colis en grammes, référence du PDF chez le transporteur, **statut du colis** (`CREATED` \| `DEPOSITED` \| `DELIVERED`, lu chez le transporteur), date de création. Appartient à la commande, qui la garde dans ses colonnes. |
| `ShippingLabelProvider` | Port vers le transporteur : `write` **crée l'étiquette chez Mondial Relay** (destinataire, poids) et rend le numéro de suivi, comme `CheckoutProvider.write` crée une session Stripe ; `read` relit l'étiquette chez le transporteur : son **statut** (déposé, livré) et le PDF pour le réimprimer. Adaptateur : `MondialRelayFetchedShippingLabels`. |
| `OrderProvider` | `readIds(filtres)` (état, recherche par numéro ou e-mail), `read`, `write`, `delete`. |

## Cycle de vie

```
paiement Stripe     confirmation          pièce terminée (admin)   colis déposé (tâche)     livraison (tâche)
      │             (retour ou tâche)       + étiquette Mondial Relay      │                      │
      │                    │                       ▼                       ▼                      ▼
      └──────────────────  PAID  ──────────────  CRAFTED  ─────────────  SHIPPED  ───────────  DELIVERED
                             │                       │                     │
                             └───────────────────────┴──── CANCELLED ◄─────┘   (remboursée dans Stripe, motif noté)
```

- `PAID → CRAFTED` : l'administratrice clique « Pièce fabriquée » quand la pièce est terminée et confirme le **poids du colis** (valeur par défaut préremplie). Le site crée l'étiquette chez Mondial Relay ; si cela réussit, la commande passe `CRAFTED` avec son numéro de suivi, sinon elle reste `PAID` et on peut réessayer. Aucun e-mail à la cliente : le suivant part à l'expédition.
- `CRAFTED → SHIPPED` : l'administratrice a imprimé l'étiquette, emballé la pièce et déposé le colis (point relais ou consigne). **La tâche `TrackOrderShipments` le constate chez Mondial Relay** et passe la commande `SHIPPED`, avec l'e-mail d'expédition. En secours, le bouton « Colis remis » fait la même transition. Pas de retour en arrière en V1.
- `SHIPPED → DELIVERED` : par la même tâche quand Mondial Relay déclare le colis livré ; en secours, bouton « Livrée ».
- Annuler une commande `CRAFTED` dont l'étiquette existe laisse une étiquette inutilisée chez Mondial Relay ; savoir si elle peut être annulée par l'API (et si elle est facturée) est à vérifier à l'étape 0.
- `CANCELLED` : depuis `PAID`, `CRAFTED` ou `SHIPPED`, avec un motif. Une commande annulée n'envoie plus d'e-mail.
- Une commande de plusieurs pièces reste une seule commande : elle passe `CRAFTED` puis `SHIPPED` d'un bloc (`CRAFTED` quand toutes les pièces sont terminées), un seul colis. Suivre la fabrication pièce par pièce n'est pas prévu.

## Parcours : payer et être enregistrée

1. La cliente paie chez Stripe et revient sur `GET /checkout/success?session_id=…`.
2. Le site lit la session. Si le paiement est confirmé, il **crée la commande** (si elle n'existe pas déjà pour cette session), vide le panier, envoie les e-mails, et affiche la page de succès avec le **numéro de commande** et le récapitulatif (aujourd'hui sans numéro).
3. Si le paiement n'est pas confirmé (en attente, abandonné, expiré), la page le dit, sans créer de commande.
4. La tâche `SettleOrders` (toutes les 5 minutes) fait le même travail pour les sessions payées dont personne n'est revenu, et **renvoie les e-mails qui ne sont pas partis**. Elle ne regarde que les sessions des dernières 48 heures.

Le numéro de commande est attribué par une séquence en base au moment de l'écriture, pour qu'il n'y ait pas de doublon entre le retour de la cliente et la tâche. Le préfixe porte l'année du paiement (fuseau `Europe/Paris`) ; la séquence, elle, ne repart pas de 1 au 1er janvier. Une écriture qui trouve déjà la commande de la session (course entre le retour de la cliente et la tâche) consomme un numéro sans l'utiliser : un trou dans les numéros est possible, mais jamais un doublon.

## Parcours : préparer et expédier (administration)

| Écran | Contenu |
|---|---|
| `GET /orders` (liste) | Commandes, les plus récentes d'abord, filtre par état (par défaut : **à fabriquer** `PAID` et **fabriquées** `CRAFTED`), recherche par numéro ou e-mail. Pastille du nombre de commandes `PAID` (à fabriquer) dans le menu. Une commande **en retard** (`PAID` depuis plus de 48 h, `CRAFTED` depuis plus de 24 h) est signalée dans la liste ; l'échéance (en heures ouvrées) se calcule en Java à partir de `paid_at` et `crafted_at`. |
| `GET /orders/{id}` (détail) | Numéro, date, cliente (nom, e-mail, téléphone), adresse de livraison, lignes avec image, sous-total, livraison, total, état, étiquette (numéro de suivi, lien d'impression), journal des e-mails. |
| `POST /orders/{id}/craft` | Poids du colis, création de l'étiquette chez Mondial Relay, passage à `CRAFTED`. Le bouton ne s'affiche que pour une commande `PAID`. En cas d'échec du transporteur, l'erreur s'affiche dans la page (fragment htmx, 422) et la commande reste `PAID`. |
| `GET /orders/{id}/label` | PDF de l'étiquette, à imprimer ou réimprimer. Ne modifie rien. Disponible pour une commande `CRAFTED` ou `SHIPPED`. |
| `POST /orders/{id}/ship` | **Secours** « Colis remis » : passage à `SHIPPED` et e-mail d'expédition, sans attendre la tâche. Le bouton ne s'affiche que pour une commande `CRAFTED`. |
| `POST /orders/{id}/deliver` | Passage à `DELIVERED`. |
| `POST /orders/{id}/cancel` | Annulation avec motif, sans e-mail automatique. |

Les écrans suivent les composants de l'administration et se chargent en fragments htmx (la liste, le détail et ses actions sont des appels séparés), comme pour les ateliers.

## E-mails

| E-mail | Destinataire | Quand | Contenu |
|---|---|---|---|
| Confirmation de commande | Cliente | À la création | Numéro, lignes, montants, adresse de livraison, **délais** (fabrication sous 48 h, expédition sous 24 h, soit un envoi sous 72 h ouvrées au plus), coordonnées de contact. |
| Nouvelle commande | `contact@eosa.me` | À la création | Numéro, nombre d'articles, total, lien vers la commande dans l'administration. |
| Commande expédiée | Cliente | Au passage à `SHIPPED` (par la tâche ou le bouton de secours) | Numéro, **lien de suivi** Mondial Relay, adresse. |

Les e-mails suivent les règles de [`docs/tech/emails.md`](../tech/emails.md). Chaque envoi est tracé sur la commande (`confirmation_sent_at`, `notification_sent_at`, `shipping_sent_at`) ; un envoi qui échoue est repris par `SettleOrders` sans jamais être doublé. Le lien de suivi se construit à partir du numéro de suivi (adresse publique de suivi de Mondial Relay, à vérifier à l'implémentation) : le site ne stocke pas l'adresse complète.

## Règles métier

- Une commande n'est créée que pour une session Stripe dont le paiement est **confirmé** (`paid`).
- Le total de la commande est celui **payé chez Stripe** (`amount_total`), pas un recalcul : si les règles de livraison changent, les commandes passées ne bougent pas.
- Les lignes sont des copies ; supprimer un bijou ne supprime pas sa ligne (`jewel_id` devient `null`, l'image reste si elle existe encore).
- L'adresse de livraison vient de Stripe (pays autorisés : FR, BE, CH, LU, comme dans la session de paiement).
- Une commande `CRAFTED`, `SHIPPED` ou `DELIVERED` a toujours une étiquette, donc un numéro de suivi ; une commande `PAID` n'en a jamais.
- L'étiquette est créée **une seule fois** par commande : une commande qui en a déjà une ne peut pas être refabriquée. Si l'écriture en base échoue après la création chez Mondial Relay, une étiquette orpheline peut exister chez eux ; la création de l'étiquette n'est pas idempotente côté transporteur.
- Les transitions inverses ne sont pas permises ; seule `CANCELLED` sort du flux.
- Les montants sont des `BigDecimal` en euros à deux décimales, jamais des `double`.
- Les délais se comptent en **heures ouvrées** : un service statique `service/BusinessTime` (sans provider, l'heure en paramètre) calcule l'échéance à partir de `paid_at` ou `crafted_at` en sautant les samedis, les dimanches et les jours fériés français (dont ceux qui dépendent de Pâques), dans le fuseau `Europe/Paris`. Il sert à signaler les commandes en retard dans la liste, et peut resservir pour les textes et l'e-mail de confirmation.

## Persistance (migration à numéroter à l'implémentation)

Les migrations Flyway du dépôt s'arrêtent à `V5`. Trois sujets en attente veulent la suivante (commandes, carte cadeau, ordre des nouveautés) : on prend le numéro libre au moment d'écrire la migration.

| Table | Colonnes |
|---|---|
| `orders` | `id` (UUID), `number` (texte unique, issu d'une séquence), `state`, `stripe_session_id` (unique), `customer_name`, `customer_email`, `customer_phone`, `shipping_name`, `shipping_line1`, `shipping_line2`, `shipping_postal_code`, `shipping_city`, `shipping_country`, `items_amount`, `shipping_amount`, `total_amount`, `carrier`, `tracking_number`, `parcel_weight_grams`, `label_reference`, `label_status`, `last_tracked_at`, `cancel_reason`, `personal_data_purged_at`, `paid_at`, `crafted_at`, `shipped_at`, `delivered_at`, `cancelled_at`, `confirmation_sent_at`, `notification_sent_at`, `shipping_sent_at`, `created_at` |
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
| `MondialRelayFetchedShippingLabels` | Adaptateur du `ShippingLabelProvider` : appelle l'API de Mondial Relay (type d'API à vérifier à l'étape 0). |
| `CraftOrder` | Vérifie que la commande est `PAID`, crée l'étiquette par le `ShippingLabelProvider`, passe la commande `CRAFTED` et l'écrit. `Result` : la commande et son numéro de suivi. |
| `ReadOrderLabel` | Récupère le PDF de l'étiquette pour `GET /orders/{id}/label`. |
| `TrackOrderShipments` | Tâche : pour chaque commande `CRAFTED` ou `SHIPPED`, relit l'étiquette par le `ShippingLabelProvider` ; déposé → `SHIPPED` + e-mail ; livré → `DELIVERED`. Une commande en erreur n'arrête pas les autres. Idempotente : une commande déjà passée n'est pas retraitée, l'e-mail n'est jamais doublé (`shipping_sent_at`). |
| `ShipOrder`, `DeliverOrder`, `CancelOrder` | Actions de l'administration, un cas d'usage chacune (`ShipOrder` et `DeliverOrder` sont les secours manuels). La règle de passage à `SHIPPED` (état, date, e-mail) est la même que celle de la tâche, dupliquée par un service statique `service/OrderMails` pour l'e-mail. |
| `PurgeOrderPersonalData` | Tâche : efface adresse et téléphone 1 an après la livraison ou l'annulation (voir Configuration). |
| Handlers admin | Un handler par route, un seul cas d'usage chacun. |

## Configuration

| Variable | Rôle |
|---|---|
| `EOSA_SETTLE_ORDERS_TASK` | Active la tâche `SettleOrders` (CronJob toutes les 5 minutes, service compose `settle-orders` du profil `jobs`). |
| `EOSA_TRACK_ORDER_SHIPMENTS_TASK` | Active la tâche `TrackOrderShipments` (CronJob toutes les 15 minutes, service compose `track-order-shipments`). |
| `EOSA_PURGE_ORDER_PERSONAL_DATA_TASK` | Active la tâche de purge (CronJob chaque nuit à 3 h 45, service compose `purge-order-personal-data`). |
| `EOSA_MONDIAL_RELAY_*` | Identifiants de l'API Mondial Relay (noms exacts selon l'API retenue à l'étape 0). Secrets, jamais dans le dépôt. |
| `EOSA_SHIPPING_SENDER_*` | Adresse de l'expéditeur (EOSA) imprimée sur l'étiquette. |
| `EOSA_SHIPPING_DEFAULT_PARCEL_WEIGHT_GRAMS` | Poids du colis prérempli à la fabrication (aucun bijou n'a de poids dans le modèle). |

La clé secrète Stripe existante suffit pour les commandes. Les nouvelles variables obligatoires s'ajoutent aussi à `compose.yaml`, à `.env.example` et aux manifestes Kubernetes. Les CronJobs s'ajoutent dans `kubernetes/` à côté de ceux des ateliers, et les tâches dans [`docs/tech/scheduled-tasks.md`](../tech/scheduled-tasks.md).

## Tests en local

Un vrai paiement n'est pas testable en local sans clé de test Stripe. Les tests automatiques couvrent : création idempotente (deux appels, une commande), session non payée sans commande, copie des lignes, numérotation, transitions d'état valides et refusées, rattrapage par la tâche, e-mail non doublé après un échec. Une vérification manuelle avec une clé `sk_test_` et la carte de test Stripe reste à faire une fois : payer, fermer l'onglet avant le retour, lancer la tâche, vérifier la commande et les e-mails dans Mailpit.

## Points ouverts

1. **Pièces uniques ou reproductibles ?** Sans stock, deux clientes peuvent acheter le « même » bijou. Si certaines pièces sont uniques, il faut une quantité disponible, décrémentée à la commande (et gérée entre l'ajout au panier et le paiement). Hors périmètre ici ; à trancher.
2. **Facture.** Selon le régime d'EOSA, une facture ou un justificatif doit être remis. Le reçu Stripe n'est pas une facture. À confirmer avant l'étape 4.
3. **Mondial Relay.** La création de l'étiquette par l'API est décidée (décision 8) : il faut un contrat Mondial Relay avec accès à l'API et des identifiants (étape 0), et choisir l'API (la plus récente ou l'ancienne). À vérifier avec eux : la livraison à domicile (le site l'annonce) ou en point relais, les pays FR/BE/CH/LU, le poids et les dimensions exigés, l'annulation d'une étiquette inutilisée et sa facturation. **Suivi** (décision 14) : l'API doit exposer le statut du colis ; vérifier surtout qu'un **dépôt en consigne (locker)** produit bien un statut « pris en charge / déposé » chez le transporteur, et au bout de combien de temps, car toute la transition automatique vers `SHIPPED` repose dessus. Si ce n'est pas le cas, seul le bouton « Colis remis » joue ce rôle.
   Le poids de chaque bijou n'est pas dans le modèle : en V1, un poids par défaut est confirmé à chaque fabrication.
4. **Commandes à l'étranger.** Les pays autorisés sont FR, BE, CH et LU ; la Suisse est hors Union européenne (douane). À confirmer.
6. ~~**Délai annoncé.**~~ Tranché : fabrication 48 h ouvrées au maximum, puis expédition 24 h ouvrées (décision 13). Le délai remplace le « expédié sous 48h » actuel dans la page Livraison, la FAQ, le bandeau, les CGV et l'e-mail de confirmation. Il change aussi le point 1 : si tout est fabriqué à la commande, il n'y a pas de stock à gérer, seulement une charge de travail.
7. **Annulation par la cliente.** Pas de parcours en V1 : elle écrit à EOSA, qui rembourse dans Stripe et marque la commande annulée. Une pièce déjà fabriquée (`CRAFTED`) ou en cours de fabrication peut ne plus être annulable librement (pièce personnalisée) : à ajouter aux CGV.

## Sujets juridiques et fiscaux à valider

- [x] **Durée de conservation des commandes** (validé le 11 novembre 2026) : les pièces comptables se conservent 10 ans (Code de commerce), mais l'adresse et le téléphone n'ont pas besoin de rester aussi longtemps. Retenu : garder les montants et les lignes, effacer l'adresse de livraison et le téléphone **1 an après la livraison** (ou l'annulation), pour couvrir retours et garantie, par la tâche de purge `PurgeOrderPersonalDataTask` comme pour les ateliers. Reste à rédiger : l'effacement est tracé par `personal_data_purged_at`.
- [ ] **Politique de confidentialité** : ajouter le traitement « commandes » (données, finalités, base légale : exécution du contrat, destinataires : Stripe, Mondial Relay, hébergeur, Infomaniak).
- [ ] **Registre des traitements** : la fiche « commandes » existe en brouillon ; la compléter avec les nouvelles données (adresse, téléphone, numéro de suivi).
- [ ] **CGV et pages Livraison, Retours, FAQ** : vérifier qu'elles décrivent bien les e-mails réellement envoyés (confirmation, expédition avec suivi) et le délai. **Le texte actuel promet une expédition sous 48 heures ouvrées** (page Livraison, FAQ, bandeau « expédié sous 48h ») : il n'est plus vrai si chaque pièce est fabriquée après la commande. À remplacer par « fabrication sous 48 h, expédition sous 24 h » (décision 13), aux quatre endroits : page Livraison, FAQ, bandeau de livraison (`shipping-info`), CGV.
- [ ] **Droit de rétractation** : 14 jours après réception ; vérifier le parcours de retour et de remboursement avec le suivi de livraison (la date de réception vient de `DELIVERED`).
- [ ] **Facture** : voir point ouvert 2.

## Étapes

Chaque étape est une pull request ; la boutique reste utilisable entre deux.

| # | Étape | Contenu |
|---|---|---|
| 0 | Prérequis (hors code) | Trancher les points ouverts 1 à 4, ; **obtenir le contrat et les identifiants de l'API Mondial Relay** (obligatoire : sans eux, `CraftOrder` ne peut pas passer une commande à `CRAFTED`). Les étapes 1 à 4 n'en dépendent pas. |
| 1 | Commande enregistrée | Migration, `Order`, `OrderProvider`, lecture élargie de Stripe, création dans `ConfirmCheckoutSession`, numéro affiché sur la page de succès. Aucune administration encore. **Fait** : la migration ne contient que les colonnes de cette étape (les suivantes arrivent avec leur étape, par de nouvelles migrations) ; les lignes sont copiées depuis les lignes Stripe, donc `jewel_id` et `image_id` restent vides tant que Stripe ne les renvoie pas (à traiter avec l'écran de détail, étape 2). |
| 2 | Administration | `ReadOrders`, `ReadOrder`, écrans liste et détail, pastille « à fabriquer » et commandes en retard. |
| 3 | Rattrapage | `SettleOrders`, liste des sessions payées dans `CheckoutProvider`, CronJob, service compose, e-mails renvoyés. |
| 4 | E-mails | Confirmation à la cliente, nouvelle commande à EOSA (envoyés à la création et par la tâche). |
| 5 | Fabrication et étiquette | `ShippingLabel`, `ShippingLabelProvider`, `MondialRelayFetchedShippingLabels`, `CraftOrder` (étiquette créée au passage à `CRAFTED`, poids confirmé), `ReadOrderLabel` et impression du PDF. Dépend du contrat et des identifiants Mondial Relay (étape 0). |
| 6 | Expédition manuelle | `ShipOrder` (« Colis remis »), e-mail d'expédition avec lien de suivi, `DeliverOrder`, `CancelOrder`. Utile dès l'étape 5 et gardé ensuite comme secours. |
| 7 | Suivi automatique | Statut du colis dans `ShippingLabel` (lecture par `ShippingLabelProvider.read`), tâche `TrackOrderShipments`, CronJob et service compose : dépôt → `SHIPPED` avec e-mail, livraison → `DELIVERED`. Dépend de ce que l'API de Mondial Relay expose (étape 0). |
| 8 | Purge | `PurgeOrderPersonalData`, tâche, CronJob, service compose : adresse et téléphone effacés 1 an après la livraison. |
| 9 | Textes | Politique de confidentialité, CGV, Livraison, FAQ (délais, étiquette, suivi). |
