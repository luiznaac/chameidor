# Wire contracts

chameidor's HTTP surface is the integration contract other apps depend on. There is no
OpenAPI spec; the fixtures in this directory are the reference. They are kept honest by
chameidor's own tests, which assert the real request/response payloads against them.

## Registering a task

### `POST /tasks/one-time`

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer <token>` (required) — the token resolves to the registered system,
  whose name is stored as `created_by`.
- `X-External-System: <name>` — deprecated alias, accepted without a token during the
  migration window (the use is logged at WARNING); alongside a token it must match the
  token's system. New consumers never use it — it will be removed, see
  [the registry doc](../backend/docs/external-systems.md#removing-the-alias).

Request (fixture: [`tasks-one-time-request.json`](tasks-one-time-request.json)):

| Field      | Type            | Required | Meaning                                                        |
| ---------- | --------------- | -------- | -------------------------------------------------------------- |
| `host`     | string          | yes      | Callback target authority without scheme, e.g. `portfolio:8080`. |
| `endpoint` | string          | yes      | Path on the target, e.g. `/consolidations/BOND/1`.             |
| `data`     | any JSON value  | no       | Payload echoed to the callback; omitted when absent.           |

Response `201 Created` (fixture: [`tasks-one-time-response.json`](tasks-one-time-response.json)) — the
stored task as registered: `id`, `host`, `endpoint`, and `data` when present. This is not the
full task view served by the read endpoints (`GET /tasks/{id}`).

### `POST /tasks/periodic`

Same as one-time plus `cron` (fixture: [`tasks-periodic-request.json`](tasks-periodic-request.json))
— a required 5-field cron expression, e.g. `*/5 * * * *`. Response `201 Created` adds `cron`
to the stored task (fixture: [`tasks-periodic-response.json`](tasks-periodic-response.json)).

## Callback (chameidor → consumer)

When a task runs, chameidor sends `POST http://{host}{endpoint}`:

- Body: the registered `data`, exactly as sent — no envelope, no task metadata. When the task
  has no `data`, the request has no body. [`callback-request.json`](callback-request.json) is
  the body of a task registered with `"data": {"product_id": 1}`.
- `Content-Type: application/json` when there is a body.
- 2xx means success; a JSON-object response body is recorded as the execution result. Anything
  else is recorded as a failure.

## Evolution

- Changes are **additive**; there is no version in the URL. New response fields may appear and
  unknown request fields are ignored.
- Each consumer pins its own copy of the fixtures it depends on and tests its real payload
  against them: a breaking change on this side fails the consumer's CI when the pin is updated.
- Registration requires the `Authorization: Bearer` token; the `X-External-System` alias is
  temporary and will be removed, so consumers must not build on it. Callback headers and auth
  (creator identity, task id, bearer token) are planned and will be additive; the callback
  request currently carries no identifying headers.
