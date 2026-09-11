# chameidor

Monorepo for the self-hosted task scheduler/executor.

| Path                     | What                                                                                                  |
| ------------------------ | ---------------------------------------------------------------------------------------------------- |
| [`backend/`](backend/)   | Kotlin/Ktor + Spring-DI + Exposed/MySQL service. See [backend/README.md](backend/README.md) and [backend/AGENTS.md](backend/AGENTS.md). |
| [`frontend/`](frontend/) | React + Vite SPA — dashboard and task browser. See [frontend/README.md](frontend/README.md).         |

The frontend's `src/api/types.ts` mirrors the backend's edge DTOs
(`http-api/.../controller/TaskResponse.kt` et al.); keep them in sync in the same
change — that's the reason these two live in one repo.

## Dev

```bash
npm run setup      # npm install in frontend/
npm run db         # start MySQL (docker) for the backend
```

Then, in two shells:

```bash
npm run be:build && (cd backend && ./gradlew :application:run)   # backend  -> http://localhost:8080
npm run fe:dev                                                   # frontend -> http://localhost:5273  (proxies /api -> :8080)
```

(Or run `dev.agner.chameidor.application.BootKt` from IntelliJ — see
`backend/.run/BootKt.run.xml`.)

## Docker

One image holds both processes — the Ktor app (API) and `nginx` (the built SPA,
which also reverse-proxies `/api` to the app) — supervised by `supervisord`, on
separate ports. The database is **not** in the image.

```bash
docker compose up --build     # app + MySQL, full stack
#   UI   -> http://localhost:8081
#   API  -> http://localhost:8080
docker compose -f backend/docker-compose.yml up -d mysql   # just the DB
```

Ports are configurable with `API_PORT` / `WEB_PORT`.

**Publishing:** every push to `master` that touches `backend/`, `frontend/`,
`Dockerfile`, or `deploy/` builds and pushes `luiznaac/chameidor:latest` and
`:v<run-number>` (a sequential build number) to Docker Hub (`.github/workflows/docker-image.yml`).

## Checks

```bash
npm run check      # backend clean build + frontend typecheck + build
```

CI (`.github/workflows/test.yml`) runs the backend and frontend jobs
independently.

`package.json` at the repo root is only a script shim (no dependencies) — the
real toolchains are Gradle in `backend/` and npm in `frontend/`.
