# Spécifications : carte cadeau

Statut : **à valider** (7 novembre 2026). Aucun code n'est écrit ; ce document fixe le fonctionnement, ses dépendances et l'ordre de mise en place.

## Contexte

Une carte cadeau est un montant à dépenser dans la boutique EOSA. La personne qui l'achète la paie en ligne et reçoit **le code et une page imprimable**. Ce qu'elle en fait ensuite (l'offrir en main propre, l'imprimer, la transmettre) est de son côté : **le site ne connaît pas la destinataire**. La personne qui détient le code l'utilise au panier. Le sujet touche au paiement et aux commandes, deux parties du site que le reste du code ne modélise pas encore (voir « Ce que le code actuel impose »).

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
| 8 | Hors périmètre | Carte physique, destinataire et petit mot, envoi à date choisie, achat d'une carte avec une carte, usage pour les ateliers, remboursement en argent, transfert de solde entre cartes, rappel d'expiration (la seule adresse connue est celle de l'acheteuse). |

## Ce que le code actuel impose

Trois constats, lus dans le code, qui décident de l'ordre des travaux :

1. **Le paiement n'est pas enregistré chez EOSA.** `InitiateCheckout` crée un objet `Checkout` qui n'existe que chez Stripe (`StripeFetchedCheckouts`) ; il n'y a aucune table de commandes. La TODO le dit : « conceptualiser et modéliser les commandes après le tunnel de paiement Stripe ».
2. **La confirmation du paiement est lue au retour de la personne** (`GET /checkout/success`), sans webhook Stripe. Pour une commande de bijou, c'est tolérable ; pour une carte cadeau, c'est insuffisant : si la personne ferme l'onglet après avoir payé, la carte ne serait jamais créée. **Il faut un webhook Stripe** (`checkout.session.completed`) qui crée la carte de façon sûre, même sans retour sur le site.
3. **Un panier entièrement couvert par une carte ne peut pas passer par Stripe** : Stripe refuse un paiement de 0 €. Il faut alors confirmer la commande sans Stripe, donc la **modéliser**. C'est le point qui dépend de la conception des commandes.

## Vocabulaire

| Terme | Sens |
|---|---|
| Carte cadeau (`GiftCard`) | Un montant acheté, avec un code, un solde, une date de validité et un état. |
| Code | Texte aléatoire de 12 caractères (alphabet sans caractères ambigus), saisi au panier. Il n'est jamais dans une URL de page publique. |
| Jeton | Texte aléatoire de 32 octets, qui ouvre la page imprimable de la carte (comme les liens de réservation d'atelier). |
| Mouvement | Une ligne du journal d'une carte : émission, réservation du solde, utilisation, libération, annulation. Le solde est la somme des mouvements. |

## États d'une carte

`PENDING_PAYMENT` (créée, en attente du paiement) → `ACTIVE` (payée, utilisable) → `EXPIRED` (au bout d'un an) ou `VOID` (annulée par l'administration). Une carte `PENDING_PAYMENT` qui n'est jamais payée est supprimée par une tâche.

## Parcours : acheter une carte

### 1. Page d'achat — `GET /gift-cards`

Montants proposés en boutons et champ libre (10 à 200 €) ; prénom et e-mail de la personne qui achète (l'e-mail où arrivera la carte). Le texte rappelle la validité d'un an et que la carte s'utilise dans la boutique en ligne. Page indexable, ajoutée au sitemap.

### 2. Paiement — `POST /gift-cards`

Crée la carte en `PENDING_PAYMENT` et une session Stripe Checkout pour son montant, puis redirige vers Stripe. Le champ piège à robots et la limite par IP des formulaires de réservation s'appliquent.

### 3. Création de la carte — `POST /webhooks/stripe`

À `checkout.session.completed`, la carte passe en `ACTIVE`, son code est généré et le premier mouvement (« émission », montant initial) est écrit, puis l'e-mail est envoyé. Le traitement est **idempotent** : un webhook reçu deux fois ne crée rien de plus et n'envoie pas deux e-mails. La signature est vérifiée (`EOSA_STRIPE_WEBHOOK_SECRET`).

### 4. Retour de la personne — `GET /gift-cards/success`

Affiche « Merci, votre carte arrive par e-mail » (la carte peut ne pas être encore active si le webhook est en retard : la page ne promet rien de plus). Aucun code n'est affiché ici.

### 5. Remise — e-mail et page imprimable

- **E-mail « Votre carte cadeau »** à la personne qui a acheté, envoyé dès l'activation : montant, code, validité, lien vers la page imprimable. Il sert aussi de reçu. Un envoi qui échoue est retenté par une tâche planifiée (`RetryGiftCardEmails`, comme les e-mails d'atelier).
- **Page imprimable** — `GET /gift-cards/{token}` : montant, code, validité, solde restant, avec une feuille de style d'impression. Pas de PDF au départ.

## Parcours : utiliser une carte

### Au panier — `POST /cart/gift-card`, `DELETE /cart/gift-card`

La personne saisit son code. Le site vérifie qu'il est valide (carte `ACTIVE`, non expirée, solde positif) et affiche le montant déduit et le reste à payer. Un code faux ou expiré donne le même message (« Ce code n'est pas valable »), sans révéler lequel. Les essais sont limités par IP (10 par heure) : un code de 12 caractères aléatoires n'est pas devinable, mais la limite empêche d'essayer.

Le montant déduit est `min(solde, total du panier avec livraison)`. Une seule carte par commande au départ.

### Au paiement

1. **Réservation du solde** : à la création de la session Stripe, le montant déduit est réservé sur la carte (mouvement « réservé »), pour 30 minutes. Deux paiements en même temps avec la même carte ne peuvent pas dépasser le solde.
2. **Stripe reçoit le reste à payer** : la remise est transmise comme un coupon à montant fixe, à usage unique, créé pour la session.
3. **À la confirmation du paiement** (webhook), la réservation devient une utilisation (mouvement « utilisé »).
4. **Session abandonnée ou expirée** : la réservation est libérée (mouvement « libéré ») par la tâche de nettoyage ou par le webhook `checkout.session.expired`.

### Panier entièrement couvert

Si le solde couvre tout, aucune session Stripe n'est créée : la commande est confirmée directement et la carte est débitée. **Ce cas demande la modélisation des commandes** (voir plus bas) : il ne peut pas être fait avant.

## Administration

- **Liste des cartes** : code (masqué sauf les 4 derniers caractères), montant initial, solde, état, date d'achat, validité, e-mail de l'acheteuse.
- **Détail** : journal des mouvements, bouton « Annuler la carte » (état `VOID`, avec confirmation), bouton « Renvoyer l'e-mail ».
- **Émission manuelle** : créer une carte offerte sans paiement (geste commercial, lot pour un concours), avec un motif qui s'écrit dans le journal, et l'e-mail de la personne qui la reçoit.

## E-mails

| E-mail | Destinataire | Quand |
|---|---|---|
| Votre carte cadeau (reçu, code, lien imprimable) | Personne qui a acheté | À l'activation de la carte |

Mêmes règles d'écriture que les autres e-mails ([e-mails](../tech/emails.md)). Le code apparaît dans l'e-mail ; la page imprimable passe par le jeton.

## Règles métier

- Le montant se calcule en centimes entiers côté serveur ; le montant d'une demande n'est jamais pris tel quel d'un champ caché.
- Le solde ne peut jamais être négatif, et jamais supérieur au montant initial.
- Une carte expirée ou annulée ne se réactive pas. Pas de prolongation automatique.
- La carte est au porteur : qui saisit un code valable l'utilise.
- Les cartes ne s'échangent contre rien d'autre que des bijoux : ni espèces, ni atelier.
- Un retour de bijou payé en partie avec une carte rend la part de la carte sur la carte (mouvement « libéré ») et le reste par Stripe. Ce cas dépend aussi des commandes.
- Les dates sont des `Instant` (UTC) ; la validité compte en jours calendaires (`Europe/Paris`).

## Persistance — migration `V6__gift_cards.sql` (nom à confirmer)

- `gift_cards` : `id`, `code` (unique), `token` (unique), `state`, `initial_amount`, `buyer_first_name`, `buyer_email`, `email_sent_at`, `expires_at`, `stripe_session_id`, `created_at`.
- `gift_card_movements` : `id`, `gift_card_id`, `kind` (`ISSUED`, `RESERVED`, `USED`, `RELEASED`, `VOIDED`), `amount`, `checkout_session_id`, `reason`, `created_at`.
- Un montant est un `NUMERIC(10,2)`, jamais un flottant.

## Organisation du code

Même architecture que les ateliers ([architecture](../tech/architecture.md)) : un `GiftCardProvider` limité à `readIds(filtres…)`, `read`, `write`, `delete` (le journal est un enfant de la carte, écrit avec elle, comme les lignes d'un panier) ; un cas d'usage par action, avec son `Result` ; un service statique pour ce qui croise la carte et le panier (remise à déduire) ; un seul cas d'usage par handler.

| Cas d'usage | Rôle |
|---|---|
| `PurchaseGiftCard` | Crée la carte `PENDING_PAYMENT` et la session Stripe |
| `ConfirmGiftCardPayment` | Active la carte au webhook, écrit l'émission, envoie l'e-mail |
| `RetryGiftCardEmails` | Tâche : renvoie les e-mails de carte qui ne sont pas partis |
| `ReadGiftCard` | Page imprimable, par jeton |
| `ApplyGiftCardToCart` | Valide un code, calcule la remise |
| `RemoveGiftCardFromCart` | Retire la carte du panier |
| `ReleaseExpiredGiftCardReservations` | Tâche : libère les soldes réservés par une session expirée, supprime les cartes jamais payées |
| `ListGiftCards`, `ReadGiftCardMovements`, `VoidGiftCard`, `IssueGiftCardByAdmin`, `ResendGiftCard` | Administration |

## Configuration

| Variable | Rôle |
|---|---|
| `EOSA_STRIPE_WEBHOOK_SECRET` | Secret de signature des webhooks Stripe (obligatoire dès l'étape 3) |
| `EOSA_GIFT_CARD_AMOUNTS` | Montants proposés, séparés par des virgules (`25,50,100`) |
| `EOSA_GIFT_CARD_MIN_AMOUNT`, `EOSA_GIFT_CARD_MAX_AMOUNT` | Bornes du montant libre (`10`, `200`) |
| `EOSA_GIFT_CARD_VALIDITY_DAYS` | Validité en jours (`365`) |

À ajouter à `.env.example`, `compose.yaml`, aux manifestes Kubernetes et au [déploiement](../ops/deployment.md).

## Sujets juridiques et fiscaux à traiter

À faire valider avant la mise en ligne (conseil juridique ou comptable) ; ce document ne tranche aucun de ces points.

- [ ] **Validité** : durée à afficher sur la carte et dans les CGV ; règles applicables aux cartes cadeau pour un consommateur.
- [ ] **TVA** : traitement d'une carte cadeau à la vente (bon à usage unique ou à usages multiples) selon votre régime de TVA.
- [ ] **Droit de rétractation** : application de la rétractation de 14 jours à l'achat d'une carte à distance.
- [ ] **CGV** : un article « Cartes cadeau » (validité, non remboursable en espèces, solde, carte au porteur, perte ou transmission du code, usage de la boutique en ligne uniquement).
- [ ] **Données personnelles** : seule l'acheteuse est connue (prénom, e-mail) ; durée de conservation (proposée : cartes soldées ou expirées supprimées 5 ans après, pour la comptabilité) ; mention dans la politique de confidentialité et le registre des traitements.
- [ ] **Comptabilité** : suivi des cartes vendues non encore utilisées (dette envers les porteurs) ; export de la liste.

## Tests en local

- **Paiement et webhook** : le CLI Stripe (`stripe listen --forward-to localhost:8080/webhooks/stripe`) en mode test.
- **E-mails** : Mailpit.
- **Renvoi des e-mails** : `docker compose run --rm <job>` pour la tâche.

## Étapes de mise en place

Chaque étape est une pull request qui garde le site fonctionnel.

### Étape 0 — Prérequis (hors code)

- [ ] Valider ce document, la validité (1 an) et les bornes (10 à 200 €).
- [ ] Créer le webhook dans le tableau de bord Stripe (adresse du site, évènements `checkout.session.completed` et `checkout.session.expired`) et fournir son secret.
- [ ] Lancer les points juridiques et fiscaux ci-dessus.

### Étape 1 — Carte, journal et administration

Domaine, persistance, `GiftCardProvider`, cas d'usage d'administration (liste, détail, émission manuelle, annulation). Aucun achat en ligne. Permet de vérifier le modèle et le journal.

### Étape 2 — Webhook Stripe

`POST /webhooks/stripe` avec vérification de signature et idempotence, sans effet encore sur les cartes. Brique réutilisable pour les commandes.

### Étape 3 — Achat d'une carte

Page d'achat, session Stripe, activation au webhook, e-mail « Votre carte cadeau », page de retour, page imprimable.

### Étape 4 — Renvoi des e-mails non partis

Tâche `RetryGiftCardEmails`, CronJob Kubernetes et service du profil `jobs` de `compose.yaml`.

### Étape 5 — Utilisation au panier (couverture partielle)

Saisie du code, remise, réservation du solde, coupon Stripe, utilisation à la confirmation, libération à l'expiration.

### Étape 6 — Panier entièrement couvert

Dépend de la modélisation des commandes (TODO) : confirmation sans Stripe, débit de la carte, enregistrement de la commande.

### Étape 7 — Textes

CGV, FAQ, politique de confidentialité, mention dans le sitemap et les données structurées de la page d'achat.
