# Deployment

## Prerequisites

- Docker
- A running MongoDB instance (v6+)

## Running the image

```bash
docker run \
  -e EOSA_MONGO_URL="mongodb://user:password@host:27017/eosa?authSource=admin" \
  -e EOSA_ADMIN_ID="admin" \
  -e EOSA_ADMIN_SECRET="change_me" \
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

### `EOSA_MONGO_URL`

Connection URL to the MongoDB server.

| Property | Value |
|---|---|
| Required | Yes — the application crashes on startup if absent |
| Format | `mongodb://[user]:[password]@[host]:[port]/[database]?authSource=admin` |
| Example | `mongodb://root:root@localhost:27017/eosa?authSource=admin` |

The database name in the URL is ignored — the application always connects to the `eosa` database.

---

### `EOSA_ADMIN_ID`

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