# chameidor domain glossary

Vocabulary for the chameidor scheduler/executor, in the sense this repo uses it.

## Language

**Task**:
A unit of scheduled work — a target `host` + `endpoint` and an optional JSON `data` payload that chameidor calls when due. A task is either one-time or periodic.
_Avoid_: job, cron entry

**One-time task**:
A task chameidor runs once at its next due time, then stops.
_Avoid_: single task, immediate task

**Periodic task**:
A task carrying a cron expression; after each run chameidor schedules its next occurrence.
_Avoid_: recurring task, cron job

**Registration**:
The act of a consumer creating a task over HTTP; the stored task is the registration.
_Avoid_: creation, submission

**Consumer**:
An app that registers tasks and receives chameidor's callbacks; it joins the ecosystem by registering in chameidor's external-systems registry under a stable `name`.
_Avoid_: client, user

**External system**:
The registry entry a consumer's credential resolves to; its `name` becomes the task's creator.
_Avoid_: app, integration

**Caller**:
The identity a request authenticates as — the external system its bearer token (or the deprecated alias) resolves to.
_Avoid_: consumer (that is the app, not the credential)

**Creator**:
The external-system name recorded on a task as `created_by` — the consumer that registered it.
_Avoid_: owner, author

**Callback**:
The outbound request chameidor makes to a task's target when it runs, carrying chameidor's own bearer token, the creator and the task id.
_Avoid_: webhook, notification

**Execution**:
One attempt to deliver a task's callback, recorded with its outcome (success or failure) and duration.
_Avoid_: run, invocation

**Job**:
The identity minted when a task is enqueued for execution; it correlates that execution's logs.
_Avoid_: execution (a job is the enqueue identity, an execution is the outcome)

**Task status**:
The lifecycle state of a task — `WAITING`, `QUEUED`, `EXECUTING`, `EXECUTED`.
_Avoid_: state, phase

**Scheduler**:
The part that decides which tasks are due and enqueues them.
_Avoid_: poller, dispatcher

**Executor**:
The part that performs a queued task's callback and records the outcome.
_Avoid_: worker, runner

**Wire contract**:
The request/response shapes chameidor publishes as JSON fixtures under `contracts/`; each consumer pins its own copy and tests its real payload against it.
_Avoid_: API spec, OpenAPI, schema

**DTO mirror**:
The hand-maintained copy of the backend's edge DTOs in the SPA.
_Avoid_: generated types, client model
