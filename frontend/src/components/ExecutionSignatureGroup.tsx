import type { EnrichedExecution, SignatureGroup } from "../lib/executions.ts";
import { formatDuration, fromNow } from "../lib/format.ts";
import { Sparkline } from "./Sparkline.tsx";

export function ExecutionSignatureGroup({
  group,
  expanded,
  onToggle,
  onSelect,
  selectedId,
}: {
  group: SignatureGroup;
  expanded: boolean;
  onToggle: () => void;
  onSelect: (exec: EnrichedExecution) => void;
  selectedId?: number;
}) {
  return (
    <div className="border-t border-white/5 first:border-t-0">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        className="flex w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-slate-900"
      >
        <span className="w-2.5 shrink-0 font-mono text-xs text-slate-600">
          {expanded ? "▾" : "▸"}
        </span>
        <span
          className="h-2 w-2 shrink-0 rounded-full"
          style={{
            backgroundColor: group.success
              ? "var(--color-exec-success)"
              : "var(--color-exec-failure)",
          }}
        />
        <span className="truncate font-mono text-xs text-slate-200">
          {group.label}
        </span>
        <span className="ml-auto shrink-0">
          <Sparkline data={group.spark} />
        </span>
        <span className="shrink-0 tabular-nums text-xs text-slate-500">
          {group.count}×
        </span>
        <span
          className="shrink-0 text-xs text-slate-600"
          title={group.lastAt}
        >
          {fromNow(group.lastAt)}
        </span>
      </button>

      {expanded && (
        <div className="bg-slate-950/40">
          {group.executions.map((e) => (
            <button
              key={e.id}
              type="button"
              onClick={() => onSelect(e)}
              className={[
                "grid w-full grid-cols-[64px_1fr_90px_92px] items-center gap-3 py-2 pl-10 pr-4 text-left text-xs hover:bg-slate-900",
                e.id === selectedId
                  ? "bg-accent-500/10 shadow-[inset_2px_0_0_var(--color-accent-500)]"
                  : "",
              ].join(" ")}
            >
              <span className="font-mono text-slate-500">#{e.task_id}</span>
              <span
                className={e.success ? "text-exec-success" : "text-exec-failure"}
              >
                {e.success ? "Sucesso" : "Falha"}
              </span>
              <span className="text-right tabular-nums text-slate-400">
                {formatDuration(e.duration_ms)}
              </span>
              <span className="text-right tabular-nums text-slate-500" title={e.executed_at}>
                {fromNow(e.executed_at)}
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
