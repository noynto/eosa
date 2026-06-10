# Deployment

## Prerequisites

- Docker
- A running MongoDB instance (v6+)

## Running the image

```bash
docker run \
  -e EOSA_BASE_URL="https://eosa.me" \
  -e EOSA_MONGO_URL="mongodb://user:password@host:27017/eosa?authSource=admin" \
  -e EOSA_ADMIN_NAME="admin" \
  -e EOSA_ADMIN_SECRET="change_me" \
  -e EOSA_CLIENT_STRIPE_SECRET_KEY="sk_live_..." \
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

Public base URL of the server, without trailing slash. Used to build Stripe redirect URLs and product image URLs.

| Property | Value |
|---|---|
| Required | Yes |
| Example | `https://eosa.me` |

---

### `EOSA_MONGO_URL`

Connection URL to the MongoDB server.

| Property | Value |
|---|---|
| Required | Yes — the application crashes on startup if absent |
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

### `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK`

When set to `true`, the application creates the default administrator identity on startup and exits. Intended for one-shot initialisation jobs.

| Property | Value |
|---|---|
| Required | No |
| Default | `false` |