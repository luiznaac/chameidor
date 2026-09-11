import { useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  RECENT_EXECUTIONS_LIMIT,
  useAllTasks,
  useRecentExecutions,
} from "../api/queries.ts";
import { buildTaskMap, enrichExecutions } from "../lib/executions.ts";
import { summarizeSystems } from "../lib/systems.ts";
import { SystemCard } from "../components/SystemCard.tsx";

const LIMITS = [50, 100, 200, 500];

export function Systems() {
  const [params, setParams] = useSearchParams();
  const limit = Number(params.get("limit")) || RECENT_EXECUTIONS_LIMIT;

  const tasksQ = useAllTasks();
  const execQ = useRecentExecutions(limit);

  const summaries = useMemo(() => {
    if (!tasksQ.data) return [];
    const map = buildTaskMap(tasksQ.data);
    const enriched = enrichExecutions(execQ.data ?? [], map);
    return summarizeSystems(tasksQ.data, enriched);
  }, [tasksQ.data, execQ.data]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-lg font-semibold text-slate-100">Sistemas</h1>
        {tasksQ.data && (
          <span className="text-sm text-slate-500">
            {summaries.length}{" "}
            {summaries.length === 1 ? "sistema" : "sistemas"} · janela: {limit}{" "}
            execuções
          </span>
        )}
        <div className="ml-auto flex items-center gap-2">
          <select
            value={limit}
            onChange={(e) => {
              const next = new URLSearchParams(params);
              next.set("limit", e.target.value);
              setParams(next, { replace: true });
            }}
            className="rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200"
          >
            {LIMITS.map((n) => (
              <option key={n} value={n}>
                janela {n}
              </option>
            ))}
          </select>
          <Link
            to="/tasks/new"
            className="rounded-md bg-accent-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-accent-500"
          >
            Nova task
          </Link>
        </div>
      </div>

      {tasksQ.isLoading && <p className="text-slate-400">Carregando…</p>}
      {tasksQ.error && (
        <p className="text-exec-failure">
          Falha ao carregar: {String(tasksQ.error)}
        </p>
      )}

      {tasksQ.data && summaries.length === 0 && (
        <div className="rounded-xl border border-white/10 bg-slate-900/50 p-8 text-center">
          <p className="text-sm text-slate-400">Nenhum sistema ainda.</p>
          <Link
            to="/tasks/new"
            className="mt-3 inline-block text-sm text-accent-400 hover:underline"
          >
            Registrar a primeira task →
          </Link>
        </div>
      )}

      {summaries.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {summaries.map((s) => (
            <SystemCard key={s.name} summary={s} />
          ))}
        </div>
      )}
    </div>
  );
}
