import { useState } from "react";
import {
  CRON_PRESETS,
  WEEKDAYS,
  dailyAt,
  weeklyAt,
  describeCron,
  isValidCron,
  nextRuns,
  normalizeCron,
} from "../lib/cron.ts";
import { formatDateTime } from "../lib/format.ts";

const inputClass =
  "rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200";

export function CronField({
  value,
  onChange,
  error,
}: {
  value: string;
  onChange: (value: string) => void;
  error?: string;
}) {
  const [time, setTime] = useState("09:00");
  const [dow, setDow] = useState("");

  const normalized = normalizeCron(value);
  const valid = isValidCron(value);
  const runs = valid ? nextRuns(value, 3) : [];

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-1.5">
        {CRON_PRESETS.map((p) => (
          <button
            key={p.expr}
            type="button"
            aria-pressed={normalized === p.expr}
            onClick={() => onChange(p.expr)}
            className={[
              "rounded-full border px-2.5 py-1 text-xs transition-colors",
              normalized === p.expr
                ? "border-accent-500/40 bg-accent-500/10 text-accent-400"
                : "border-white/10 bg-slate-900 text-slate-400 hover:text-slate-200",
            ].join(" ")}
          >
            {p.label}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <input
          type="time"
          value={time}
          onChange={(e) => setTime(e.target.value)}
          className={inputClass}
        />
        <select
          value={dow}
          onChange={(e) => setDow(e.target.value)}
          className={inputClass}
        >
          <option value="">todo dia</option>
          {WEEKDAYS.map((label, i) => (
            <option key={label} value={i}>
              {label}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={() =>
            onChange(dow === "" ? dailyAt(time) : weeklyAt(Number(dow), time))
          }
          className="rounded-md border border-white/10 px-2.5 py-1.5 text-sm text-slate-300 hover:bg-slate-800"
        >
          aplicar horário
        </button>
      </div>

      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        maxLength={20}
        spellCheck={false}
        placeholder="*/5 * * * *"
        className={[
          "w-full font-mono",
          inputClass,
          error ? "border-exec-failure" : "",
        ].join(" ")}
      />

      <div className="rounded-md border border-white/10 bg-slate-900 px-3 py-2 text-sm">
        <div className={valid ? "text-slate-200" : "text-exec-failure"}>
          {describeCron(value)}
        </div>
        {runs.length > 0 && (
          <div className="mt-1 font-mono text-xs text-slate-500">
            próximas: {runs.map((d) => formatDateTime(d.toISOString())).join(" · ")}
          </div>
        )}
      </div>

      {error && <p className="text-xs text-exec-failure">{error}</p>}
    </div>
  );
}
