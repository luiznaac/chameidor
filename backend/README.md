# chameidor backend

The backend service of the [chameidor monorepo](../README.md). All commands below run from this
`backend/` directory.

A small, self-hosted **task scheduler**. Other applications register tasks with chameidor over
HTTP — "call this URL once at some point" or "call this URL every N minutes forever" — and
chameidor takes care of calling them on time, keeping track of whether each call succeeded, and
telling you the history.

Think of it as a tiny, private alternative to a cron job runner or a cloud task-scheduling
service, meant to be shared by a handful of your own applications.

## Why it exists

If you have several small personal services and each of them needs "do X every 10 minutes" or
"do Y once, five hours from now", you'd otherwise bolt a scheduler onto every single one of them.
chameidor centralizes that: your other apps just register a task once, and chameidor calls them
back when it's time.

## How it works, in short

1. An external system sends an HTTP request to chameidor: "register a task that calls
   `POST /some/endpoint` on host `X` with this JSON payload, either once or on this cron
   schedule."
2. chameidor stores the task in a MySQL database.
3. A background loop wakes up periodically, checks which tasks are due, and calls them.
4. Every attempt (success or failure) is recorded, along with the task's history, so you can see
   what happened and when the next execution is scheduled.

Requests must include an `X-External-System` header — this is how chameidor knows which system a
task belongs to, so many different apps can safely use the same chameidor instance.

## Using it

### Run it locally

You'll need Docker and JDK 21.

```bash
docker compose -f docker-compose.yml up -d mysql   # starts a local MySQL instance
```

Then run the application (main class `dev.agner.chameidor.application.BootKt`) with these
environment variables set:

```
MYSQL_HOST=localhost
MYSQL_USER=root
MYSQL_PASSWORD=
```

The service listens on port `8080`.

### Register a one-time task

```bash
curl -X POST http://localhost:8080/tasks/one-time \
  -H "Content-Type: application/json" \
  -H "X-External-System: my-app" \
  -d '{ "host": "https://example.com", "endpoint": "/do-thing", "data": {"key": "value"} }'
```

### Register a periodic (cron) task

```bash
curl -X POST http://localhost:8080/tasks/periodic \
  -H "Content-Type: application/json" \
  -H "X-External-System: my-app" \
  -d '{ "host": "https://example.com", "endpoint": "/do-thing", "data": {"key": "value"}, "cron": "*/10 * * * *" }'
```

chameidor will call `POST https://example.com/do-thing` with the given payload according to the
schedule, and keep retrying it going forward for periodic tasks.

### Inspect tasks

```bash
curl http://localhost:8080/tasks                 # list (filters: ?status=, ?created_by=, ?limit=, ?offset=)
curl http://localhost:8080/tasks/1               # one task
curl http://localhost:8080/tasks/1/executions    # its run history
curl http://localhost:8080/tasks/1/history       # its state-change audit trail
curl http://localhost:8080/tasks/executions      # recent executions across all tasks
```

These endpoints back the [`frontend/`](../frontend) SPA.

### Build & test

```bash
./gradlew clean build   # compile, lint, run unit tests
./gradlew test          # unit tests only
```

## Where things live

- `application` — the entry point and configuration.
- `http-api` — the HTTP endpoints you call to register tasks.
- `usecase` — the core scheduling logic.
- `persistence` — how tasks and their history are stored in MySQL.
- `gateway` — how chameidor calls back into the systems that registered tasks.

See [CLAUDE.md](CLAUDE.md) if you're going to make changes — it documents the architecture and
conventions in detail.

## Deployment

The published `luiznaac/chameidor` Docker image is the **combined** backend + SPA image built from
the [repo-root `Dockerfile`](../Dockerfile) (see [../README.md](../README.md)). `backend/Dockerfile`
still builds a backend-only image if you want just the API.
