# Code map

Module → key files → seam index for the `frontend/` lane, plus the app entry points. React 19 + Vite + TypeScript, Tailwind v4-in-CSS, TanStack Query v5 and React Router; `api/` → `components/` → `pages/` → `lib/`. Vocabulary: **module**, **interface**, **seam**, **adapter**.

## Modules

### `src/api` — backend adapter and wire types
**Purpose**: the only place that talks HTTP — the fetch wrapper, the hand-mirrored backend DTOs and the TanStack Query hooks.
**Files**: `frontend/src/api/client.ts`, `frontend/src/api/types.ts`, `frontend/src/api/queries.ts`
**Seam(s)**:
- **DTO mirror** (`api/types.ts`) — mirrors `backend/http-api/.../controller/*.kt` (`TaskResponse`, `TaskRegistrationResponse`, `TaskCreation`, `HealthCheckResult`, `ErrorResponse`, plus the list filter); snake_case and null-omitted per `JsonMapper`. Changing a backend DTO updates this file in the same commit, and `TaskStatus`/`TaskType` must gain any new enum value.
- **Fetch adapter** (`api/client.ts`) — `BASE` is `VITE_API_BASE ?? "/api"`; `ApiError` carries `status` and the `{error|message|detail}` detail; the `request<T>` helper treats `204` as `undefined`. The dev proxy in `vite.config.ts` rewrites `/api` → the backend, so a new endpoint adds a method to `api`, not a second fetch wrapper.
- **Query seam** (`api/queries.ts`) — the `keys` factory is the cache-key contract (`tasks`, `task`, `executions`, `history`, `recentExecutions`, `health`); a mutation invalidates the key it changed (`useCreateTask` → `["tasks"]`). `useHealth` polls every 30 s.

### `src/pages` — routed screens
**Purpose**: the three screens — the status dashboard, the task list and the placeholder detail/registration pages.
**Files**: `frontend/src/pages/Dashboard.tsx`, `frontend/src/pages/Tasks.tsx`, `frontend/src/pages/ComingSoon.tsx`
**Seam(s)**:
- **Status dashboard** (`pages/Dashboard.tsx`) — reads `useTasks()` + `useRecentExecutions()` and derives the four status counts through `TASK_STATUSES`/`TASK_STATUS_META`; a new status must land in `lib/taskStatus.ts`, not here.
- **Task list** (`pages/Tasks.tsx`) — maps URL search params (`status`) plus the `created_by` input into a `TaskListFilter`; the `status` param is the same link target the dashboard uses.
- **Placeholders** (`pages/ComingSoon.tsx`) — `/tasks/new` and `/tasks/:id` are deliberately unimplemented; check `App.tsx` before assuming a page exists.

### `src/components` — chrome and task widgets
**Purpose**: the shared layout primitives and the four task-specific widgets.
**Files**: `frontend/src/components/Layout.tsx`, `Panel.tsx`, `StatusBadge.tsx`, `ExecutionsTable.tsx`, `UpcomingExecutions.tsx`, `HealthPanel.tsx`
**Seam(s)**:
- **Chrome seam** (`components/Layout.tsx`) — the header nav (`/`, `/tasks`, `/tasks/new`) and the `<Outlet />` every routed page renders into.
- **Primitive seam** (`components/Panel.tsx`) — the titled card every panel uses; new screens compose `Panel`, not a bespoke container.
- **Task widgets** — `StatusBadge` renders `TASK_STATUS_META`, `ExecutionsTable` renders `TaskExecutionResponse[]`, `UpcomingExecutions` filters `WAITING`/`QUEUED` by `next_execution_at`, `HealthPanel` renders `HealthCheckResult[]`.

### `src/lib` — pure helpers
**Purpose**: the unit-tested surface — status metadata and pt-BR formatting.
**Files**: `frontend/src/lib/taskStatus.ts`, `frontend/src/lib/taskStatus.test.ts`, `frontend/src/lib/format.ts`, `frontend/src/lib/format.test.ts`
**Seam(s)**:
- **Status metadata** (`lib/taskStatus.ts`) — `TASK_STATUS_META: Record<TaskStatus, {label,pill,color}>` is the single status→label/colour map, `TASK_STATUSES` its key order, and `isExecutionSuccess` the outcome predicate; a new backend status fails the `Record` type until it is added here.
- **Formatting** (`lib/format.ts`) — `fromNow`, `formatDateTime`, `formatDate`, `formatDuration`, all `pt-BR`; UI code formats through these, never with an inline `Intl` call.

### Theme and root wiring
**Purpose**: the design tokens and the Vite/TypeScript/Biome build that serves the SPA.
**Files**: `frontend/src/index.css`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/package.json`, `frontend/tsconfig*.json`, `frontend/biome.json`, `frontend/vitest.config.ts`
**Seam(s)**:
- **Theme seam** (`src/index.css`) — the `@theme` block owns the accent, the four task-status colours (`--color-status-*`) and the execution-success/failure colours; components consume the CSS variables/classes by name, never a raw hex.
- **Base-path seam** (`vite.config.ts`) — `base` is `/chameidor/` in production and `/` in dev, overridable with `VITE_BASE`; `main.tsx` derives the router `basename` from `import.meta.env.BASE_URL`, so the SPA is served behind the unified dashboard proxy.
- **Dev server** (`vite.config.ts`) — port 5273 with the `/api` proxy to `VITE_API_TARGET ?? http://localhost:8080`.
- **Quality gate** (`package.json`) — `npm run check` = `typecheck` + Biome + Vitest; `npm test` runs `src/lib/**` only (no DOM).

## Entry points

- `frontend/src/main.tsx` — mounts React `StrictMode` with the `QueryClient` and the browser router.
- `frontend/src/App.tsx` — the route table: `/` under `Layout` (index `Dashboard`, `tasks`, `tasks/new`, `tasks/:id`).
- `frontend/index.html` — the Vite HTML shell with the `#root` mount.
- `frontend/vite.config.ts` — dev server, base path and API proxy.
