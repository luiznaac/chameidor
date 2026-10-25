# Code map

Module → key files → seam index for the `backend/` lane, plus the app entry points. Gradle multi-module: `application → http-api → usecase ← persistence` and `gateway → usecase`; each Gradle module is one block below, then the repo-root clusters that bind the wire contracts and the image. Vocabulary: **module**, **interface**, **seam**, **adapter**.

## Modules

### `usecase` — domain, ports and services
**Purpose**: the core the other layers implement against — the task model and scheduling loop, the auth seam, the health ports and the shared JSON/clock configuration. Depends on Spring DI annotations, `kotlinx-*`, Jackson, Logback and `kotlin-reflect`; never on Ktor, Exposed or HTTP types.
**Files**: `backend/usecase/src/main/kotlin/dev/agner/chameidor/usecase/task/{Task,TaskCreation,TaskView,TaskService,TaskExecutor,TaskQueryService}.kt`, `task/{ITaskRepository,ITaskExecutionRepository,CallClient,TaskJobHealthCheck}.kt`; `usecase/auth/{AuthorizationService,AuthorizationResult,ExternalSystem,TokenHasher,IExternalSystemRepository}.kt`; `usecase/health/{HealthChecker,HealthGateway,HealthCheckResult}.kt`; `usecase/configuration/{JsonMapper,TimeProviderConfig}.kt` and `configuration/serializers/*`; `usecase/commons/CommonExtensions.kt`; `usecase/src/testFixtures/kotlin/dev/agner/chameidor/usecase/BasicHelpers.kt`
**Seam(s)**:
- **`ITaskRepository`** (`task/ITaskRepository.kt`) — the persisted-task port consumed by `TaskService`/`TaskExecutor`; every mutation must also write the audit row through `TaskRepository.saveToHistory`. Callers name the interface, never `TaskRepository`.
- **`ITaskExecutionRepository`** (`task/ITaskExecutionRepository.kt`) — execution-outcome port; the sealed `TaskExecutionResult` (success/failure, duration) is the record shape.
- **`CallClient`** (`task/CallClient.kt`) — outbound callback port implemented by `gateway`'s `CallGateway`; keep `gateway` depending on `usecase`, never the reverse.
- **`AuthorizationService.authorize(...)`** (`auth/AuthorizationService.kt`) — the one auth decision: resolve the bearer token (SHA-256 via `TokenHasher`) or the deprecated `X-External-System` alias against `IExternalSystemRepository`, returning `AuthorizationResult`. No Ktor request object crosses this seam.
- **`HealthChecker` / `HealthGateway`** (`health/`) — inbound health port (implemented by the MySQL and outbound-HTTP checks) and outbound health port (implemented by `AppHealthGateway`).
- **`TaskCreation` / `TaskStatus`** (`task/TaskCreation.kt`, `task/Task.kt`) — the one-time/periodic discriminator and the status enum mirrored by the persistence table (`cron` nullable) and by `frontend/src/api/types.ts`.
- **`TaskService.execute()` loop** (`task/TaskService.kt`) — the background poll loop; `destroy()` cancels it, so a new lifecycle hook must preserve that cancellation.

### `persistence` — Exposed/MySQL adapters and migrations
**Purpose**: implements the `usecase` repository ports against MySQL via Exposed, and owns the Flyway migration tooling.
**Files**: `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/task/{TaskTable,TaskHistoryTable,TaskExecutionTable,TaskRepository,TaskExecutionRepository}.kt`; `persistence/auth/{ExternalSystemTable,ExternalSystemRepository}.kt`; `persistence/configuration/DatabaseConfig.kt`; `persistence/health/MySqlConnectionHealthCheck.kt`; `persistence/migration/{MigrationScripts,Migrator}.kt`; `persistence/src/main/resources/db/migration/V*.sql`
**Seam(s)**:
- **Adapter seam** — each `@Component … : I*Repository` sits at its usecase interface; `TaskRepository` wraps every method in `transaction { … }` and writes `TaskHistoryTable` on mutation. `TransactionService`-style composition does not exist here — a service that needs two repositories stays in one transaction by calling one repository method.
- **Query seam** — `TaskTable.idx_status_next_execution` backs `findExecutableTasks` (`status = WAITING AND next_execution_at <= now`); changing the predicate means changing the index in a migration.
- **Migration seam** — a new `Table` object must join `allTables` in `migration/MigrationScripts.kt`; `Migrator.migrate(...)` applies `V*.sql` under `persistence/src/main/resources/db/migration/`, and `MigrationSchemaTest` fails on drift between `allTables` and a migrated database.
- **Connection seam** — `DatabaseConfig.mysqlJdbcUrl` is shared with `Migrator`, so Exposed and Flyway cannot drift.

### `gateway` — outbound HTTP integrations
**Purpose**: Ktor-client adapters for callback delivery and the self-health probe.
**Files**: `backend/gateway/src/main/kotlin/dev/agner/chameidor/gateway/call/CallGateway.kt`; `gateway/configuration/KtorClientConfig.kt`; `gateway/health/{AppHealthGateway,HttpClientHealthCheck}.kt`; `gateway/src/test/kotlin/dev/agner/chameidor/gateway/call/CallGatewayContractTest.kt`
**Seam(s)**:
- **`CallClient` adapter** (`call/CallGateway.kt`) — `TOKEN` from `chameidor.token`, sends `Authorization: Bearer`, `X-External-System` and `X-Chameidor-Task-Id`, and throws on non-2xx. The request body is pinned against `contracts/callback-request.json`.
- **`HealthGateway` adapter** (`health/AppHealthGateway.kt`) — probes the service's own `GET /health/internal` on `ktor.port` (`API_PORT` at runtime), not `WEB_PORT`.
- **`KtorClientConfig`** — the one shared `HttpClient` bean (Jackson negotiation, timeout); a new integration adds an adapter here, not another client.

### `http-api` — Ktor routes and edge adapters
**Purpose**: the HTTP surface — controllers, DTOs, content negotiation, CORS and the Ktor server bootstrap.
**Files**: `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/{ControllerTemplate,TaskController,HealthController}.kt` and `controller/{TaskResponse,TaskRegistrationResponse,ErrorResponse}.kt`; `http-api/src/main/kotlin/dev/agner/chameidor/httpapi/configuration/KtorConfig.kt`; `http-api/src/test/kotlin/dev/agner/chameidor/httpapi/controller/ContractFixturesTest.kt`
**Seam(s)**:
- **`ControllerTemplate.routes()`** (`controller/ControllerTemplate.kt`) — `typealias RouteDefinition = Routing.() -> Unit`; `KtorConfig` injects `Set<ControllerTemplate>` and mounts each one, so adding an endpoint is adding a `@Component` controller and never editing `KtorConfig`.
- **Authorization gate** (`controller/TaskController.kt`) — `authorizedSystem()` reads the bearer header (and the deprecated alias), calls `AuthorizationService.authorize(...)`, and returns a JSON `ErrorResponse` with 401/403 instead of reaching a usecase.
- **Route precedence** (`controller/TaskController.kt`) — `GET /tasks/executions` is declared before `GET /tasks/{id}` so the literal path is not captured as an id.
- **Wire-DTO seam** (`controller/*Response.kt`) — snake_case, null-omitted JSON produced by the shared `JsonMapper`; the same shapes are mirrored in `frontend/src/api/types.ts` and pinned by `contracts/`.

### `application` — composition root
**Purpose**: the Spring Boot entry and runtime configuration; wires the modules.
**Files**: `backend/application/src/main/kotlin/dev/agner/chameidor/application/Boot.kt`; `application/src/main/resources/{application.yaml,application-prod.yaml,logback.xml}`
**Seam(s)**: `Boot.kt`'s `@ComponentScan("dev.agner.chameidor")` is the wiring seam — every `@Component`/`@Service` is picked up, so a new class needs no registration. `application.yaml` is the config seam (`ktor.port`, `ktor.wait: true`, `mysql.*`, `chameidor.token`, `enqueue.period`).

### `integrationTest` — full-stack test harness
**Purpose**: end-to-end specs that boot the real app against a compose MySQL and stubbed callbacks.
**Files**: `backend/integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/config/{IntegrationTest,DockerComposeExtension,HttpMockService,HttpRequestService,ClockMock,KotestConfig,ResetWiremock,StopKtorServer}.kt`; `integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/tests/{MigrationSchemaTest,TaskCallbackAuthenticationTest,TaskRegistrationAuthorizationTest}.kt`; `integrationTest/src/test/resources/application-test.yaml`
**Seam(s)**: `DockerComposeExtension` starts `backend/docker-compose.yml` (MySQL + WireMock) and migrates it to head before any spec; `IntegrationTest` boots `Boot` with the `test` profile; `HttpMockService` stubs the callback target. `MigrationSchemaTest` is the schema guard for a changed `Table`. Copy this module as-is when the harness is needed elsewhere.

### Repo root — contracts, deploy and CI
**Purpose**: the cross-cutting fixtures and packaging that bind backend + frontend into one image.
**Files**: `contracts/{tasks-one-time-request,tasks-one-time-response,tasks-periodic-request,tasks-periodic-response,callback-request}.json`, `contracts/README.md`; `deploy/{entrypoint.sh,supervisord.conf,nginx.conf.template}`; `Dockerfile`, `docker-compose.yml`; `package.json`; `.github/workflows/{ci,docker-publish}.yml`
**Seam(s)**:
- **Contract fixtures** — consumers pin the JSON in `contracts/`; `ContractFixturesTest` keeps them equal to the real payloads, so a DTO change edits the fixture in the same commit.
- **Container entry** (`deploy/entrypoint.sh`) — renders the nginx template, runs `/opt/app/bin/migrate` (Flyway), then `exec supervisord`; a failed migration aborts the container. `API_PORT` (app) and `WEB_PORT` (nginx, reverse-proxying `/api`) are the two ports the combined image separates.
- **Script shims** (`package.json`) — `be:unit` is the Docker-free `unitTest` lane, `be:integration` the Docker-backed `:integrationTest:test`; `db`/`db:migrate` drive the compose MySQL. Not a real Node package.

## Entry points

- `backend/application/src/main/kotlin/dev/agner/chameidor/application/Boot.kt` — `main()`; Spring Boot component-scans `dev.agner.chameidor` and starts the process.
- `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/configuration/KtorConfig.kt` — the HTTP server: embedded Netty on `ktor.port`, mounts every `ControllerTemplate`, installs content negotiation and CORS, and blocks the main thread (`ktor.wait: true`).
- `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/TaskController.kt` — the task route table (`POST /tasks/one-time`, `POST /tasks/periodic`, list/read/executions/history) behind `authorizedSystem()`.
- `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/migration/Migrator.kt` — `main()`; the Flyway migrator shipped as `bin/migrate`, run by `deploy/entrypoint.sh` before the app.
- `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/migration/MigrationScripts.kt` — `main()`; the Exposed migration-script generator (`-Pname=V<n>__x`).
- `backend/integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/config/IntegrationTest.kt` — the integration suite's entry: compose MySQL + WireMock, migrate to head, boot the app.
