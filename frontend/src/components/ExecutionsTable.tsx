import { Link } from "react-router-dom";
import type { TaskExecutionResponse } from "../api/types.ts";
import { formatDuration, fromNow } from "../lib/format.ts";
import { isExecutionSuccess } from "../lib/taskStatus.ts";

export function ExecutionsTable({
  executions,
  showTask = true,
}: {
  executions: TaskExecutionResponse[];
  showTask?: boolean;
}) {
  if (executions.length === 0) {
    return <p className="text-sm text-slate-500">Nenhuma execução ainda.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs uppercase tracking-wide text-slate-500">
            {showTask && <th className="pb-2 pr-4 font-medium">Task</th>}
            <th className="pb-2 pr-4 font-medium">Resultado</th>
            <th className="pb-2 pr-4 font-medium">Duração</th>
            <th className="pb-2 font-medium">Quando</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {executions.map((e) => (
            <tr key={e.id}>
              {showTask && (
                <td className="py-2 pr-4">
                  <Link to={`/tasks/${e.task_id}`} className="text-accent-400 hover:underline">
                    #{e.task_id}
                  </Link>
                </td>
              )}
              <td className="py-2 pr-4">
                <span
                  className={
                    isExecutionSuccess(e.status)
                      ? "text-exec-success"
                      : "text-exec-failure"
                  }
                >
                  {isExecutionSuccess(e.status) ? "Sucesso" : "Falha"}
                </span>
              </td>
              <td className="py-2 pr-4 tabular-nums text-slate-400">
                {formatDuration(e.duration_ms)}
              </td>
              <td className="py-2 text-slate-500" title={e.executed_at}>
                {fromNow(e.executed_at)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
