import { Link } from "react-router-dom";
import type { TaskResponse } from "../api/types.ts";
import { formatDateTime, fromNow } from "../lib/format.ts";

const PENDING: TaskResponse["status"][] = ["WAITING", "QUEUED"];

export function UpcomingExecutions({ tasks }: { tasks: TaskResponse[] }) {
  const upcoming = tasks
    .filter((t) => t.next_execution_at != null && PENDING.includes(t.status))
    .sort(
      (a, b) =>
        new Date(a.next_execution_at!).getTime() - new Date(b.next_execution_at!).getTime(),
    )
    .slice(0, 8);

  if (upcoming.length === 0) {
    return <p className="text-sm text-slate-500">Nada agendado.</p>;
  }

  return (
    <ul className="space-y-2 text-sm">
      {upcoming.map((t) => (
        <li key={t.id} className="flex items-baseline gap-3">
          <Link to={`/tasks/${t.id}`} className="text-accent-400 hover:underline">
            #{t.id}
          </Link>
          <span className="truncate text-slate-300">
            {t.host}
            {t.endpoint}
          </span>
          <span
            className="ml-auto shrink-0 text-slate-500"
            title={formatDateTime(t.next_execution_at!)}
          >
            {fromNow(t.next_execution_at!)}
          </span>
        </li>
      ))}
    </ul>
  );
}
