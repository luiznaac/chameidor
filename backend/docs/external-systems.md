# External systems registry

The allowlist of systems allowed to register tasks in chameidor. A new app joins the
ecosystem by sending a PR that adds a row to [the registry](#the-registry) below; the
actual token is provisioned out of band (nobody's token belongs in git).

## How a caller authenticates

```
Authorization: Bearer <token>
```

- **The token is the identity.** It is opaque to callers, prefixed by app name for human
  identification only, has at least 32 characters of entropy, and carries no embedded
  expiry.
- chameidor stores only the token's SHA-256 hex digest in the `external_systems` table and
  looks it up on every request, so **rotation and deactivation are plain `UPDATE`s — no
  redeploy**.
- The system name resolved from the token becomes the task's `created_by`.
- Scope is all-or-nothing today. The single `AuthorizationService.authorize(caller, target,
  credential)` seam in `usecase/auth/` is where a future centralized authz provider plugs
  in without changing the wire.
- The legacy `X-External-System` header is not a credential: when present it may only
  accompany a Bearer token, and it must match the token's system.

Errors are minimal and JSON:

| Status | Body error | When |
|---|---|---|
| `401` | `missing bearer credential` | No credential, or a malformed/non-Bearer `Authorization` header |
| `401` | `invalid bearer credential` | Unknown token, or the `X-External-System` claim conflicts with it |
| `403` | `inactive external system` | Token belongs to a system with `active = FALSE` |

## The registry

| name | app | token prefix | status |
|---|---|---|---|
| `valoab` | `valoab` (Python) | `valoab-` | active |
| `portfolio` | `portfolio-2` (Kotlin) | `portfolio-` | active |

`name` is the identity chameidor records as `created_by` — keep it stable, ≤ 50 chars, and
lowercase. A new app opens a PR adding its row here; callers never get to choose a name at
request time.

## Provisioning a token

Generate one (prefix by app, 32 random bytes):

```bash
echo "valoab-$(openssl rand -base64 32)"
```

Register it (replace `<token>` with the generated value; `SHA2(..., 256)` is MySQL's
SHA-256):

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

## Local development

Fixture literals follow the canonical `dev-<app>-<suffix>` shape (e.g.
`dev-valoab-fixture-token`). They exist **only** in local databases and test suites — never
insert them into a shared environment:

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
