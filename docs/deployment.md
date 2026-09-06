# Deployment

## Prerequisites

- Docker
- A running PostgreSQL instance (v15+)

## Running the image

```bash
docker run \
  -e EOSA_BASE_URL="https://eosa.me" \
  -e EOSA_JDBC_URL="jdbc:postgresql://host:5432/eosa" \
  -e EOSA_JDBC_USERNAME="user" \
  -e EOSA_JDBC_PASSWORD="password" \
  -e EOSA_ADMIN_NAME="admin" \
  -e EOSA_ADMIN_SECRET="change_me" \
  -e EOSA_CLIENT_STRIPE_SECRET_KEY="sk_live_..." \
  -e EOSA_SHIPPING_AMOUNT="4.90" \
  -e EOSA_SHIPPING_FREE_THRESHOLD="60.00" \
  -p 8080:8080 \
  eosa
```

Or using an env file:

```bash
cp .env.example .env
# edit .env with your values
docker run --env-file .env -p 8080:8080 eosa
```

## Environment variables

### `EOSA_BASE_URL`

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

### `EOSA_MONGO_URL`

Connection URL to the legacy MongoDB server. Only required when running `EOSA_MIGRATE_MONGO_TO_POSTGRES_TASK` — not used otherwise.

| Property | Value |
|---|---|
| Required | No — only during the one-shot Mongo → Postgres migration |
| Format | `mongodb://[user]:[password]@[host]:[port]/[database]?authSource=admin` |
| Example | `mongodb://root:root@localhost:27017/eosa?authSource=admin` |

The database name in the URL is ignored — the application always connects to the `eosa` database.

---

### `EOSA_ADMIN_NAME`

Username for the administrator account, used to authenticate against protected routes (`POST /admin/*`) via HTTP Basic Auth.

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

### `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK`

When set to `true`, the application creates the default administrator identity on startup and exits. Intended for one-shot initialisation jobs.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |

---

### `EOSA_MIGRATE_PRODUCTS_TO_JEWELS_TASK`

When set to `true`, the application copies every document from the legacy `products` collection into `jewels` (skipped if `jewels` already has documents) and exits. The `products` collection is left untouched as a backup. One-shot job, run once after upgrading to the `Jewel` rename if the deployment has pre-existing catalog data.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |

---

### `EOSA_MIGRATE_MONGO_TO_POSTGRES_TASK`

When set to `true`, the application copies every `identity`, `identity_session`, `jewel` (with its images and image order), `cart` (with its items) from MongoDB (including the `images` GridFS bucket) into the PostgreSQL database, then exits. Skipped if the PostgreSQL tables already contain data. Requires `EOSA_MONGO_URL` to be set. One-shot job, run once when cutting a deployment over from MongoDB to PostgreSQL.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |