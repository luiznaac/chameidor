# chameidor

Monorepo for the self-hosted task scheduler/executor.

| Path                     | What                                                                                                  |
| ------------------------ | ---------------------------------------------------------------------------------------------------- |
| [`backend/`](backend/)   | Kotlin/Ktor + Spring-DI + Exposed/MySQL service. See [backend/README.md](backend/README.md) and [backend/AGENTS.md](backend/AGENTS.md). |
| [`frontend/`](frontend/) | React + Vite SPA — dashboard and task browser. See [frontend/README.md](frontend/README.md).         |

The frontend's `src/api/types.ts` mirrors the backend's edge DTOs (`http-api/.../controller/TaskResponse.kt` et al.); keep them in sync in the same change — that's the reason these two live in one repo.

The wire contract other apps integrate against — request/response fixtures for task registration and the callback — lives in [`contracts/`](contracts/README.md); each consumer pins its own copy and tests its real payload against it.

## Dev

```bash
npm run setup      # npm install in frontend/
npm run db         # start MySQL (docker) for the backend
```

Then, in two shells:

```bash
npm run be:build && (cd backend && ./gradlew :application:run)   # backend  -> http://localhost:8100
npm run fe:dev                                                   # frontend -> http://localhost:8103  (proxies /api -> :8100)
```

(Or run `dev.agner.chameidor.application.BootKt` from IntelliJ — see `backend/.run/BootKt.run.xml`.)

### Ports

Local host ports follow the family's canonical allocation block. Only the **host** side moves;
container-internal ports stay fixed (`API_PORT=8080`, `WEB_PORT=8081`, MySQL `3306`).

| Role | URL | Host env key | Container-internal |
|---|---|---|---|
| `api` | `http://localhost:8100` | `KTOR_PORT` | `8080` |
| `web` | `http://localhost:8101` | `WEB_HOST_PORT` | `8081` |
| `mysql` | `localhost:8102` | `MYSQL_PORT` | `3306` |
| `frontend-dev` | `http://localhost:8103` | `FRONTEND_DEV_PORT` | — |

The committed [`ports.json`](.salgadinhos/ports.json) records the block; a local env file may
override any host port. The integration suite binds none of them — it takes free ports at run time.

## Docker

One image holds both processes — the Ktor app (API) and `nginx` (the built SPA, which also reverse-proxies `/api` to the app) — supervised by `supervisord`, on separate ports. The database is **not** in the image.

```bash
docker compose up --build     # app + MySQL, full stack
#   UI   -> http://localhost:8101
#   API  -> http://localhost:8100
docker compose -f backend/docker-compose.yml up -d mysql   # just the DB
```

The host ports are configurable with `KTOR_PORT` / `WEB_HOST_PORT` / `MYSQL_PORT` (see the port table above).

**Publishing:** every push to `master` that touches `backend/`, `frontend/`, `Dockerfile`, or `deploy/` builds and pushes `luiznaac/chameidor:latest` and `:v<run-number>` (a sequential build number) to Docker Hub (`.github/workflows/docker-image.yml`).

## Checks

```bash
npm run check      # backend clean build + frontend typecheck + build
```

CI (`.github/workflows/test.yml`) runs the backend and frontend jobs independently.

`package.json` at the repo root is only a script shim (no dependencies) — the real toolchains are Gradle in `backend/` and npm in `frontend/`.
