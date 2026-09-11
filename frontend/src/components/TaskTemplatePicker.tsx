import { useMemo, useState } from "react";
import type { TaskResponse } from "../api/types.ts";

const controlClass =
  "w-full rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-600";

function optionLabel(t: TaskResponse): string {
  const base = `#${t.id} · ${t.host}${t.endpoint} · ${t.created_by}`;
  return t.type === "periodic" && t.cron ? `${base} · ${t.cron}` : `${base} · (única)`;
}

export function TaskTemplatePicker({
  tasks,
  onPick,
}: {
  tasks: TaskResponse[];
  onPick: (task: TaskResponse) => void;
}) {
  const [q, setQ] = useState("");

  const options = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return [...tasks]
      .filter(
        (t) =>
          !needle ||
          `${t.id} ${t.host}${t.endpoint} ${t.created_by} ${t.cron ?? ""}`
            .toLowerCase()
            .includes(needle),
      )
      .sort((a, b) => b.id - a.id);
  }, [tasks, q]);

  return (
    <div className="space-y-2 border-b border-white/10 pb-4">
      <label className="block text-xs uppercase tracking-wide text-slate-500">
        Usar task existente como template
      </label>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="filtrar por id, host, endpoint, sistema…"
        className={controlClass}
      />
      <select
        value=""
        onChange={(e) => {
          const picked = options.find((t) => String(t.id) === e.target.value);
          if (picked) onPick(picked);
        }}
        className={controlClass}
      >
        <option value="">— começar em branco —</option>
        {options.map((t) => (
          <option key={t.id} value={t.id}>
            {optionLabel(t)}
          </option>
        ))}
      </select>
      <p className="text-xs text-slate-500">
        preenche tipo, host, endpoint, sistema, payload e cron — tudo continua
        editável.
      </p>
    </div>
  );
}
