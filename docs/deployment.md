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