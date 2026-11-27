# Code map

Hand-authored navigation: module → files that matter → the seam a change crosses, plus the app entry points. Read it to go from a named seam to the file a change belongs to without a `find`/`ls` sweep. Vocabulary is the `codebase-design` one (**module**, **interface**, **seam**, **adapter**).

chameidor is a monorepo of two lanes — the Gradle multi-module backend (`application → http-api → usecase ← persistence`, `gateway → usecase`) and the React SPA — bound by the repo-root wire contracts and one combined image. Each backend Gradle module is one block, then the frontend SPA, then the root clusters.

## Backend modules

### `backend/usecase` — domain, ports and services
**Purpose**: the core the other layers implement against — the task model and scheduling loop, the auth seam, the health ports and the shared JSON/clock configuration. Depends on Spring DI annotations, `kotlinx-*`, Jackson, Logback and `kotlin-reflect`; never on Ktor, Exposed or HTTP types.
**Files**: `backend/usecase/src/main/kotlin/dev/agner/chameidor/usecase/task/{Task,TaskCreation,TaskView,TaskService,TaskExecutor,TaskQueryService}.kt`, `task/{ITaskRepository,ITaskExecutionRepository,CallClient,TaskJobHealthCheck}.kt`; `usecase/auth/{AuthorizationService,AuthorizationResult,ExternalSystem,TokenHasher,IExternalSystemRepository}.kt`; `usecase/health/{HealthChecker,HealthGateway,HealthCheckResult,HealthTelemetry}.kt`; `usecase/configuration/{JsonMapper,TimeProviderConfig}.kt` and `configuration/serializers/*`; `usecase/commons/{CommonExtensions,MetricCatalog,TelemetryAttributes,RequestContext,TelemetryLogging}.kt`; `usecase/src/testFixtures/kotlin/dev/agner/chameidor/usecase/BasicHelpers.kt`
**Seam(s)**:
- **`ITaskRepository`** (`task/ITaskRepository.kt`) — the persisted-task port consumed by `TaskService`/`TaskExecutor`; every mutation must also write the audit row through `TaskRepository.saveToHistory`. Callers name the interface, never `TaskRepository`.
- **`ITaskExecutionRepository`** (`task/ITaskExecutionRepository.kt`) — execution-outcome port; the sealed `TaskExecutionResult` (success/failure, duration) is the record shape.
- **`CallClient`** (`task/CallClient.kt`) — outbound callback port implemented by `gateway`'s `CallGateway`; keep `gateway` depending on `usecase`, never the reverse.
- **`AuthorizationService.authorize(...)`** (`auth/AuthorizationService.kt`) — the one auth decision: resolve the bearer token (SHA-256 via `TokenHasher`) or the deprecated `X-External-System` alias against `IExternalSystemRepository`, returning `AuthorizationResult`. No Ktor request object crosses this seam.
- **`HealthChecker` / `HealthGateway`** (`health/`) — inbound health port (implemented by the MySQL and outbound-HTTP checks) and outbound health port (implemented by `AppHealthGateway`).
- **`HealthTelemetry.record(...)`** (`health/HealthTelemetry.kt`) — the `health.check.*` gauge/counter and the transition-only logs; the HTTP edge calls it, it never performs I/O in the gauge callback.
- **`RequestContext` / `event(...)`** (`commons/RequestContext.kt`, `commons/TelemetryLogging.kt`) — the correlation carrier and the structured log call; `MetricCatalog` + `TelemetryAttributes` are the shared contract names. `usecase` may use the OTel API only, never the SDK (`backend/docs/observability.md` §6).
- **`TaskCreation` / `TaskStatus`** (`task/TaskCreation.kt`, `task/Task.kt`) — the one-time/periodic discriminator and the status enum mirrored by the persistence table (`cron` nullable) and by `frontend/src/api/types.ts`.
- **`TaskService.execute()` loop** (`task/TaskService.kt`) — the background poll loop; `destroy()` cancels it, so a new lifecycle hook must preserve that cancellation. Each enqueue mints a `job.id`, carried into `TaskExecutor.execute(task, jobId)`, which emits the single `tasks.execution.failed` ERROR; the loop runs on `GlobalScope`, so no `request.id` reaches it.

### `backend/persistence` — Exposed/MySQL adapters and migrations
**Purpose**: implements the `usecase` repository ports against MySQL via Exposed, and owns the Flyway migration tooling.
**Files**: `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/task/{TaskTable,TaskHistoryTable,TaskExecutionTable,TaskRepository,TaskExecutionRepository}.kt`; `persistence/auth/{ExternalSystemTable,ExternalSystemRepository}.kt`; `persistence/configuration/DatabaseConfig.kt`; `persistence/health/MySqlConnectionHealthCheck.kt`; `persistence/migration/{MigrationScripts,Migrator}.kt`; `persistence/src/main/resources/db/migration/V*.sql`
**Seam(s)**:
- **Adapter seam** — each `@Component … : I*Repository` sits at its usecase interface; `TaskRepository` wraps every method in `transaction { … }` and writes `TaskHistoryTable` on mutation. `TransactionService`-style composition does not exist here — a service that needs two repositories stays in one transaction by calling one repository method.
- **Query seam** — `TaskTable.idx_status_next_execution` backs `findExecutableTasks` (`status = WAITING AND next_execution_at <= now`); changing the predicate means changing the index in a migration.
- **Migration seam** — a new `Table` object must join `allTables` in `migration/MigrationScripts.kt`; `Migrator.migrate(...)` applies `V*.sql` under `persistence/src/main/resources/db/migration/`, and `MigrationSchemaTest` fails on drift between `allTables` and a migrated database.
- **Connection seam** — `DatabaseConfig.mysqlJdbcUrl` is shared with `Migrator`, so Exposed and Flyway cannot drift.

### `backend/gateway` — outbound HTTP integrations
**Purpose**: Ktor-client adapters for callback delivery and the self-health probe.
**Files**: `backend/gateway/src/main/kotlin/dev/agner/chameidor/gateway/call/CallGateway.kt`; `gateway/configuration/KtorClientConfig.kt`; `gateway/health/{AppHealthGateway,HttpClientHealthCheck}.kt`; `gateway/src/test/kotlin/dev/agner/chameidor/gateway/{call/CallGatewayContractTest,configuration/ClientTelemetryTest}.kt`
**Seam(s)**:
- **`CallClient` adapter** (`call/CallGateway.kt`) — `TOKEN` from `chameidor.token`, sends `Authorization: Bearer`, `X-External-System` and `X-Chameidor-Task-Id`, and throws on non-2xx. The request body is pinned against `contracts/callback-request.json`.
- **`HealthGateway` adapter** (`health/AppHealthGateway.kt`) — probes the service's own `GET /health/internal` on `ktor.port` (`API_PORT` at runtime), not `WEB_PORT`; its component id is `app-health`.
- **`KtorClientConfig`** — the one shared `HttpClient` bean (Jackson negotiation, `KtorClientTelemetry` for `http.client.request.duration`, and the `X-Request-ID` propagation plugin); a new integration adds an adapter here, not another client. `configureTelemetry(mapper, openTelemetry)` is the seam its contract test drives.

### `backend/http-api` — Ktor routes and edge adapters
**Purpose**: the HTTP surface — controllers, DTOs, content negotiation, CORS and the Ktor server bootstrap.
**Files**: `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/{ControllerTemplate,TaskController,HealthController}.kt` and `controller/{TaskResponse,TaskRegistrationResponse,ErrorResponse}.kt`; `http-api/src/main/kotlin/dev/agner/chameidor/httpapi/configuration/{KtorConfig,RequestTelemetry,ErrorHandling}.kt`; `http-api/src/test/kotlin/dev/agner/chameidor/httpapi/{controller/ContractFixturesTest,configuration/RequestTelemetryTest}.kt`
**Seam(s)**:
- **`ControllerTemplate.routes()`** (`controller/ControllerTemplate.kt`) — `typealias RouteDefinition = Routing.() -> Unit`; `KtorConfig` injects `Set<ControllerTemplate>` and mounts each one, so adding an endpoint is adding a `@Component` controller and never editing `KtorConfig`.
- **`RequestTelemetry.install(...)`** (`configuration/RequestTelemetry.kt`) — installed before routing: binds `X-Request-ID`, echoes it, keeps `http.server.active_requests` honest and emits the single boundary record (4xx WARN driven from the response status, 5xx ERROR with exception semconv, success silent). `installErrorHandling()` owns the 500 response body; do not add a second boundary logger.
- **Authorization gate** (`controller/TaskController.kt`) — `authorizedSystem()` reads the bearer header (and the deprecated alias), calls `AuthorizationService.authorize(...)`, and returns a JSON `ErrorResponse` with 401/403 instead of reaching a usecase.
- **Route precedence** (`controller/TaskController.kt`) — `GET /tasks/executions` is declared before `GET /tasks/{id}` so the literal path is not captured as an id.
- **Wire-DTO seam** (`controller/*Response.kt`) — snake_case, null-omitted JSON produced by the shared `JsonMapper`; the same shapes are mirrored in `frontend/src/api/types.ts` and pinned by `contracts/`.

### `backend/application` — composition root
**Purpose**: the Spring Boot entry and runtime configuration; wires the modules.
**Files**: `backend/application/src/main/kotlin/dev/agner/chameidor/application/Boot.kt`; `application/src/main/kotlin/dev/agner/chameidor/application/telemetry/{Telemetry,TelemetryConfig,TelemetryResource,TelemetryViews,RedactingMdcFilter,TelemetryJsonProviders}.kt`; `application/src/main/resources/{application.yaml,application-prod.yaml,logback-spring.xml}`
**Seam(s)**: `Boot.kt`'s `@ComponentScan("dev.agner.chameidor")` is the wiring seam — every `@Component`/`@Service` is picked up, so a new class needs no registration. `application.yaml` is the config seam (`ktor.port`, `ktor.wait: true`, `mysql.*`, `chameidor.token`, `enqueue.period`, `telemetry.*`). `TelemetryConfig` is the one SDK composition seam: it builds the resource/exporters/views, installs the Logback appender, exposes the `OpenTelemetry`/`Meter`/`HealthTelemetry` beans and flushes on `destroy()` (10 s). `logback-spring.xml` is the log bridge (OTel appender + prod JSON / dev console) with the redaction TurboFilter as the single chokepoint; `build-info.properties` supplies `service.version`.

### `backend/integrationTest` — full-stack test harness
**Purpose**: end-to-end specs that boot the real app against a Testcontainers MySQL and stubbed callbacks, on free ports.
**Files**: `backend/integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/config/{IntegrationTest,MysqlContainerExtension,HttpMockService,HttpRequestService,ClockMock,KotestConfig,ResetWiremock,StopKtorServer}.kt`; `integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/tests/{MigrationSchemaTest,HarnessContractTest,TaskCallbackAuthenticationTest,TaskRegistrationAuthorizationTest}.kt`; `integrationTest/src/test/resources/{application-test.yaml,mysql-test-conf/my.cnf}`
**Seam(s)**: `MysqlContainerExtension` starts one Testcontainers MySQL per test JVM, publishes its coordinates and a probed free app port as system properties (outranking `application.yaml`), and migrates the container to head before any spec; `IntegrationTest` boots `Boot` with the `test` profile; `HttpRequestService` reads the app port from the Spring environment; `HttpMockService` binds a dynamic WireMock port and exposes `baseUrl`/`host`. `MigrationSchemaTest` is the schema guard for a changed `Table`; `HarnessContractTest` pins the no-fixed-port contract. Copy this module as-is when the harness is needed elsewhere.

## Frontend SPA

### `frontend/src` — the React/Vite app
**Purpose**: the browser surface — a typed backend adapter, the routed screens and their widgets, and the pure formatting helpers. React 19 + Vite + TypeScript, Tailwind v4-in-CSS, TanStack Query v5 and React Router; `api/` → `components/` → `pages/` → `lib/`.
**Files**: `frontend/src/api/{client,types,queries}.ts`; `frontend/src/pages/{Dashboard,Tasks,ComingSoon}.tsx`; `frontend/src/components/{Layout,Panel,StatusBadge,ExecutionsTable,UpcomingExecutions,HealthPanel}.tsx`; `frontend/src/lib/{taskStatus,format}.ts` and their `*.test.ts`; `frontend/src/index.css`; `frontend/vite.config.ts`; `frontend/src/{main,App}.tsx`; `frontend/index.html`; `frontend/package.json`; `frontend/{tsconfig*.json,biome.json,vitest.config.ts}`
**Seam(s)**:
- **DTO mirror** (`api/types.ts`) — mirrors `backend/http-api/.../controller/*.kt` (`TaskResponse`, `TaskRegistrationResponse`, `TaskCreation`, `HealthCheckResult`, `ErrorResponse`, plus the list filter); snake_case and null-omitted per `JsonMapper`. Changing a backend DTO updates this file in the same commit, and `TaskStatus`/`TaskType` must gain any new enum value.
- **Fetch adapter** (`api/client.ts`) — `BASE` is `VITE_API_BASE ?? "/api"`; `ApiError` carries `status` and the `{error|message|detail}` detail; the `request<T>` helper treats `204` as `undefined`. The dev proxy in `vite.config.ts` rewrites `/api` → the backend, so a new endpoint adds a method to `api`, not a second fetch wrapper.
- **Query seam** (`api/queries.ts`) — the `keys` factory is the cache-key contract (`tasks`, `task`, `executions`, `history`, `recentExecutions`, `health`); a mutation invalidates the key it changed (`useCreateTask` → `["tasks"]`). `useHealth` polls every 30 s.
- **Status dashboard** (`pages/Dashboard.tsx`) — reads `useTasks()` + `useRecentExecutions()` and derives the four status counts through `TASK_STATUSES`/`TASK_STATUS_META`; a new status must land in `lib/taskStatus.ts`, not here.
- **Task list** (`pages/Tasks.tsx`) — maps URL search params (`status`) plus the `created_by` input into a `TaskListFilter`; the `status` param is the same link target the dashboard uses.
- **Placeholders** (`pages/ComingSoon.tsx`) — `/tasks/new` and `/tasks/:id` are deliberately unimplemented; check `App.tsx` before assuming a page exists.
- **Chrome / primitive seams** (`components/Layout.tsx`, `components/Panel.tsx`) — the header nav and `<Outlet />` every routed page renders into, and the titled card every panel composes.
- **Task widgets** (`components/{ExecutionsTable,UpcomingExecutions,HealthPanel}.tsx`) — render `TaskExecutionResponse[]`, filter `WAITING`/`QUEUED` by `next_execution_at`, and render `HealthCheckResult[]`; they compose `Panel`, not bespoke containers.
- **Status metadata** (`lib/taskStatus.ts`) — `TASK_STATUS_META: Record<TaskStatus, {label,pill,color}>` is the single status→label/colour map, `TASK_STATUSES` its key order, and `isExecutionSuccess` the outcome predicate; a new backend status fails the `Record` type until it is added here.
- **Formatting** (`lib/format.ts`) — `fromNow`, `formatDateTime`, `formatDate`, `formatDuration`, all `pt-BR`; UI code formats through these, never with an inline `Intl` call.
- **Theme seam** (`src/index.css`) — the `@theme` block owns the accent, the four task-status colours (`--color-status-*`) and the execution-success/failure colours; components consume the CSS variables/classes by name, never a raw hex.
- **Base-path seam** (`vite.config.ts`) — `base` is `/chameidor/` in production and `/` in dev, overridable with `VITE_BASE`; `main.tsx` derives the router `basename` from `import.meta.env.BASE_URL`.
- **Dev server** (`vite.config.ts`) — `loadEnv` reads the env files, `port: FRONTEND_DEV_PORT ?? 8103` with `strictPort: true` (a taken port fails loudly instead of drifting off the block), and the `/api` proxy targets `VITE_API_TARGET ?? http://localhost:8100`.
- **Quality gate** (`package.json`) — `npm run check` = `typecheck` + Biome + Vitest; `npm test` runs `src/lib/**` only (no DOM).

## Repo root — contracts, deploy and CI
**Purpose**: the cross-cutting fixtures and packaging that bind backend + frontend into one image.
**Files**: `contracts/{tasks-one-time-request,tasks-one-time-response,tasks-periodic-request,tasks-periodic-response,callback-request}.json`, `contracts/README.md`; `deploy/{entrypoint.sh,supervisord.conf,nginx.conf.template}`; `Dockerfile`, `docker-compose.yml`, `backend/docker-compose.yml`; `package.json`; `scripts/release.sh`; `.github/workflows/{ci,docker-publish}.yml`, `.github/release.yml`, `CHANGELOG.md`
**Seam(s)**:
- **Contract fixtures** — consumers pin the JSON in `contracts/`; `ContractFixturesTest` keeps them equal to the real payloads, so a DTO change edits the fixture in the same commit.
- **Container entry** (`deploy/entrypoint.sh`) — renders the nginx template, runs `/opt/app/bin/migrate` (Flyway), then `exec supervisord`; a failed migration aborts the container. `API_PORT` (app) and `WEB_PORT` (nginx, reverse-proxying `/api`) are the two ports the combined image separates.
- **Script shims** (`package.json`) — `be:unit` is the Docker-free `unitTest` lane, `be:integration` the Docker-backed `:integrationTest:test`, `be:run` the local dev server; `db`/`db:migrate` drive the compose MySQL. Not a real Node package.
- **Release flow** (`docker-publish.yml` + `scripts/release.sh`) — a pull request builds the root image with `push: false`; a `v*` tag cut by the script publishes immutable semver/short-SHA tags and moves `latest`; a merge to `master` never publishes.

## Entry points

- `backend/application/src/main/kotlin/dev/agner/chameidor/application/Boot.kt` — `main()`; Spring Boot component-scans `dev.agner.chameidor` and starts the process.
- `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/configuration/KtorConfig.kt` — the HTTP server: embedded Netty on `ktor.port`, mounts every `ControllerTemplate`, installs content negotiation and CORS, and blocks the main thread (`ktor.wait: true`).
- `backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/TaskController.kt` — the task route table (`POST /tasks/one-time`, `POST /tasks/periodic`, list/read/executions/history) behind `authorizedSystem()`.
- `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/migration/Migrator.kt` — `main()`; the Flyway migrator shipped as `bin/migrate`, run by `deploy/entrypoint.sh` before the app.
- `backend/persistence/src/main/kotlin/dev/agner/chameidor/persistence/migration/MigrationScripts.kt` — `main()`; the Exposed migration-script generator (`-Pname=V<n>__x`).
- `backend/integrationTest/src/test/kotlin/dev/agner/chameidor/integrationTest/config/IntegrationTest.kt` — the integration suite's entry: compose MySQL + WireMock, migrate to head, boot the app.
- `frontend/src/main.tsx` — mounts React `StrictMode` with the `QueryClient` and the browser router.
- `frontend/src/App.tsx` — the route table: `/` under `Layout` (index `Dashboard`, `tasks`, `tasks/new`, `tasks/:id`).
- `frontend/index.html` — the Vite HTML shell with the `#root` mount.
- `frontend/vite.config.ts` — dev server, base path and API proxy.
