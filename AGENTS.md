# AGENTS.md — chameidor monorepo

Development guidelines for anyone (human, agent, or tool) working in this repository.

Two projects, one repo:

- **`backend/`** — the Kotlin/Ktor + Spring-DI + Exposed task scheduler/executor.
  All backend commands run from `backend/` (`cd backend && ./gradlew <task>`). Architecture,
  conventions and rules for evolving it are in [backend/AGENTS.md](backend/AGENTS.md) —
  read that before touching `backend/`.
- **`frontend/`** — the React/Vite SPA. Commands run from `frontend/`
  (`npm --prefix frontend run <script>`). Details in
  [frontend/README.md](frontend/README.md).

## Cross-cutting rule

`frontend/src/api/types.ts` is a hand-maintained mirror of the edge DTOs in
`backend/http-api/src/main/kotlin/dev/agner/chameidor/httpapi/controller/`
(`TaskResponse.kt` for the read endpoints, `TaskCreation` / `HealthCheckResult`
for the rest). They are serialized by `JsonMapper` — snake_case keys, null
fields omitted, dates as ISO-8601 strings. Any change to a request/response DTO
on one side must update the other **in the same commit** — that's the reason
these two live in one repo.

## Git workflow

**Do not commit directly to `master`.** Always create a feature branch and open a PR,
even for a small or "obviously safe" change. This applies to all contributors.

## Tooling

Root `package.json` holds script shims only (`npm run be:build`, `npm run
fe:build`, `npm run check`, `npm run db`, `npm run db:migrate`, `npm run
db:generate -- -Pname=V2__x`, `npm run up`). It has no dependencies
and is not a real package. `.pre-commit-config.yaml` lives at the root and scopes
hooks by path (`^backend/` → `./gradlew detekt`, `^frontend/` → `npm run
typecheck`). It also carries `no-commit-to-branch`, which refuses a commit made
while `master` is checked out — the "don't commit to master" rule below is
enforced here, not merely stated.

## Docker

One image (repo-root `Dockerfile`, multi-stage) ships backend + frontend
together: `supervisord` runs the Ktor app (`API_PORT`/8080) and `nginx`
(`deploy/nginx.conf.template` — serves the built SPA on `WEB_PORT`/8081 and
reverse-proxies `/api` → the app). No DB in the image. `docker-compose.yml` at
the root adds MySQL for full-stack / DB-only local runs. `backend/docker-compose.yml`
is the MySQL-only compose for backend-only local runs. The schema comes from
`backend/persistence/src/main/resources/db/migration/V*.sql`, applied by Flyway
(`bin/migrate` in the image) from `deploy/entrypoint.sh` before the app starts —
see [backend/AGENTS.md](backend/AGENTS.md) §7.
`.github/workflows/docker-publish.yml` pushes `luiznaac/chameidor:latest` +
`:v<run-number>` (a sequential build number, `github.run_number`) after the "CI" workflow
succeeds on `master`.

## Related repositories

Same monorepo shape (backend + Vite SPA, combined image) as
[../shougong](../shougong/AGENTS.md). The backend shares its hexagonal
architecture with [../portfolio-2](../portfolio-2/AGENTS.md) and the
[../environments](../environments/AGENTS.md) kotlin scaffold.
