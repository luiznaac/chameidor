import { Link } from "react-router-dom";
import type { SystemSummary } from "../lib/systems.ts";
import { fromNow } from "../lib/format.ts";
import { TASK_STATUSES, TASK_STATUS_META } from "../lib/taskStatus.ts";

function pct(rate: number | null): string {
  return rate == null ? "—" : `${Math.round(rate * 100)}%`;
}

export function SystemCard({ summary }: { summary: SystemSummary }) {
  const { name, taskCount, periodic, oneTime, statusMix, successRate, lastFailureAt } =
    summary;
  const total = TASK_STATUSES.reduce((n, s) => n + statusMix[s], 0);

  return (
    <Link
      to={`/systems/${encodeURIComponent(name)}`}
      className="block rounded-xl border border-white/10 bg-slate-900/50 p-4 transition-colors hover:border-white/20 hover:bg-slate-900"
    >
      <div className="flex items-center gap-2 text-base font-semibold text-slate-100">
        <span
          className="h-2 w-2 shrink-0 rounded-full"
          style={{
            backgroundColor:
              successRate != null && successRate < 0.9
                ? "var(--color-exec-failure)"
                : "var(--color-accent-500)",
          }}
        />
        {name}
      </div>
      <div className="mt-0.5 text-xs text-slate-500">
        {taskCount} {taskCount === 1 ? "task" : "tasks"} · {periodic} periódicas ·{" "}
        {oneTime} únicas
      </div>

      <div className="mt-3 flex h-1.5 overflow-hidden rounded-full bg-slate-800">
        {total > 0 &&
          TASK_STATUSES.map((s) =>
            statusMix[s] > 0 ? (
              <span
                key={s}
                title={`${TASK_STATUS_META[s].label}: ${statusMix[s]}`}
                style={{
                  width: `${(statusMix[s] / total) * 100}%`,
                  backgroundColor: TASK_STATUS_META[s].color,
                }}
              />
            ) : null,
          )}
      </div>

      <div className="mt-3 flex justify-between text-xs">
        <span className="text-slate-500">Sucesso (janela)</span>
        <span
          className={[
            "font-medium tabular-nums",
            successRate != null && successRate < 0.9
              ? "text-exec-failure"
              : "text-slate-200",
          ].join(" ")}
        >
          {pct(successRate)}
        </span>
      </div>
      <div className="mt-1 flex justify-between text-xs">
        <span className="text-slate-500">Última falha</span>
        <span className="text-slate-300" title={lastFailureAt ?? ""}>
          {lastFailureAt ? fromNow(lastFailureAt) : "sem falhas"}
        </span>
      </div>
    </Link>
  );
}
