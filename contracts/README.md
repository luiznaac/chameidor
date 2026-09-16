# Wire contracts

chameidor's HTTP surface is the integration contract other apps depend on. There is no
OpenAPI spec; the fixtures in this directory are the reference. They are kept honest by
chameidor's own tests, which assert the real request/response payloads against them.

## Registering a task

### `POST /tasks/one-time`

Headers:

- `Content-Type: application/json`
- `X-External-System: <caller>` (required) — caller identity, stored as `created_by`.

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
- Callback headers and auth are planned and will be additive: creator identity, task id, and
  bearer-token auth in both directions. Today `X-External-System` is the only credential (on
  registration) and the callback request carries no identifying headers.
