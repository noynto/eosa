# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`eosa` is a Java 25 e-commerce web application (`me.noynto:eosa`) selling jewelry (necklaces, bracelets). It exposes a public storefront (browsing, cart, Stripe checkout) and a session-authenticated admin area (product management).

The `pom.xml` and `src/` are at the repository root (there is no `server/` subdirectory).

## Stack

| Layer | Technology |
|---|---|
| Web framework | Javalin 7.2.2, on Jetty — pinned to 12.1.11 via `jetty-bom`/`jetty-ee10-bom` in `pom.xml` (Javalin still bundles the vulnerable 12.1.8) |
| Templates | JTE 3.2.3 (pre-compiled to `jte-classes/` at build time) |
| Persistence | MongoDB (driver-sync 5.7.0) |
| Password hashing | jBCrypt 0.4 |
| Payments | Stripe (Checkout Sessions), called via raw `java.net.http.HttpClient` — no Stripe SDK |
| Logging | SLF4J 2.0.17 + slf4j-simple |
| Metrics | Micrometer (javalin-micrometer) |
| Tests | JUnit Jupiter 5.11.0 + Mockito 5.18.0 |
| Runtime | Java 25 (distroless `java25-debian13` Docker image) |

## Common Commands

All commands run from the repository root (where `pom.xml` lives). Use the bundled wrapper (`./mvnw` / `mvnw.cmd`) so the build doesn't depend on a local Maven install:

```bash
# Build
./mvnw compile

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Full build + test
./mvnw verify

# Package
./mvnw package

# Clean build
./mvnw clean package
```

## Project Structure

```
Bootstrap.java               — composition root: reads env vars, wires every dependency by hand (no DI framework), registers all Javalin routes
application/                 — use cases (one class per command, e.g. AddProductToCart, InitiateCheckout)
product/, identity/,         — domain: interfaces + records only, no infrastructure code
  cart/, checkout/, image/,     (identity/ also holds IdentitySession — there is no separate session/ package)
  hash/, shared/
shared/                       — value objects (IDs as records, e.g. ProductId, CartId)
task/                         — one-shot startup tasks (CreateDefaultAdministratorIdentityTask)
infrastructure/
  persistence/                — MongoPersistedX implements domain provider interfaces
  persistence/mongo/           — MongoDB config (MongoConfiguration reads EOSA_MONGO_URL)
  fetch/stripe/                — Stripe Checkout Sessions client, wired into Bootstrap
  fetch/photon/, fetch/mondialrelay/ — address autocomplete / pickup-point clients; NOT wired into
                                  Bootstrap (no route registers them) — treat as in-progress/unused
  security/                   — SecuredCrypts (BCrypt)
  web/                        — Javalin handlers
src/main/jte/                 — JTE templates
src/test/java/.../application/ — use case unit tests (Mockito mocks), the only tests run by CI
docs/                          — deployment docs + product images used in the storefront
requests/                      — .http files for manual API testing
.github/workflows/             — ci.yml (build+test), security.yml (Trivy scan), release.yml
  (semantic-release + Docker push), dependencies.yml (weekly outdated-dependency report)
```

## Architecture

`Bootstrap.java` is the single composition root: it reads env vars via `Configuration`/`Properties`, wires all dependencies manually, and registers every Javalin route in one place — read it first to trace any request end-to-end.

Domain packages (`product/`, `identity/`, `cart/`, `checkout/`, etc.) define interfaces and records only. Infrastructure implementations live in `infrastructure/`, one subpackage per external system (`persistence/mongo`, `fetch/stripe`, etc.).

Not everything under `infrastructure/fetch/` is live: `photon/` (address autocomplete) and `mondialrelay/` (pickup-point search) have handlers/resources but are never instantiated in `Bootstrap.java` — check the routes list there before assuming a feature is reachable.

Admin auth is cookie/session-based, not HTTP Basic Auth: `POST /sign-in` creates an `IdentitySession` and sets an `identity-session-id` cookie; `EnsureIdentityHandler` (registered as a `before` filter on `/admin/*`) validates that cookie on every admin request and redirects to `/sign-in` if invalid.

## Routes

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | — | Home page |
| GET | `/products`, `/products/necklaces`, `/products/bracelets` | — | Product list (all / filtered by category) |
| GET | `/products/{id}` | — | Product detail |
| GET | `/products/{id}/card` | — | Product card partial |
| GET | `/images/{id}` | — | Image download |
| GET | `/legal`, `/terms`, `/privacy` | — | Static legal pages |
| GET | `/shipping/banner`, `/shipping/info` | — | Shipping rule partials |
| GET, POST, PATCH, DELETE | `/cart`, `/cart/items/{product-id}` | — | Cart (session ensured via `before` filter) |
| POST | `/checkout`, GET `/checkout/success` | — | Stripe checkout session creation / confirmation |
| GET, POST | `/sign-in` | — | Admin login page / credential submit |
| GET | `/admin/products`, `/admin/products/{id}`, `/admin/products/{id}/row` | Session cookie | Admin product views |
| POST | `/admin/products`, `/admin/products/{id}/images` | Session cookie | Create product, add images |
| PATCH | `/admin/products/{product-id}/tagline\|price\|category\|state` | Session cookie | Product field updates |

## Environment Variables

See `.env.example` for a ready-to-copy template and `docs/deployment.md` for the full reference.

| Variable | Required | Description |
|---|---|---|
| `EOSA_BASE_URL` | Yes | Public base URL, used for Stripe redirect URLs and image URLs |
| `EOSA_MONGO_URL` | Yes | MongoDB connection URL (database name in the URL is ignored — always connects to `eosa`) |
| `EOSA_ADMIN_NAME` | Yes | Default admin username |
| `EOSA_ADMIN_SECRET` | Yes | Default admin password |
| `EOSA_CLIENT_STRIPE_SECRET_KEY` | Yes | Stripe secret key (`sk_test_...` / `sk_live_...`) |
| `EOSA_SHIPPING_AMOUNT` | Yes | Shipping cost applied to the cart (euros) |
| `EOSA_SHIPPING_FREE_THRESHOLD` | Yes | Cart total above which shipping is free (euros) |
| `EOSA_CREATE_DEFAULT_ADMINISTRATOR_IDENTITY_TASK` | No (default `false`) | If `true`, creates the default admin identity on startup then exits — a one-shot init job, not the normal server run |

## Testing

`mvn test`/`verify` only picks up Surefire's default pattern (`*Test.java`) — that's everything under `src/test/java/.../application/`. Classes suffixed `*IT` (e.g. `MondialRelayApiResourceIT`) are integration tests that hit a real external API (Mondial Relay sandbox); there is no Failsafe plugin configured, so they never run automatically and must be run manually from the IDE.

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
- **Security** (`security.yml`): runs a Trivy filesystem scan on every push/PR to `main`; fails the build on any HIGH or CRITICAL vulnerability.
- **Dependencies** (`dependencies.yml`): weekly (`mvn versions:display-dependency-updates`) report of outdated dependencies.
- **Release** (`release.yml`): on push to `main`, runs semantic-release then builds and pushes the Docker image to `ghcr.io/noynto/eosa` with semver tags.

Commit messages must follow Conventional Commits for semantic-release to work (`feat:`, `fix:`, `chore:`, etc.).
