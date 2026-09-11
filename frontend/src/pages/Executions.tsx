import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import {
  RECENT_EXECUTIONS_LIMIT,
  useAllTasks,
  useRecentExecutions,
} from "../api/queries.ts";
import { buildTaskMap, enrichExecutions } from "../lib/executions.ts";
import { distinctSystems } from "../lib/systems.ts";
import { ExecutionBrowser } from "../components/ExecutionBrowser.tsx";

const LIMITS = [50, 100, 200, 500];

export function Executions() {
  const [params, setParams] = useSearchParams();
  const limit = Number(params.get("limit")) || RECENT_EXECUTIONS_LIMIT;

  const tasksQ = useAllTasks();
  const execQ = useRecentExecutions(limit);

  const { enriched, systems } = useMemo(() => {
    const tasks = tasksQ.data ?? [];
    const map = buildTaskMap(tasks);
    return {
      enriched: enrichExecutions(execQ.data ?? [], map),
      systems: distinctSystems(tasks),
    };
  }, [tasksQ.data, execQ.data]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-lg font-semibold text-slate-100">Execuções</h1>
        <span className="text-sm text-slate-500">
          janela carregada: {execQ.data?.length ?? 0}
          {execQ.isFetching && " · atualizando…"}
        </span>
        <select
          value={limit}
          onChange={(e) => {
            const next = new URLSearchParams(params);
            next.set("limit", e.target.value);
            setParams(next, { replace: true });
          }}
          className="ml-auto rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200"
        >
          {LIMITS.map((n) => (
            <option key={n} value={n}>
              {n} execuções
            </option>
          ))}
        </select>
      </div>

      {tasksQ.isLoading && <p className="text-slate-400">Carregando…</p>}
      {(tasksQ.error || execQ.error) && (
        <p className="text-exec-failure">
          Falha ao carregar: {String(tasksQ.error ?? execQ.error)}
        </p>
      )}

      {!tasksQ.isLoading && (
        <ExecutionBrowser enriched={enriched} mode="page" systems={systems} />
      )}
    </div>
  );
}
