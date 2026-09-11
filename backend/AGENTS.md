# AGENTS.md — chameidor backend

Development guidelines for anyone (human, agent, or tool) working in the chameidor **backend**
(`backend/`). This is the backend half of a two-project repo — the React SPA lives in
[`../frontend/`](../frontend) and the monorepo-level layout is described in
[`../AGENTS.md`](../AGENTS.md). This document describes the backend architecture and
the rules to follow when writing code here, not just a facts dump — when in doubt about how to
implement something, follow the patterns below rather than inventing a new one.

All backend commands run from `backend/` (`cd backend && ./gradlew …`).

## What this service does

chameidor is a task scheduler/executor. Clients register a **one-time** or **periodic** (cron)
task over HTTP, giving a target host/endpoint and a payload. chameidor persists the task, and a
background loop periodically polls for tasks that are due, calls the target endpoint with the
stored payload, and records the outcome (success/failure, next execution time for periodic
tasks). Every registration must carry an `X-External-System` header identifying the caller — this
is stored as `createdBy` and lets multiple systems share one chameidor instance safely.

## Architecture

This is a Gradle multi-module project. Modules form strict layers; **dependencies only point
inward**, never outward:

```
application  →  http-api  →  usecase  ←  persistence
                  gateway  →  usecase
```

- **`usecase`** — the core. Domain models (`Task`, `TaskCreation`, `TaskStatus`), repository
  *interfaces* (`ITaskRepository`, `ITaskExecutionRepository`), services (`TaskService`,
  `TaskExecutor`), health-check abstractions, JSON/date serializers. Has zero dependency on Ktor,
  Spring web, or Exposed — it only depends on Spring for `@Component`/`@Value` DI wiring and on
  `kotlinx-coroutines`/`kotlinx-datetime`. Also owns a `testFixtures` source set
  (`BasicHelpers.kt`) for shared test builders consumed by other modules.
- **`persistence`** — implements the `usecase` repository interfaces against MySQL via **Exposed**
  (not JPA/Hibernate/Ebean). One `Table` object + one `Entity` + one `@Component` repository per
  aggregate (see `TaskTable.kt`/`TaskEntity`/`TaskRepository.kt`). Every mutation on a task also
  writes an audit row via `saveToHistory` into `TaskHistoryTable` — preserve this whenever you add
  a new mutating method to `ITaskRepository`.
- **`http-api`** — Ktor routes, wired into Spring as `@Component` beans implementing
  `ControllerTemplate` (see "Runtime wiring model" below). Talks to `usecase` services only —
  never to `persistence` directly.
- **`gateway`** — outbound HTTP clients (Ktor client/CIO) used to call external systems and their
  health checks.
- **`application`** — composition root. `Boot.kt` is a plain `@Configuration` class with
  `@ComponentScan(basePackages = ["dev.agner.chameidor"])` and a `main()` that calls
  `runApplication<Boot>()`. It contributes no logic of its own — just `application.yaml` /
  `application-prod.yaml` / `logback.xml`.
- **`integrationTest`** — separate module; boots the real stack via docker-compose + WireMock and
  exercises it end-to-end. Not run as part of `test`; see "Testing" below.

### Runtime wiring model (important — don't reinvent this)

There is **no Spring MVC** anywhere in this codebase. HTTP is served entirely by an embedded Ktor
server (`KtorConfig`, in `http-api`), but Spring's `ApplicationContext` is what constructs and
wires everything, via component scanning. Concretely:

1. Spring scans `dev.agner.chameidor.**` and instantiates every `@Component`/`@Service`.
2. Each Ktor route lives in a class that implements `ControllerTemplate` (`fun routes():
   Routing.() -> Unit`) and is annotated `@Component`.
3. Spring collects all of them into a `Set<ControllerTemplate>`, injected into `KtorConfig`
   (`http-api/.../configuration/KtorConfig.kt`), which starts the embedded Netty server and calls
   `routing(it.routes())` for each one.
4. Repository interfaces declared in `usecase` are implemented by `@Component` classes in
   `persistence`; Spring autowires the interface to the implementation by type, so a usecase class
   just asks for `ITaskRepository` in its constructor and gets the Exposed-backed implementation
   at runtime.

This means: **adding a new HTTP endpoint never means touching `KtorConfig`.** You only add a new
`ControllerTemplate` implementation; it is picked up automatically.

## Design principles

- **Interfaces live where they're consumed, not where they're implemented.** `ITaskRepository`
  lives in `usecase` because that's who needs it; `persistence` depends on `usecase` to implement
  it, not the other way around. Apply this to any new port you introduce (repository, external
  gateway, clock, etc.).
- **Domain models are immutable data classes / sealed types.** `TaskCreation` is a sealed class
  with `OneTimeTaskCreation`/`PeriodicTaskCreation` variants — prefer sealed hierarchies over
  boolean flags or nullable-field unions when a type has genuinely distinct shapes.
  `TaskStatus` is an enum.
- **Suspend functions, not blocking calls, at the usecase/repository boundary.** Repository
  methods are `suspend fun`; Exposed transactions are wrapped with `transaction { ... }` inside
  the persistence implementation, not leaked into `usecase`.
- **No framework leakage into `usecase`.** Don't import Ktor, Exposed, or `javax.persistence`
  types there — only plain Kotlin, `kotlinx-*`, and Spring's DI annotations.
- **Every task mutation is audited.** If you add a new state-changing repository method, write the
  corresponding row to `TaskHistoryTable`, following `TaskRepository.saveToHistory`.
- Prefer extending an existing module's package (e.g. `usecase/task/`) over creating a new
  top-level package for a closely related concept.

## How to implement a new feature

Model in `usecase` → define an `I<Name>Repository` port there → service (constructor-injected,
`TaskService`/`TaskExecutor` are the reference shape) → `persistence` implementation (Exposed
`Table`+`Entity`+`@Component` repository) → `@Component class <Name>Controller : ControllerTemplate`
in `http-api` (no manual registration — component scanning handles it). Full walkthrough,
including the migration step: salgadinhos' `kotlin-hexagonal-feature` skill.

## Code style

Detekt (`config/detekt/{config,format}.yml`), `maxIssues: 0`, `autoCorrect: true` — **the build
fails on any issue**, run `./gradlew detekt` before considering a change done. 4-space indent, 120
cols, trailing commas mandatory, no wildcard imports, `allWarningsAsErrors = true` (a compiler
warning is a build failure — fix it, don't suppress it).

## Testing

Kotest `StringSpec` + MockK, JUnit5 platform. Unit tests mirror the main package structure per
module. Integration tests (`integrationTest` module, `DockerComposeExtension` + WireMock) are for
behavior that only makes sense across process boundaries (e.g. "a task actually gets called on
schedule") — don't duplicate unit coverage there. `./gradlew test` runs both and needs Docker —
scope to `./gradlew :usecase:test` etc. while iterating on something that doesn't need it.

## Database migrations

The schema is versioned SQL under `persistence/src/main/resources/db/migration/V*.sql` — there is
no more `mysql/init.sql`. Two tools, each doing one half of the job:

- **Exposed's migration module** (`persistence/.../migration/MigrationScripts.kt`) *generates* the
  SQL by diffing `allTables` (every `Table` object, defined in the same file) against a live
  database. It never applies anything.
- **Flyway** (`persistence/.../migration/Migrator.kt`) *applies* those `V*.sql` files. It runs as
  a standalone `main()` — packaged as a second start script, `bin/migrate`, alongside
  `bin/application` (see `application/build.gradle.kts`) — invoked from `deploy/entrypoint.sh`
  before the app starts. Not from the Spring context: `KtorConfig` blocks the main thread for the
  process's entire lifetime (`ktor.wait: true`), so nothing hooked into Spring's lifecycle would
  run before the server starts accepting requests anyway (see "Runtime wiring model" above). A
  failed migration aborts the container instead of serving traffic against a stale schema.
  `baselineOnMigrate` means a database that already has the tables (a local volume from before
  migrations existed, or any of today's production databases) gets stamped at V1 rather than
  having it re-applied.

Changing a table: edit the `Table` object, then `./gradlew :persistence:generateMigrationScript
-Pname=V2__add_something` (review the generated SQL — it's mechanical, won't detect a rename), then
`./gradlew :persistence:migrate` locally. Full steps and pitfalls: salgadinhos' `kotlin-db-migration`
skill.

`integrationTest/.../tests/MigrationSchemaTest.kt` is the guard: `DockerComposeExtension`
migrates the compose-provided MySQL to head before any spec runs, and this test asserts
`MigrationUtils.statementsRequiredForDatabaseMigration(*allTables)` is empty. If a `Table`
changes without a matching migration (or vice versa), this test fails.

## Configuration

`application.yaml` (see `application/src/main/resources/`):

| Key | Source | Notes |
|---|---|---|
| `ktor.port` | `KTOR_PORT` | defaults to `8080`; the combined Docker image sets it to `API_PORT` so nginx can own `8081` |
| `ktor.wait` | fixed `true` | blocks main thread on the embedded server |
| `mysql.host` / `mysql.user` / `mysql.password` | `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` | required env vars, no defaults |
| `enqueue.period` | `ENQUEUE_TASKS_PERIOD` | defaults to `10` (seconds) if unset |

Use `${VAR}` (required) or `${VAR:default}` (optional) in YAML for any new setting — don't hardcode
values that differ between local/prod.

## Build, run, deploy

```bash
./gradlew clean build          # full build, same as CI
./gradlew test                 # unit tests only
./gradlew testCoverageReport   # aggregated JaCoCo report
./gradlew detekt               # lint (auto-fixes what it can)
```

Local dev: `docker compose -f backend/docker-compose.yml up -d mysql` (or `npm run db` from the
repo root) starts MySQL 9.4.0 empty — run `./gradlew :persistence:migrate` (or `npm run
db:migrate`) to bring it to head, see "Database migrations" above — then run
`dev.agner.chameidor.application.BootKt` with `MYSQL_HOST=localhost`, `MYSQL_USER=root`,
`MYSQL_PASSWORD=` (see `.run/BootKt.run.xml` for the IntelliJ config).

Docker: `backend/Dockerfile` still builds a **backend-only** image (`gradle:8.14-jdk21` →
`openjdk:21-slim`, port `8080`). The published `luiznaac/chameidor` image is now the **combined**
one built from the repo-root `Dockerfile` (backend + built SPA under supervisord + nginx) — see
`../AGENTS.md`.

## Git & CI

- Remote: `git@github.com:luiznaac/chameidor.git`, default branch `master`.
- Commits: short, imperative (`"prevent global job to die"`, `"fix zone"`). Merge via GitHub PR.
- CI: `../.github/workflows/ci.yml` has independent `backend` (this module: `./gradlew clean
  build`) and `frontend` jobs, on every push/PR to `master`.
  `../.github/workflows/docker-publish.yml` runs after that succeeds on `master` and publishes the
  combined Docker image.

**Do not commit directly to `master`.** Always create a feature branch and open a PR,
even for a small or "obviously safe" change — no exceptions.

## Related repositories

Generated from [environments/kotlin](../../environments/AGENTS.md), and shares the same
architecture with [portfolio-2](../../portfolio-2/AGENTS.md) — which uses chameidor as its
task-scheduling backend via `ChameidorGateway`. The monorepo split (`backend/` + `frontend/`,
combined Docker image) mirrors [shougong](../../shougong/AGENTS.md). If you change a cross-cutting
convention here (e.g. the `ControllerTemplate` wiring, the Exposed repository pattern), consider
whether it should be ported to the others as well.
