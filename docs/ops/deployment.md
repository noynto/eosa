# Deployment

## Prerequisites

- Docker
- A running PostgreSQL instance (v15+)

## Running the image

```bash
docker run \
  -e EOSA_PUBLIC_BASE_URL="https://eosa.me" \
  -e EOSA_ADMIN_BASE_URL="http://localhost:18080" \
  -e EOSA_JDBC_URL="jdbc:postgresql://host:5432/eosa" \
  -e EOSA_JDBC_USERNAME="user" \
  -e EOSA_JDBC_PASSWORD="password" \
  -e EOSA_ADMIN_NAME="admin" \
  -e EOSA_ADMIN_SECRET="change_me" \
  -e EOSA_CLIENT_STRIPE_SECRET_KEY="sk_live_..." \
  -e EOSA_SHIPPING_AMOUNT="4.90" \
  -e EOSA_SHIPPING_FREE_THRESHOLD="60.00" \
  -e EOSA_SMTP_HOST="mail.infomaniak.com" \
  -e EOSA_SMTP_USERNAME="contact@eosa.me" \
  -e EOSA_SMTP_PASSWORD="change_me" \
  -p 8080:8080 \
  -p 127.0.0.1:18080:18080 \
  eosa
```

Or using an env file:

```bash
cp .env.example .env
# edit .env with your values
docker run --env-file .env -p 8080:8080 -p 127.0.0.1:18080:18080 eosa
```

## Environment variables

### `EOSA_PUBLIC_BASE_URL`

Public base URL of the server, without trailing slash. Used to build Stripe redirect URLs and jewel image URLs.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `https://eosa.me` |

---

### `EOSA_JDBC_URL`

JDBC connection URL to the PostgreSQL server. The schema is created/updated automatically via Flyway on startup.

| Property | Value |
|---|---|
| Required | Yes — the application crashes on startup if absent |
| Format | `jdbc:postgresql://[host]:[port]/[database]` |
| Example | `jdbc:postgresql://localhost:5432/eosa` |

---

### `EOSA_JDBC_USERNAME` / `EOSA_JDBC_PASSWORD`

Credentials for the PostgreSQL connection.

| Property | Value |
|---|---|
| Required | Yes |

---

### `EOSA_ADMIN_NAME`

Username of the default administrator identity, used to sign in on the administration server (`/sign-in`).

| Property | Value |
|---|---|
| Required | Yes |
| Example | `admin` |

---

### `EOSA_ADMIN_SECRET`

Password for the administrator account.

| Property | Value |
|---|---|
| Required | Yes |
| Recommendation | Use a strong random value in production (e.g. `openssl rand -base64 32`) |
| Example | `change_me` |

---

### `EOSA_CLIENT_STRIPE_SECRET_KEY`

Secret key for the Stripe API. Use a `sk_test_` key for test mode and `sk_live_` for production.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `sk_live_...` |

---

### `EOSA_SHIPPING_AMOUNT`

Montant des frais de livraison appliqués au panier, exprimé en euros.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `4.90` |

---

### `EOSA_SHIPPING_FREE_THRESHOLD`

Montant total du panier (en euros) à partir duquel la livraison est offerte.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `60.00` |

---

### Étiquette d'expédition : colis et expéditeur

Imprimés sur l'étiquette que « Pièce fabriquée » crée dans l'administration. Facultatifs au démarrage : sans l'adresse d'expédition, l'application et les tâches fonctionnent et « Pièce fabriquée » échoue avec un message clair.

| Variable | Rôle | Exemple |
|---|---|---|
| `EOSA_SHIPPING_SENDER_NAME`, `_LINE1`, `_POSTAL_CODE`, `_CITY` | Adresse d'expédition (nom, rue, code postal, ville) | `EOSA`, `1 rue Exemple`, `54000`, `Nancy` |
| `EOSA_SHIPPING_SENDER_PHONE`, `_EMAIL` | Téléphone et e-mail de l'expéditeur (facultatifs) | `+33600000000` |
| `EOSA_SHIPPING_DEFAULT_PARCEL_WEIGHT_GRAMS` | Poids du colis proposé à la fabrication (150 par défaut) | `150` |
| `EOSA_SHIPPING_PARCEL_LENGTH_CM`, `_WIDTH_CM`, `_DEPTH_CM` | Dimensions de la boîte d'expédition en centimètres, **exigées par Mondial Relay** (sans elles : `WebApi_MandatoryParcelSize`) et identiques pour toutes les commandes (12 × 9 × 4 par défaut) | `12`, `9`, `4` |

---

### Mondial Relay V2 : création de l'étiquette

L'API Connect (V2.0) crée l'étiquette quand on clique « Pièce fabriquée ». Facultative au démarrage, comme le bloc précédent. Dans Kubernetes, ces variables ne concernent que le `Deployment` (les CronJobs n'en ont pas besoin tant qu'aucune tâche ne crée d'étiquette). Les identifiants se génèrent dans Connect, sous « Configuration des API », en choisissant « API Version V2.0 ».

| Variable | Rôle | Exemple |
|---|---|---|
| `EOSA_CLIENT_MONDIAL_RELAY_V2_API_URL` | Adresse de l'API ; le **bac à sable** par défaut (étiquettes factices, non expédiables), la production à configurer explicitement | `https://connect-api.mondialrelay.com/api/shipment` |
| `EOSA_CLIENT_MONDIAL_RELAY_V2_LOGIN` | Identifiant de l'API (secret) | `CODE@business-api.mondialrelay.com` |
| `EOSA_CLIENT_MONDIAL_RELAY_V2_PASSWORD` | Mot de passe de l'API (secret) | `change_me` |
| `EOSA_CLIENT_MONDIAL_RELAY_V2_CUSTOMER_ID` | Code client de 8 caractères | `BDTEST` |

Les identifiants du bac à sable et ceux de la production sont distincts : passer à la production change l'adresse de l'API **et** les trois identifiants.

---

### Mondial Relay V1 : suivi du colis

Le web service SOAP (V1) lit le statut d'un colis pour le suivi automatique : la tâche `TrackOrderShipments` (CronJob `track-order-shipments`, toutes les 15 minutes) en a besoin, pas le `Deployment`. Facultatives au démarrage : sans elles, la tâche échoue avec un message clair. Le web service V1 n'a **pas de bac à sable** : ce sont les identifiants de production, et l'appel ne fait que lire. Ils se trouvent dans Connect, sous « Configuration des API », en choisissant « API Version V1.0 » ; ce ne sont pas ceux de la V2. Le web service doit être activé sur l'enseigne par Mondial Relay (sans cela, il répond « compte non activé »).

| Variable | Rôle | Exemple |
|---|---|---|
| `EOSA_CLIENT_MONDIAL_RELAY_V1_API_URL` | Adresse du web service, la même pour les essais et la production | `https://api.mondialrelay.com/WebService.asmx` |
| `EOSA_CLIENT_MONDIAL_RELAY_V1_ENSEIGNE` | Code enseigne (8 caractères) | `BDTEST13` |
| `EOSA_CLIENT_MONDIAL_RELAY_V1_PRIVATE_KEY` | Clé privée servant à signer les appels (secret) | `change_me` |

---

### `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK`

When set to `true`, the application creates the default administrator identity on startup and exits. Intended for one-shot initialisation jobs.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |

---

### `EOSA_SMTP_HOST`

SMTP server used to send e-mails (workshop bookings). In production: Infomaniak, with the `contact@eosa.me` mailbox. Locally, `docker compose` points to Mailpit (`mailpit`), which captures the e-mails (web UI on `http://localhost:8025`).

| Property | Value |
|---|---|
| Required | Yes |
| Example | `mail.infomaniak.com` (production), `mailpit` (compose) |

---

### `EOSA_SMTP_PORT` / `EOSA_SMTP_SECURITY`

SMTP port and transport security: `ssl` (TLS from the start, port `465`), `starttls` (port `587`) or `none` (plain text, local Mailpit only — never in production).

| Property | Value |
|---|---|
| Required | No |
| Default | `465` / `ssl` |

---

### `EOSA_SMTP_USERNAME` / `EOSA_SMTP_PASSWORD`

SMTP credentials. Leave both unset for a server without authentication (Mailpit). With Infomaniak, use an app password if two-factor authentication is enabled on the mailbox.

| Property | Value |
|---|---|
| Required | No (the password is required when the username is set) |
| Example | `contact@eosa.me` |

---

### `EOSA_MAIL_FROM`

Sender address of every e-mail, displayed as « EOSA ». Replies go to the same address.

| Property | Value |
|---|---|
| Required | No |
| Default | `contact@eosa.me` |

---

### `EOSA_SEND_TEST_EMAIL_TO`

When set, the application sends a test e-mail to this address and exits instead of starting the servers. Use it to check the SMTP configuration, the deliverability (spam folder, sender name) and the e-mail layout. Locally: `docker compose run --rm send-test-email` (sent to Mailpit unless the SMTP variables are overridden in `.env`).

| Property | Value |
|---|---|
| Required | No |
| Example | `you@example.com` |

---

### `EOSA_ADMIN_BASE_URL`

Base URL of the administration server, without trailing slash. Administration (`/sign-in`, `/jewels`, `/charms`…) runs on a separate Javalin server, served on its own host (e.g. `https://admin.eosa.me`) as an installable PWA. With an `https` URL the session cookie is marked `Secure`; always use HTTPS when the admin is reachable from the internet.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `https://admin.eosa.me` (production), `http://localhost:18080` (local) |

---

### `EOSA_PUBLIC_SERVER_PORT` / `EOSA_ADMIN_SERVER_PORT`

Ports the public storefront and the administration servers listen on.

| Property | Value |
|---|---|
| Required | No |
| Default | `8080` (public) / `18080` (admin) |
