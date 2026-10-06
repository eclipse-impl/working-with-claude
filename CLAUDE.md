# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`ops-dashboard`: a Spring Boot 3.2 / Java 17 read-only JSON API (`/api/*`) over PostgreSQL, plus a vanilla-JS wall-screen page in `src/main/resources/static/` (no framework, no build step, charts are inline SVG). The repo doubles as workshop material (`workshop/`, `docs/` — tickets live in `docs/tickets/`).

## Commands

```bash
./mvnw test                                        # Java suite (JUnit 5 + H2 + MockMvc)
./mvnw test -Dtest=DashboardRepositoryTest#kpisForTheLast30DaysMatchTheAnswerKey
npm install && npm test                            # Frontend suite (Jest + jsdom)
npx jest src/test/javascript/render.test.js -t "vendors panel"
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run # run on in-memory H2, no Docker
docker compose up -d db && ./mvnw spring-boot:run  # run on PostgreSQL 16 (default profile)
```

There is no linter. After a change, run both suites and report both counts.

## Rules

- `pom.xml` dependencies are frozen. Any change needs a CHG ticket.
- Every element `id` in `static/index.html` must be listed in `REGISTERED_IDS` in `src/test/javascript/setup/loadApp.js`; `harness.test.js` fails otherwise.
- Don't edit `V2__seed.sql`, `docs/data/deliveries-last-30-days.csv` or `docs/data/ANSWER-KEY.md` by hand; they are generated together by `python3 tools/make_seed.py`. Schema changes go in a new Flyway migration (`V3__...`), never by editing an applied one.

## Architecture

- **Pinned clock.** "Today" is fixed at 2026-09-21 (`ops.today`, `ClockConfig` provides a fixed `Clock` bean). Always derive dates from the injected `Clock`, never `LocalDate.now()`. Default ranges are the last 30 days ending today (`DateRange.resolve`).
- **Backend layering.** Controllers are thin: parse `from`/`to` via `DateRange.resolve`, call `DashboardRepository` or `VendorRepository` (plain SQL through `NamedParameterJdbcTemplate`, no JPA), return Java records. Query conventions (on time = `delivered_date <= promised_date`, deliveries counted by arrival date, revenue excludes cancelled orders, tickets by `opened_at`) are documented at the top of `DashboardRepository`.
- **Query parameters are validated by hand** (TODO-232; `pom.xml` is frozen, so no validation starter). `DateRange.resolve` checks ISO dates, `from <= to` and a span of at most 366 days (counted inclusively); `DeliveryController` checks `limit` is an integer 1 to 500. Every problem is collected and returned as `400 {"errors": [...]}` by `ApiExceptionHandler`. Validation lives in `resolve`, not in the `DateRange` constructor, because the repository tests build backwards ranges directly.
- **SQL must run on both databases.** Tests and the `demo` profile use H2 in PostgreSQL mode with the same migrations; production uses real PostgreSQL. Avoid Postgres-only syntax H2 can't parse.
- **Java tests** are all `@SpringBootTest` + `@ActiveProfiles("demo")` against the real seed, and assert exact figures from `docs/data/ANSWER-KEY.md`.
- **Frontend.** `app.js` is a UMD-style module: `initApp(document, fetchImpl)` is the entry point, exported via `module.exports` under Node and started on `DOMContentLoaded` in the browser. Pure helpers (`formatRate`, `formatMoney`, `barWidths`, `applyPreset`, `daysUntil`) are exported for unit tests.
- **Frontend tests** never hit a server: `loadApp(overrides)` mounts the real `index.html` body into jsdom, replaces `fetch` with a fake API serving `FIXTURES` (overridable per test; `failing: [paths]` forces 500s; `api.calls` records requests), then awaits `app.ready`. The fixtures mirror real API responses by hand — if a response shape changes in Java, update `FIXTURES` too.
