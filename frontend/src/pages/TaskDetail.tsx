import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError } from "../api/client.ts";
import { useExecutions, useHistory, useTask } from "../api/queries.ts";
import { describeCron, nextRuns } from "../lib/cron.ts";
import { formatDateTime } from "../lib/format.ts";
import { Breadcrumb } from "../components/Breadcrumb.tsx";
import { ExecutionsTable } from "../components/ExecutionsTable.tsx";
import { JsonPanel } from "../components/JsonPanel.tsx";
import { Panel } from "../components/Panel.tsx";
import { StatusBadge } from "../components/StatusBadge.tsx";

export function TaskDetail() {
  const id = Number(useParams().id);
  const taskQ = useTask(id);
  const execQ = useExecutions(id);
  const historyQ = useHistory(id);

  const notFound = taskQ.error instanceof ApiError && taskQ.error.status === 404;
  const task = taskQ.data;

  return (
    <div className="space-y-4">
      <Breadcrumb
        items={[
          { label: "Tasks", to: "/tasks" },
          ...(task
            ? [{ label: task.created_by, to: `/systems/${encodeURIComponent(task.created_by)}` }]
            : []),
          { label: `#${id}` },
        ]}
      />

      {taskQ.isLoading && <p className="text-slate-400">Carregando…</p>}
      {notFound && (
        <div className="rounded-xl border border-white/10 bg-slate-900/50 p-8 text-center">
          <p className="text-sm text-slate-400">Task não encontrada.</p>
          <Link
            to="/tasks"
            className="mt-3 inline-block text-sm text-accent-400 hover:underline"
          >
            ← Voltar para tasks
          </Link>
        </div>
      )}
      {taskQ.error && !notFound && (
        <p className="text-exec-failure">
          Falha ao carregar: {String(taskQ.error)}
        </p>
      )}

      {task && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-lg font-semibold text-slate-100">
              Task #{task.id}
            </h1>
            <StatusBadge status={task.status} />
            <Link
              to={`/tasks/new?from=${task.id}`}
              className="ml-auto rounded-md border border-white/10 px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800"
            >
              Duplicar
            </Link>
          </div>

          <Panel title="Configuração">
            <dl className="grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
              <Row label="Tipo">
                {task.type === "periodic" ? "Periódica" : "Única"}
              </Row>
              <Row label="Criada por">
                <Link
                  to={`/systems/${encodeURIComponent(task.created_by)}`}
                  className="text-accent-400 hover:underline"
                >
                  {task.created_by}
                </Link>
              </Row>
              <Row label="Alvo">
                <span className="font-mono text-xs text-slate-300">
                  {task.host}
                  {task.endpoint}
                </span>
              </Row>
              <Row label="Criada em">{formatDateTime(task.created_at)}</Row>
              {task.cron && (
                <Row label="Cron">
                  <span className="font-mono text-xs text-slate-300">
                    {task.cron}
                  </span>{" "}
                  <span className="text-slate-500">— {describeCron(task.cron)}</span>
                </Row>
              )}
              <Row label="Próxima execução">
                {task.next_execution_at
                  ? formatDateTime(task.next_execution_at)
                  : "—"}
              </Row>
              <Row label="Última execução">
                {task.executed_at ? formatDateTime(task.executed_at) : "—"}
              </Row>
            </dl>
            {task.cron && (
              <p className="mt-3 font-mono text-xs text-slate-500">
                próximas:{" "}
                {nextRuns(task.cron, 3)
                  .map((d) => formatDateTime(d.toISOString()))
                  .join(" · ")}
              </p>
            )}
            <div className="mt-4">
              <JsonPanel label="data" value={task.data} />
            </div>
          </Panel>

          <Panel title="Execuções">
            {execQ.isLoading ? (
              <p className="text-sm text-slate-500">Carregando…</p>
            ) : (
              <ExecutionsTable executions={execQ.data ?? []} showTask={false} />
            )}
          </Panel>

          <Panel title="Histórico">
            {historyQ.isLoading ? (
              <p className="text-sm text-slate-500">Carregando…</p>
            ) : (historyQ.data?.length ?? 0) === 0 ? (
              <p className="text-sm text-slate-500">Sem histórico.</p>
            ) : (
              <ul className="space-y-2 text-sm">
                {historyQ.data!.map((h, i) => (
                  <li key={i} className="flex items-baseline gap-3">
                    <span className="text-slate-300">{h.status}</span>
                    <span className="text-slate-500" title={h.created_at}>
                      {formatDateTime(h.created_at)}
                    </span>
                    {h.next_execution_at && (
                      <span className="ml-auto text-xs text-slate-500">
                        próxima: {formatDateTime(h.next_execution_at)}
                      </span>
                    )}
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

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 text-slate-300">{children}</dd>
    </div>
  );
}
