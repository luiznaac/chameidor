import { useMemo } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import {
  RECENT_EXECUTIONS_LIMIT,
  useAllTasks,
  useRecentExecutions,
} from "../api/queries.ts";
import { buildTaskMap, enrichExecutions } from "../lib/executions.ts";
import { distinctSystems, systemDetailStats } from "../lib/systems.ts";
import { fromNow } from "../lib/format.ts";
import { Breadcrumb } from "../components/Breadcrumb.tsx";
import { KpiTile } from "../components/KpiTile.tsx";
import { Panel } from "../components/Panel.tsx";
import { TasksTable } from "../components/TasksTable.tsx";
import { ExecutionBrowser } from "../components/ExecutionBrowser.tsx";

function ratePct(rate: number | null): string {
  return rate == null ? "—" : `${Math.round(rate * 100)}%`;
}

export function SystemDetail() {
  const name = decodeURIComponent(useParams().name ?? "");
  const [params] = useSearchParams();
  const limit = Number(params.get("limit")) || RECENT_EXECUTIONS_LIMIT;

  const tasksQ = useAllTasks();
  const execQ = useRecentExecutions(limit);

  const view = useMemo(() => {
    if (!tasksQ.data) return null;
    const known = distinctSystems(tasksQ.data).includes(name);
    const map = buildTaskMap(tasksQ.data);
    const enriched = enrichExecutions(execQ.data ?? [], map);
    const tasks = tasksQ.data.filter((t) => t.created_by === name);
    const execs = enriched.filter((e) => e.system === name);
    return {
      known,
      tasks,
      execs,
      stats: systemDetailStats(tasks, execs),
      failures: execs.filter((e) => !e.success).slice(0, 8),
    };
  }, [tasksQ.data, execQ.data, name]);

  return (
    <div className="space-y-4">
      <Breadcrumb items={[{ label: "Sistemas", to: "/systems" }, { label: name }]} />

      {tasksQ.isLoading && <p className="text-slate-400">Carregando…</p>}
      {tasksQ.error && (
        <p className="text-exec-failure">
          Falha ao carregar: {String(tasksQ.error)}
        </p>
      )}

      {view && !view.known && (
        <div className="rounded-xl border border-white/10 bg-slate-900/50 p-8 text-center">
          <p className="text-sm text-slate-400">Sistema não encontrado.</p>
          <Link
            to="/systems"
            className="mt-3 inline-block text-sm text-accent-400 hover:underline"
          >
            ← Voltar para sistemas
          </Link>
        </div>
      )}

      {view && view.known && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-lg font-semibold text-slate-100">{name}</h1>
            <span className="rounded-full bg-accent-500/10 px-2 py-0.5 text-xs font-medium text-accent-400">
              X-External-System
            </span>
            <Link
              to={`/tasks/new?system=${encodeURIComponent(name)}`}
              className="ml-auto rounded-md bg-accent-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-accent-500"
            >
              Nova task
            </Link>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
            <KpiTile
              label="Tasks"
              value={view.stats.taskCount}
              hint={`${view.tasks.filter((t) => t.type === "periodic").length} periódicas`}
            />
            <KpiTile
              label="Sucesso 24 h"
              value={ratePct(view.stats.successRate24h)}
            />
            <KpiTile
              label="Execuções (janela)"
              value={view.stats.execCount}
              hint={`${view.stats.failedCount} falharam`}
            />
            <KpiTile
              label="Falhas"
              value={view.stats.failedCount}
            />
            <KpiTile
              label="p95 duração"
              value={
                view.stats.p95Ms == null
                  ? "—"
                  : `${(view.stats.p95Ms / 1000).toFixed(2)} s`
              }
              hint={view.stats.smallSample ? "amostra pequena" : undefined}
            />
          </div>

          <Panel title="Tasks do sistema">
            <TasksTable
              tasks={view.tasks}
              emptyText="Nenhuma task nesse sistema."
            />
          </Panel>

          <Panel title="Execuções (janela)">
            <ExecutionBrowser
              enriched={view.execs}
              mode="embedded"
              lockedSystem={name}
            />
          </Panel>

          <Panel
            title="Falhas recentes"
            action={
              <Link
                to={`/executions?system=${encodeURIComponent(name)}&status=FAILURE`}
                className="text-xs text-accent-400 hover:underline"
              >
                ver no explorer →
              </Link>
            }
          >
            {view.failures.length === 0 ? (
              <p className="text-sm text-slate-500">
                Nenhuma falha na janela carregada.
              </p>
            ) : (
              <ul className="divide-y divide-white/5 text-sm">
                {view.failures.map((e) => (
                  <li key={e.id} className="flex flex-wrap items-baseline gap-x-3 py-2">
                    <span className="font-mono text-xs text-slate-400">
                      #{e.task_id} {e.endpoint}
                    </span>
                    <span className="text-exec-failure">
                      {typeof e.result === "object" && e.result
                        ? String(
                            (e.result as Record<string, unknown>).message ?? "falha",
                          )
                        : "falha"}
                    </span>
                    <span
                      className="ml-auto text-xs text-slate-500"
                      title={e.executed_at}
                    >
                      {fromNow(e.executed_at)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>
        </>
      )}
    </div>
  );
}
