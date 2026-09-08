import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useTasks } from "../api/queries.ts";
import type { TaskListFilter, TaskStatus } from "../api/types.ts";
import { fromNow } from "../lib/format.ts";
import { TASK_STATUSES, TASK_STATUS_META } from "../lib/taskStatus.ts";
import { StatusBadge } from "../components/StatusBadge.tsx";

function isStatus(v: string | null): v is TaskStatus {
  return v != null && (TASK_STATUSES as string[]).includes(v);
}

export function Tasks() {
  const [params, setParams] = useSearchParams();
  const status = params.get("status");
  const [createdBy, setCreatedBy] = useState("");

  const filter = useMemo<TaskListFilter>(
    () => ({
      status: isStatus(status) ? status : undefined,
      created_by: createdBy.trim() || undefined,
    }),
    [status, createdBy],
  );

  const { data: tasks, isLoading, error } = useTasks(filter);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-lg font-semibold text-slate-100">Tasks</h1>
        <div className="ml-auto flex flex-wrap items-center gap-2">
          <select
            value={isStatus(status) ? status : ""}
            onChange={(e) => {
              const next = new URLSearchParams(params);
              if (e.target.value) next.set("status", e.target.value);
              else next.delete("status");
              setParams(next, { replace: true });
            }}
            className="rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200"
          >
            <option value="">Todos os status</option>
            {TASK_STATUSES.map((s) => (
              <option key={s} value={s}>
                {TASK_STATUS_META[s].label}
              </option>
            ))}
          </select>
          <input
            value={createdBy}
            onChange={(e) => setCreatedBy(e.target.value)}
            placeholder="X-External-System"
            className="rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-600"
          />
          <Link
            to="/tasks/new"
            className="rounded-md bg-accent-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-accent-500"
          >
            Nova task
          </Link>
        </div>
      </div>

      {isLoading && <p className="text-slate-400">Carregando…</p>}
      {error && <p className="text-exec-failure">Falha ao carregar: {String(error)}</p>}

      {tasks && (
        <div className="overflow-x-auto rounded-xl border border-white/10 bg-slate-900/50">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-medium">#</th>
                <th className="px-4 py-3 font-medium">Tipo</th>
                <th className="px-4 py-3 font-medium">Alvo</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Criada por</th>
                <th className="px-4 py-3 font-medium">Próxima</th>
                <th className="px-4 py-3 font-medium">Última</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {tasks.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-500">
                    Nenhuma task para esse filtro.
                  </td>
                </tr>
              )}
              {tasks.map((t) => (
                <tr key={t.id} className="hover:bg-slate-900">
                  <td className="px-4 py-3">
                    <Link to={`/tasks/${t.id}`} className="text-accent-400 hover:underline">
                      {t.id}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    {t.type === "periodic" ? (
                      <span className="font-mono text-xs text-slate-300">{t.cron}</span>
                    ) : (
                      <span className="text-slate-500">única</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-slate-300">
                      {t.host}
                      <span className="text-slate-500">{t.endpoint}</span>
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={t.status} />
                  </td>
                  <td className="px-4 py-3 text-slate-400">{t.created_by}</td>
                  <td className="px-4 py-3 text-slate-400" title={t.next_execution_at ?? ""}>
                    {t.next_execution_at ? fromNow(t.next_execution_at) : "—"}
                  </td>
                  <td className="px-4 py-3 text-slate-400" title={t.executed_at ?? ""}>
                    {t.executed_at ? fromNow(t.executed_at) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
