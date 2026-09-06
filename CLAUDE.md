# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`eosa` is a Java 25 e-commerce web application (`me.noynto:eosa`). It exposes a public storefront and a password-protected admin area.

The `pom.xml` and `src/` are at the repository root (there is no `server/` subdirectory).

## Stack

| Layer | Technology |
|---|---|
| Web framework | Javalin 7.2.0 |
| Templates | Mustache (`javalin-rendering-mustache`, `com.github.spullara.mustache.java`) |
| Persistence | PostgreSQL via JDBC brut (no ORM) + Flyway for schema migrations |
| Password hashing | jBCrypt 0.4 |
| Logging | SLF4J 2.0.17 + slf4j-simple |
| Metrics | Micrometer (javalin-micrometer) |
| Tests | JUnit Jupiter 5.11.0 + Mockito 5.18.0 |
| Runtime | Java 25 (distroless Docker image) |

## Common Commands

All commands run from the repository root (where `pom.xml` lives):

```bash
# Build
mvn compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Full build + test
mvn verify

# Package
mvn package

# Clean build
mvn clean package
```

## Project Structure

```
Bootstrap.java              — composition root, all wiring done here
application/                — use cases (one class per command)
jewel/, identity/,          — domain: interfaces + records
  image/, session/, hash/
shared/                     — value objects (IDs as records)
infrastructure/
  persistence/              — JdbcPersistedX implements domain interfaces (plain JDBC, upsert via ON CONFLICT)
  persistence/jdbc/         — Postgres config (JdbcConfiguration reads EOSA_JDBC_URL/USERNAME/PASSWORD, runs Flyway)
  persistence/mongo/        — legacy MongoDB config/classes, only used by the one-shot MigrateMongoToPostgresTask
  security/                 — SecuredCrypts (BCrypt)
  web/                      — Javalin handlers + BasicAuth helper
src/main/resources/templates/ — Mustache templates; partials/ holds header-main/footer-main
  and header-admin/footer-admin (no JTE-style layout wrapping — each page includes its own
  header/footer partial)
src/main/resources/db/migration/ — Flyway SQL migrations (Vn__description.sql), run automatically on boot
src/test/java/application/  — use case unit tests (Mockito mocks)
docs/                       — documentation
requests/                   — HTTP client requests (.http files)
.github/workflows/          — ci.yml (build+test), release.yml (semantic-release + Docker push)
```

## Architecture

`Bootstrap.java` is the single composition root: it reads env vars, wires all dependencies manually (no DI framework), and registers Javalin routes.

Domain packages (`jewel/`, `identity/`, etc.) define interfaces and records only — no infrastructure code. Infrastructure implementations live in `infrastructure/`.

View models passed to `ctx.render(...)` are plain `Map<String, Object>` (no view-model classes) built by hand in each handler — Mustache is logic-less, so anything JTE could compute inline (formatting, enum comparisons, pluralization, conditional CSS classes) must be precomputed in Java before it reaches the template. `{{> partial}}` paths inside a template resolve relative to *that template's own directory*, not the templates root — a template under `admin/` or `checkout/` must reference `partials/...` as `../partials/...`.

## Routes

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | — | Home page |
| GET | `/jewels` | — | Jewel list |
| GET | `/jewels/{id}` | — | Jewel detail |
| GET | `/jewels/{id}/card` | — | Jewel card partial |
| GET | `/images/{id}` | — | Image download |
| GET | `/cart` | — | Cart page |
| GET | `/payment` | — | Payment page |
| GET | `/admin/sign-in` | — | Admin login page |
| POST | `/admin/jewels` | Basic Auth | Create jewel |
| POST | `/admin/jewels/{id}/images` | Basic Auth | Add images to jewel |

## Environment Variables

See `.env.example` for a ready-to-copy template and `docs/deployment.md` for the full reference.

| Variable | Required | Description |
|---|---|---|
| `EOSA_JDBC_URL` | Yes | PostgreSQL JDBC connection URL |
| `EOSA_JDBC_USERNAME` | Yes | PostgreSQL username |
| `EOSA_JDBC_PASSWORD` | Yes | PostgreSQL password |
| `EOSA_MONGO_URL` | No | Legacy MongoDB connection URL — only needed to run `EOSA_MIGRATE_MONGO_TO_POSTGRES_TASK` |
| `EOSA_ADMIN_ID` | Yes | Admin username (HTTP Basic Auth) |
| `EOSA_ADMIN_SECRET` | Yes | Admin password |

## Responsive Design

When working on any JTE template or HTML, every layout change must handle all three breakpoints:

| Breakpoint | Width | Represents |
|---|---|---|
| Mobile | < 640 px | All phones (min target: 390 px — iPhone 15) |
| Tablet | 640–1024 px | Tablets portrait + large phones landscape |
| Desktop | ≥ 1024 px | Tablets landscape + computers |

Always test the golden path at 390 px, 768 px, and 1280 px before considering UI work done.

## CI/CD

- **CI** (`ci.yml`): runs `mvn verify` on every push/PR to `main`.
- **Release** (`release.yml`): on push to `main`, runs semantic-release then builds and pushes the Docker image to `ghcr.io/noynto/eosa` with semver tags.

Commit messages must follow Conventional Commits for semantic-release to work (`feat:`, `fix:`, `chore:`, etc.).