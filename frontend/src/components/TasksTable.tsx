import { Link } from "react-router-dom";
import type { TaskResponse } from "../api/types.ts";
import { fromNow } from "../lib/format.ts";
import { StatusBadge } from "./StatusBadge.tsx";

export function TasksTable({
  tasks,
  emptyText = "Nenhuma task.",
}: {
  tasks: TaskResponse[];
  emptyText?: string;
}) {
  return (
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
                {emptyText}
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
              <td className="px-4 py-3">
                <Link
                  to={`/systems/${encodeURIComponent(t.created_by)}`}
                  className="text-slate-400 hover:text-slate-200 hover:underline"
                >
                  {t.created_by}
                </Link>
              </td>
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
  );
}
