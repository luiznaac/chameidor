# Deploying the combined image

The story: run the published `luiznaac/chameidor` image — the Ktor API and the nginx-served SPA in one container — against a real MySQL and a real `CHAMEIDOR_TOKEN`. The Quickstart runs the image on a dev machine; this is the host step it cannot perform.

## 1. Prepare the database

A MySQL 9 instance reachable from the container, with a database and a user that can create tables. The image applies the schema itself on start; do not seed it by hand.

## 2. Provide the environment

Set the variables the image reads: the names and their dev fixtures are in the repo's [`.env.example`](../../.env.example), and the per-variable notes are in [`backend/AGENTS.md`](../../backend/AGENTS.md) §Configuration. For this deploy that means the database block (`MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_PORT`), `CHAMEIDOR_TOKEN`, the container-internal `API_PORT` / `WEB_PORT` (publish them as the project block's `api` / `web` roles), and the prod telemetry profile's `SPRING_PROFILES_ACTIVE`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME` and `SERVICE_VERSION`. The host Collector must exist before the first deploy.

Keep the real values out of git — a gitignored `.env`, a compose override file or the host's secret store.

## 3. Run the image

```bash
docker run -d --name chameidor --env-file .env -p 8100:8080 -p 8101:8081 luiznaac/chameidor:latest
```

The entrypoint renders the nginx config, runs `bin/migrate` (Flyway) against the database, and only then starts `supervisord`. A failed migration aborts the container instead of serving traffic against a stale schema. Pin the semver image tag (e.g. `1.2.3`, published from the `v1.2.3` release tag) or its short-SHA tag instead of `latest` when the deploy must be reproducible; `latest` moves only on a release.

## 4. Register consumers

A consumer that wants to register tasks needs a row in chameidor's external-systems registry and a bearer token. The token format, the registry `INSERT`/rotation/deactivation SQL and the callback's symmetric auth are in [`backend/docs/external-systems.md`](../../backend/docs/external-systems.md).

## 5. Verify

```bash
curl -fsS http://localhost:8100/health/internal   # {"is_healthy":true}
curl -fsS http://localhost:8100/health            # the per-checker health array
```

The SPA is on the published `web` port; it reverse-proxies `/api` to the app.

## 6. Roll back

Redeploy the previous semver release tag. Migrations are forward-only, so a rollback that crosses a schema change also requires restoring a pre-migration database snapshot.
