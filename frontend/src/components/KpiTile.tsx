import type { ReactNode } from "react";

export function KpiTile({
  label,
  value,
  hint,
}: {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
}) {
  return (
    <div className="rounded-xl border border-white/10 bg-slate-900/50 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1.5 text-2xl font-semibold tabular-nums text-slate-100">
        {value}
      </div>
      {hint != null && <div className="mt-0.5 text-xs text-slate-500">{hint}</div>}
    </div>
  );
}
