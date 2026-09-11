import { Link } from "react-router-dom";
import { useRecentExecutions, useTasks } from "../api/queries.ts";
import type { TaskStatus } from "../api/types.ts";
import { ExecutionsTable } from "../components/ExecutionsTable.tsx";
import { HealthPanel } from "../components/HealthPanel.tsx";
import { Panel } from "../components/Panel.tsx";
import { UpcomingExecutions } from "../components/UpcomingExecutions.tsx";
import { TASK_STATUSES, TASK_STATUS_META } from "../lib/taskStatus.ts";

export function Dashboard() {
  const { data: tasks, isLoading, error } = useTasks();
  const { data: recent } = useRecentExecutions();

  if (isLoading) return <p className="text-slate-400">Carregando…</p>;
  if (error) return <p className="text-exec-failure">Falha ao carregar: {String(error)}</p>;
  if (!tasks) return null;

  const counts = TASK_STATUSES.reduce<Record<TaskStatus, number>>(
    (acc, s) => {
      acc[s] = tasks.filter((t) => t.status === s).length;
      return acc;
    },
    { WAITING: 0, QUEUED: 0, EXECUTING: 0, EXECUTED: 0 },
  );

  return (
    <div className="space-y-6">
      <section className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {TASK_STATUSES.map((s) => (
          <Link
            key={s}
            to={`/tasks?status=${s}`}
            className="rounded-lg border border-white/10 bg-slate-900/50 p-4 transition-colors hover:bg-slate-900"
          >
            <div
              className="text-3xl font-semibold tabular-nums"
              style={{ color: TASK_STATUS_META[s].color }}
            >
              {counts[s]}
            </div>
            <div className="mt-1 text-xs uppercase tracking-wide text-slate-500">
              {TASK_STATUS_META[s].label}
            </div>
          </Link>
        ))}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <HealthPanel />
        <Panel title="Próximas execuções">
          <UpcomingExecutions tasks={tasks} />
        </Panel>
      </div>

      <Panel
        title="Execuções recentes"
        action={
          <Link to="/tasks" className="text-xs text-accent-400 hover:underline">
            ver tasks
          </Link>
        }
      >
        <ExecutionsTable executions={recent ?? []} />
      </Panel>

      <Panel title={`Tasks (${tasks.length})`}>
        {tasks.length === 0 ? (
          <p className="text-sm text-slate-500">
            Nenhuma task registrada ainda.{" "}
            <Link to="/tasks/new" className="text-accent-400 hover:underline">
              Criar a primeira
            </Link>
            .
          </p>
        ) : (
          <Link to="/tasks" className="text-sm text-accent-400 hover:underline">
            Ver todas as tasks →
          </Link>
        )}
      </Panel>
    </div>
  );
}
