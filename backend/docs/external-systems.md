# External systems registry

The allowlist of systems allowed to register tasks in chameidor. A new app joins the ecosystem by sending a PR that adds a row to [the registry](#the-registry) below; the actual token is provisioned out of band (nobody's token belongs in git).

## How a caller authenticates

```
Authorization: Bearer <token>
```

- **The token is the identity.** It is opaque to callers, prefixed by app name for human identification only, has at least 32 characters of entropy, and carries no embedded expiry.
- chameidor stores only the token's SHA-256 hex digest in the `external_systems` table and looks it up on every request, so **rotation and deactivation are plain `UPDATE`s — no redeploy**.
- The system name resolved from the token becomes the task's `created_by`.
- Scope is all-or-nothing today. The single `AuthorizationService.authorize(caller, target, credential)` seam in `usecase/auth/` is where a future centralized authz provider plugs in without changing the wire.
- The legacy `X-External-System` header is a **deprecated alias**: without a token it authenticates by name alone — the use is logged at WARNING — until every caller has migrated to Bearer. Alongside a token it must match the token's system. New consumers never use the alias; see [Removing the alias](#removing-the-alias).

Errors are minimal and JSON:

| Status | Body error | When |
|---|---|---|
| `401` | `missing bearer credential` | No credential and no alias — including a malformed/non-Bearer `Authorization` header |
| `401` | `invalid bearer credential` | Unknown token, a standalone alias naming an unregistered system, or an alias conflicting with the token |
| `403` | `inactive external system` | Token or alias belongs to a system with `active = FALSE` |

## Removing the alias

`X-External-System` is a migration artifact, not part of the contract. It goes away once every caller sends Bearer:

1. portfolio-2's `ChameidorGateway` sends `Authorization: Bearer` (the [#16][16] effort).
2. valoab sends Bearer — validated end to end by the canary [#19][19].
3. A chameidor PR then deletes the alias path: `AuthorizationService` stops resolving the header by name, the header reverts to a claim that may only accompany a matching token (standalone it is a `401 missing bearer credential`), and the tests and docs here drop the alias.

The SPA's dormant task-creation client (`frontend/src/api/client.ts`) still sends the header; the form that will use it is ComingSoon, and it must use Bearer — never the alias — when built.

Tracked in [#23][23].

[16]: https://github.com/luiznaac/chameidor/issues/16
[19]: https://github.com/luiznaac/chameidor/issues/19
[23]: https://github.com/luiznaac/chameidor/issues/23

## The registry

| name | app | token prefix | status |
|---|---|---|---|
| `valoab` | `valoab` (Python) | `valoab-` | active |
| `portfolio` | `portfolio-2` (Kotlin) | `portfolio-` | active |

`name` is the identity chameidor records as `created_by` — keep it stable, ≤ 50 chars, and lowercase. A new app opens a PR adding its row here; callers never get to choose a name at request time.

## Provisioning a token

Generate one (prefix by app, 32 random bytes):

```bash
echo "valoab-$(openssl rand -base64 32)"
```

Register it (replace `<token>` with the generated value; `SHA2(..., 256)` is MySQL's SHA-256):

```sql
INSERT INTO external_systems (name, token_hash, active)
VALUES ('valoab', SHA2('<token>', 256), TRUE);
```

Rotate a token:

```sql
UPDATE external_systems SET token_hash = SHA2('<new token>', 256) WHERE name = 'valoab';
```

Deactivate a system:

```sql
UPDATE external_systems SET active = FALSE WHERE name = 'valoab';
```

## The callback (chameidor → consumers)

Auth is **symmetric**: when a task runs, chameidor presents its own identity token to the consumer as `Authorization: Bearer`, generated with the same convention as every app token (prefix `chameidor-`, opacity, ≥32 characters of entropy). It travels in `CHAMEIDOR_TOKEN`, which is **required** — the app refuses to boot without it, and it never gets a real default in code.

```
POST http://{task.host}{task.endpoint}
Authorization: Bearer <CHAMEIDOR_TOKEN>
X-External-System: valoab
X-Chameidor-Task-Id: 42
```

- Each consumer keeps only the token's SHA-256 in its own registry — the same shape as this one — and validates `Authorization` on every callback. A refused credential means the task is recorded as a failure; it does not silently succeed.
- `X-External-System` is the system that registered the task (its `created_by`), **not** the caller's identity: under the deprecated-alias rule, consumers must not authenticate by it.
- The earlier "no auth validation for now" note for the callback is superseded by the ecosystem's consolidated interop decision (§5.3) — see [contracts/README.md](../../contracts/README.md#callback-chameidor--consumer) for the wire.

Provision it out of band, like any app token (`chameidor` is the name a consumer registers):

```bash
echo "chameidor-$(openssl rand -base64 32)"
```

```sql
-- On each consumer's registry, not in chameidor's:
INSERT INTO external_systems (name, token_hash, active)
VALUES ('chameidor', SHA2('<token>', 256), TRUE);
```

Local development uses the canonical fixture `dev-chameidor-fixture-token` — chameidor's own `application-test.yaml`, the root `docker-compose.yml` and the IntelliJ run configuration seed it; a consumer validates its hash the same way it does for real tokens.

## Local development

Fixture literals follow the canonical `dev-<app>-<suffix>` shape (e.g. `dev-valoab-fixture-token`). They exist **only** in local databases and test suites — never insert them into a shared environment:

```sql
INSERT INTO external_systems (name, token_hash, active)
VALUES ('valoab', SHA2('dev-valoab-fixture-token', 256), TRUE);
```

Then use it as any caller would:

```bash
curl -X POST http://localhost:8080/tasks/one-time \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-valoab-fixture-token" \
  -d '{ "host": "https://example.com", "endpoint": "/do-thing", "data": {"key": "value"} }'
```
