# AGENTS.md — chameidor frontend

React 19 + Vite + TypeScript + Tailwind v4 + TanStack Query + React Router SPA for the task
scheduler — same stack and conventions across all of luiznaac's frontends, see salgadinhos'
`react-spa-screen` skill for the shared parts (Tailwind v4-in-CSS, TanStack Query v5 object
syntax, the `api/`→`components/`→`pages/`→`lib/` layout, the dev proxy/base-path setup). This
file only covers what's specific to chameidor.

## Commands

```bash
npm run dev         # vite dev server
npm run typecheck    # tsc -b --noEmit
npm run lint          # biome check . (lint + format)
npm run lint:fix      # biome check --write .
npm run test          # vitest run — src/lib/** only, no DOM
npm run check         # typecheck + lint + test — run this before opening a PR
npm run build         # tsc -b && vite build
```

`biome.json` formats with `lineEnding: "lf"` and `frontend/.gitattributes` pins the tree to
LF, so Biome is identical on Windows and Linux — don't set `lineEnding: "crlf"` to quiet a
Windows-only diff, it fails CI.

## Chameidor-specific pieces

- **`api/types.ts` mirrors `TaskResponse`/`TaskCreation`/`HealthCheckResult`** from
  `backend/http-api/.../controller/`, snake_case + null-omitted per `JsonMapper` — see the root
  [`AGENTS.md`](../AGENTS.md)'s cross-cutting rule. `TaskStatus`/`TaskType` are the two enums to
  keep in sync if the backend adds a value.
- **Pages today**: `Dashboard`, `Tasks` (flat list), and `ComingSoon` as the placeholder for
  `/tasks/new` and `/tasks/:id` — those are genuinely unimplemented, not broken; check the current
  route tree in `App.tsx` before assuming a page exists.
- **`components/`**: `ExecutionsTable`, `HealthPanel`, `StatusBadge`, `UpcomingExecutions` are
  chameidor-specific; `Layout`/`Panel` are the shared chrome primitives (same idea as every
  sibling frontend's `Layout`/`Panel`).

## Git

**Do not commit directly to `master`.** Always create a feature branch and open a PR, even for a
small or "obviously safe" change. This applies to all contributors.
