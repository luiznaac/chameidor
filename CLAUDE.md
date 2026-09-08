# CLAUDE.md — chameidor monorepo

Two projects, one repo:

- **`backend/`** — the Kotlin/Ktor + Spring-DI + Exposed task scheduler/executor.
  All backend commands run from `backend/` (`cd backend && ./gradlew <task>`). Its
  architecture, conventions and the rules for evolving it are in
  [backend/CLAUDE.md](backend/CLAUDE.md) — read that before touching `backend/`.
- **`frontend/`** — the React/Vite SPA. Commands run from `frontend/`
  (`npm --prefix frontend run <script>`). Details in
  [frontend/README.md](frontend/README.md).

## The one cross-cutting rule

`frontend/src/api/types.ts` is a hand-maintained mirror of the edge DTOs in
`backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/`
(`TaskResponse.kt` for the read endpoints, `TaskCreation` / `HealthCheckResult`
for the rest). They are serialized by `JsonMapper` — snake_case keys, null
fields omitted, dates as ISO-8601 strings. Any change to a request/response DTO
on one side must update the other **in the same commit** — that's the reason
these two live in one repo.

## Tooling

Root `package.json` holds script shims only (`npm run be:build`, `npm run
fe:build`, `npm run check`, `npm run db`, `npm run up`). It has no dependencies
and is not a real package. `.pre-commit-config.yaml` lives at the root and scopes
hooks by path (`^backend/` → `./gradlew detekt`, `^frontend/` → `npm run
typecheck`).

## Docker

One image (repo-root `Dockerfile`, multi-stage) ships backend + frontend
together: `supervisord` runs the Ktor app (`API_PORT`/8080) and `nginx`
(`deploy/nginx.conf.template` — serves the built SPA on `WEB_PORT`/8081 and
reverse-proxies `/api` → the app). No DB in the image. `docker-compose.yml` at
the root adds MySQL for full-stack / DB-only local runs. `backend/docker-compose.yml`
is the MySQL-only compose for backend-only local runs.
`.github/workflows/docker-publish.yml` pushes `luiznaac/chameidor:latest` +
`:sha-<short>` after the "CI" workflow succeeds on `master`.

## Related repositories

Same monorepo shape (backend + Vite SPA, combined image) as
[../shougong](../shougong/CLAUDE.md). The backend shares its hexagonal
architecture with [../portfolio-2](../portfolio-2/CLAUDE.md) and the
[../environments](../environments/CLAUDE.md) kotlin scaffold.
