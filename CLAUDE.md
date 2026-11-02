# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`eosa` is a Java 25 web application (`me.noynto:eosa`) for the EOSA jewelry brand: a public storefront (jewels, cart, Stripe checkout, workshop booking) and a separate, password-protected administration. `pom.xml` and `src/` are at the repository root.

## Documentation index

The documentation is split by topic in `docs/` and indexed in the README, imported here:

@README.md

Read the relevant document before working on a topic (architecture, web, e-mails, scheduled tasks, development, deployment, specs). Every document in `docs/` must be listed in `README.md` (matching section, one-line description): when you add, rename or remove a document, update the README index in the same change.

## Stack

| Layer | Technology |
|---|---|
| Web | Javalin 7.2.3 (two servers: storefront `8080`, administration `18080`) |
| Templates | Mustache (`javalin-rendering-mustache`) + Tailwind + htmx |
| Persistence | PostgreSQL 17, plain JDBC (no ORM), HikariCP 7, Flyway 11 migrations |
| E-mails | Jakarta Mail (Angus 2.0.3), Infomaniak SMTP, Mailpit locally |
| Payments | Stripe Checkout (HTTP client, Gson) |
| Passwords | jBCrypt 0.4 |
| Tests | JUnit Jupiter 6.1.0 + Mockito 5.18.0 |
| Runtime | Java 25, distroless Docker image, Kubernetes (Deployment + CronJobs) |

## Commands

```bash
./mvnw clean verify               # always clean: incremental builds miss stale test classes
./mvnw test -Dtest=ClassName
docker compose up --build -d      # storefront :8080, admin :18080 (admin/admin), Mailpit :8025
docker compose run --rm <job>     # one-shot tasks (see docs/tech/scheduled-tasks.md)
```

## Rules that always apply

- **Architecture** (`docs/tech/architecture.md`): `Bootstrap.java` is the only composition root (no DI framework). Domain packages hold business classes and `XProvider` interfaces limited to `readIds(filters…)`, `read`, `write`, `delete`; business rules live in the use cases (`application/`, one record per action with a nested `Command`), never in providers or shared helper classes. Use cases get a `Clock`, never call `Instant.now()`.
- **Naming**: ports `XProvider`; adapters `JdbcPersistedX`, `<Service>FetchedX`, `SmtpSentMails`, `SecuredX`.
- **Persistence**: schema changes only through a new Flyway migration (`Vn__description.sql`), never by editing a deployed one; `Instant` / `timestamptz` in UTC, workshop rules in `Europe/Paris`.
- **Templates** (`docs/tech/web.md`): view models are hand-built `Map<String, Object>`; Mustache is logic-less, so compute everything in Java; `{{> partial}}` paths are relative to the including template's folder.
- **E-mail links act only on POST** (mail scanners open links) and carry random tokens, never ids.
- **E-mail templates** (`docs/tech/emails.md`): table layout, padding on `<td>` only, `bgcolor` attributes, no margin / border-radius / font-weight; partials by full file name.
- **Scheduled work** (`docs/tech/scheduled-tasks.md`): one-shot tasks activated by an env var and run by Kubernetes CronJobs; never an in-app scheduler; tasks must be idempotent.
- **Responsive**: every template change handles mobile (< 640 px, min 390 px), tablet (640–1024 px) and desktop (≥ 1024 px); check the golden path at 390, 768 and 1280 px.
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`…), one branch and pull request per topic; semantic-release versions and publishes the image on `main`.
- **Configuration**: environment variables are documented in `.env.example` and `docs/ops/deployment.md`; a new required variable must also be added to `compose.yaml` and the Kubernetes manifests.
