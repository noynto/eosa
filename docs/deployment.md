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

### `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK`

When set to `true`, the application creates the default administrator identity on startup and exits. Intended for one-shot initialisation jobs.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |

---

### `EOSA_ADMIN_BASE_URL`

Base URL of the administration server, without trailing slash. Administration (`/sign-in`, `/jewels`, `/charms`…) runs on a separate Javalin server so it can be kept off the public network: bind its port to localhost, or reach it through `kubectl port-forward svc/eosa-admin 18080:18080`.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `http://localhost:18080` |

---

### `EOSA_PUBLIC_SERVER_PORT` / `EOSA_ADMIN_SERVER_PORT`

Ports the public storefront and the administration servers listen on.

| Property | Value |
|---|---|
| Required | No |
| Default | `8080` (public) / `18080` (admin) |
