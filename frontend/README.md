# chameidor-fe

Frontend do agendador/executor de tasks [`chameidor`](../backend). SPA em
React + Vite + TypeScript + Tailwind v4 + TanStack Query + React Router,
consumindo a API HTTP do backend. Mesma stack e convenções do sibling
[`shougong/frontend`](../../shougong/frontend).

## Pré-requisitos

- **Node.js 20+**.
- O backend `chameidor` rodando em `http://localhost:8080` (ver
  [`../backend/README.md`](../backend/README.md)).

## Rodar em dev

```bash
npm install
npm run dev
```

Abre em `http://localhost:5273`. As chamadas para `/api/*` são _proxied_ para
`http://localhost:8080` (config em `vite.config.ts`, ajustável por
`VITE_API_TARGET`) — assim não precisa mexer em CORS no backend.

> No PowerShell, se `npm` for bloqueado pela _execution policy_
> (`npm.ps1 cannot be loaded`), use `npm.cmd install` / `npm.cmd run dev`.

## Build

```bash
npm run build      # gera dist/ com base path /chameidor/ (para o reverse proxy)
npm run preview
```

Para buildar na raiz (`/`) em vez de `/chameidor/`: `VITE_BASE=/ npm run build`
(é o que o `Dockerfile` da raiz faz).

## Telas

| Rota          | O quê                                                                       |
| ------------- | -------------------------------------------------------------------------- |
| `/`           | Painel: contagem de tasks por status, saúde do backend, próximas execuções, execuções recentes |
| `/tasks`      | Lista de tasks com filtro por status e por `X-External-System`             |
| `/tasks/:id`  | Detalhe da task — **próxima leva**                                          |
| `/tasks/new`  | Registrar task (one-time / periódica) — **próxima leva**                    |

## Notas de arquitetura

- `src/api/types.ts` espelha os DTOs `*Response` de
  `backend/http-api/.../controller/TaskResponse.kt` (mais os corpos de request
  de `TaskController` e o `HealthCheckResult`). **Manter os dois lados em sync
  no mesmo commit.**
- `src/api/client.ts` — `fetch` tipado + `ApiError`; base em `VITE_API_BASE`.
  `src/api/queries.ts` — hooks TanStack Query (`keys`, `use*`).
- `src/lib/taskStatus.ts` — mapa `TaskStatus` → label pt-BR + cor + pílula
  (papel análogo ao `lib/srs.ts` do shougong).
- `src/lib/format.ts` — datas/duração relativas em pt-BR.
- `src/components/Panel.tsx` — card de seção compartilhado (extraído do padrão
  inline do Dashboard do shougong).
