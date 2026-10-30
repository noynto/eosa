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
| Persistence | PostgreSQL via JDBC brut (no ORM), HikariCP connection pool + Flyway for schema migrations |
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
workshop/                   — workshop booking domain (venues, sessions, bookings, waitlist, dates in Europe/Paris)
mail/                       — Mail record + MailProvider
shared/                     — value objects (IDs as records)
task/                       — one-shot tasks run by Bootstrap then exit (env-activated; Kubernetes CronJobs)
infrastructure/
  persistence/              — JdbcPersistedX implements domain interfaces (plain JDBC, upsert via ON CONFLICT)
  persistence/jdbc/         — Postgres config (JdbcConfiguration reads EOSA_JDBC_URL/USERNAME/PASSWORD, runs Flyway)
  mail/                     — SmtpSentMails (Jakarta Mail) + MailTemplates (templates/mail/*.html|txt.mustache)
  security/                 — SecuredCrypts (BCrypt)
  web/                      — Javalin handlers + BasicAuth helper
src/main/resources/templates/ — Mustache templates; partials/ holds header-main/footer-main
  and header-admin/footer-admin (no layout wrapping — each page includes its own
  header/footer partial); mail/ holds the e-mail templates (table layout, inline styles)
src/main/resources/db/migration/ — Flyway SQL migrations (Vn__description.sql), run automatically on boot
src/test/java/application/  — use case unit tests (Mockito mocks)
docs/                       — documentation
requests/                   — HTTP client requests (.http files)
.github/workflows/          — ci.yml (build+test), release.yml (semantic-release + Docker push)
```

## Architecture

`Bootstrap.java` is the single composition root: it reads env vars, wires all dependencies manually (no DI framework), and registers Javalin routes.

Domain packages (`jewel/`, `identity/`, etc.) define interfaces and records only — no infrastructure code. Infrastructure implementations live in `infrastructure/`. Domain providers stay plain CRUD (`readIds(filters…)`, `read`, `write`); business rules (capacity, deadlines, e-mails) live in the use cases.

Workshop booking is specified in `docs/workshop-booking-spec.md`: a booking is held 30 minutes until confirmed from an e-mail link, links in e-mails only act on POST (mail scanners open links), and periodic work (expiring requests, reminders, e-mail retries) runs as one-shot tasks (`docker compose run --rm <job>` locally, CronJobs in `kubernetes/workshop-cronjobs.yaml`).

View models passed to `ctx.render(...)` are plain `Map<String, Object>` (no view-model classes) built by hand in each handler — Mustache is logic-less, so anything computed (formatting, enum comparisons, pluralization, conditional CSS classes) must be precomputed in Java before it reaches the template. `{{> partial}}` paths inside a template resolve relative to *that template's own directory*, not the templates root — a template under `admin/` or `checkout/` must reference `partials/...` as `../partials/...`.

## Routes

`Bootstrap` starts two Javalin servers: the public storefront on `EOSA_PUBLIC_SERVER_PORT` (default `8080`) and the administration on `EOSA_ADMIN_SERVER_PORT` (default `18080`), meant to stay off the public network. Admin routes have no `/admin` prefix; `EnsureIdentityHandler` guards every admin route except `/sign-in`, `/images/*` and static assets. Admin templates receive `publicUrl` (the storefront base URL) through a wrapping `FileRenderer`.

Public server (`8080`):

| Method | Path | Description |
|---|---|---|
| GET | `/` | Home page |
| GET | `/jewels` | Jewel list |
| GET | `/jewels/{id}` | Jewel detail |
| GET | `/jewels/{id}/card` | Jewel card partial |
| GET | `/images/{id}` | Image download |
| GET | `/about` | Brand story page |
| GET | `/cart` | Cart page |
| GET | `/workshops` | Workshops page with the upcoming sessions |
| GET | `/workshops/sessions/{id}` | Session page (venue, booking form, waitlist form) |
| GET | `/workshops/sessions/{id}/card` | Session card partial (refreshed every 30 s) |
| POST | `/workshops/sessions/{id}/bookings` | Booking request (honeypot + per-IP limit) |
| POST | `/workshops/sessions/{id}/waitlist` | Join the waitlist (before booking opens, or when the session is full) |
| GET/POST | `/workshops/bookings/{token}/confirm` | Confirm a booking from the e-mail link |
| GET/POST | `/workshops/bookings/{token}/cancel` | Cancel a booking from the e-mail link (up to 48 h before) |
| GET/POST | `/workshops/waitlist/{token}/leave` | Leave a waitlist from the e-mail link |

Admin server (`18080`):

| Method | Path | Auth | Description |
|---|---|---|---|
| GET/POST | `/sign-in` | — | Admin login (sets the `identity-session-id` cookie) |
| GET | `/` | Session cookie | Redirects to `/jewels` |
| * | `/jewels*`, `/metal-colors*`, `/charms*` | Session cookie | Admin pages and actions |
| * | `/workshop-venues*` | Session cookie | Workshop venues (cafés…): create, edit, photo, archive |
| * | `/workshops*` | Session cookie | Workshop sessions: create, edit, cancel; bookings (cancel, attended, no-show) and waitlist |
| GET | `/images/{id}` | — | Image download (for admin thumbnails) |

## Environment Variables

See `.env.example` for a ready-to-copy template and `docs/deployment.md` for the full reference.

| Variable | Required | Description |
|---|---|---|
| `EOSA_JDBC_URL` | Yes | PostgreSQL JDBC connection URL |
| `EOSA_JDBC_USERNAME` | Yes | PostgreSQL username |
| `EOSA_JDBC_PASSWORD` | Yes | PostgreSQL password |
| `EOSA_ADMIN_NAME` | Yes | Default admin username |
| `EOSA_ADMIN_SECRET` | Yes | Default admin password |
| `EOSA_PUBLIC_BASE_URL` | Yes | Public storefront base URL (Stripe redirects, admin "Voir le site" link) |
| `EOSA_ADMIN_BASE_URL` | Yes | Administration base URL; an `https` scheme marks the session cookie `Secure` |
| `EOSA_PUBLIC_SERVER_PORT` | No | Public server port (default `8080`) |
| `EOSA_ADMIN_SERVER_PORT` | No | Admin server port (default `18080`) |
| `EOSA_SMTP_HOST` | Yes | SMTP server (`mail.infomaniak.com`; `mailpit` in compose) |
| `EOSA_SMTP_PORT` / `EOSA_SMTP_SECURITY` | No | `465` / `ssl` by default (`starttls`, or `none` for Mailpit only) |
| `EOSA_SMTP_USERNAME` / `EOSA_SMTP_PASSWORD` | No | SMTP credentials; unset for a server without authentication |
| `EOSA_MAIL_FROM` | No | Sender address (default `contact@eosa.me`) |
| `EOSA_SEND_TEST_EMAIL_TO` | No | One-shot task: sends a test e-mail to this address and exits |
| `EOSA_EXPIRE_PENDING_WORKSHOP_BOOKINGS_TASK`, `EOSA_ANNOUNCE_WORKSHOP_BOOKING_OPENINGS_TASK`, `EOSA_SEND_WORKSHOP_REMINDERS_TASK`, `EOSA_RETRY_WORKSHOP_EMAILS_TASK` | No | `true` only in their CronJob: run the task and exit |

## Responsive Design

When working on any Mustache template or HTML, every layout change must handle all three breakpoints:

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