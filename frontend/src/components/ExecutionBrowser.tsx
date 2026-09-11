import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import type { EnrichedExecution } from "../lib/executions.ts";
import { groupBySignature } from "../lib/executions.ts";
import { SegmentedControl } from "./SegmentedControl.tsx";
import { ExecutionSignatureGroup } from "./ExecutionSignatureGroup.tsx";
import { Drawer } from "./Drawer.tsx";
import { ExecutionDetail } from "./ExecutionDetail.tsx";

type Outcome = "SUCCESS" | "FAILURE";
type Window = "24h" | "7d" | "30d" | "all";

const WINDOW_MS: Record<Exclude<Window, "all">, number> = {
  "24h": 86_400_000,
  "7d": 7 * 86_400_000,
  "30d": 30 * 86_400_000,
};

const OUTCOME_OPTS = [
  { value: "SUCCESS" as const, label: "Sucesso" },
  { value: "FAILURE" as const, label: "Falha" },
];

const WINDOW_OPTS: { value: Window; label: string }[] = [
  { value: "24h", label: "últimas 24 h" },
  { value: "7d", label: "últimos 7 d" },
  { value: "30d", label: "últimos 30 d" },
  { value: "all", label: "toda a janela" },
];

const inputClass =
  "rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-600";

export function ExecutionBrowser({
  enriched,
  mode,
  lockedSystem,
  systems = [],
}: {
  enriched: EnrichedExecution[];
  mode: "page" | "embedded";
  lockedSystem?: string;
  systems?: string[];
}) {
  const isPage = mode === "page";
  const [params, setParams] = useSearchParams();
  const [localOutcome, setLocalOutcome] = useState<Outcome | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<EnrichedExecution | null>(null);

  const rawStatus = params.get("status");
  const outcome: Outcome | null = isPage
    ? rawStatus === "SUCCESS" || rawStatus === "FAILURE"
      ? rawStatus
      : null
    : localOutcome;
  const system = lockedSystem ?? (isPage ? params.get("system") : null);
  const rawWin = params.get("win");
  const win: Window =
    isPage && (rawWin === "24h" || rawWin === "7d" || rawWin === "30d")
      ? rawWin
      : "all";
  const taskId = isPage ? (params.get("taskId") ?? "") : "";
  const q = isPage ? (params.get("q") ?? "") : "";

  function setParam(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }

  const setOutcome = (v: Outcome | null) =>
    isPage ? setParam("status", v ?? "") : setLocalOutcome(v);

  const anyFilter =
    outcome != null ||
    (isPage && !lockedSystem && !!system) ||
    !!taskId ||
    !!q ||
    win !== "all";

  const groups = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const cutoff = win === "all" ? 0 : Date.now() - WINDOW_MS[win];
    const filtered = enriched.filter((e) => {
      if (outcome && (outcome === "SUCCESS") !== e.success) return false;
      if (system && e.system !== system) return false;
      if (taskId.trim() && String(e.task_id) !== taskId.trim()) return false;
      if (
        needle &&
        !`${e.host ?? ""} ${e.endpoint ?? ""}`.toLowerCase().includes(needle)
      ) {
        return false;
      }
      if (cutoff && new Date(e.executed_at).getTime() < cutoff) return false;
      return true;
    });
    return groupBySignature(filtered);
  }, [enriched, outcome, system, taskId, q, win]);

  const total = groups.reduce((n, g) => n + g.count, 0);

  function toggle(sig: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(sig)) next.delete(sig);
      else next.add(sig);
      return next;
    });
  }

  function clearFilters() {
    const next = new URLSearchParams(params);
    for (const k of ["status", "system", "taskId", "q", "win"]) {
      if (!(k === "system" && lockedSystem)) next.delete(k);
    }
    setParams(next, { replace: true });
    setLocalOutcome(null);
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <SegmentedControl
          options={OUTCOME_OPTS}
          value={outcome}
          onChange={setOutcome}
          allowClear
        />
        {isPage && !lockedSystem && (
          <select
            value={system ?? ""}
            onChange={(e) => setParam("system", e.target.value)}
            className={inputClass}
          >
            <option value="">todos os sistemas</option>
            {systems.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        )}
        {isPage && (
          <>
            <input
              value={taskId}
              onChange={(e) =>
                setParam("taskId", e.target.value.replace(/[^0-9]/g, ""))
              }
              placeholder="task id"
              inputMode="numeric"
              className={`${inputClass} w-24`}
            />
            <input
              value={q}
              onChange={(e) => setParam("q", e.target.value)}
              placeholder="host / endpoint"
              className={`${inputClass} w-44`}
            />
            <select
              value={win}
              onChange={(e) => setParam("win", e.target.value === "all" ? "" : e.target.value)}
              className={inputClass}
            >
              {WINDOW_OPTS.map((w) => (
                <option key={w.value} value={w.value}>
                  {w.label}
                </option>
              ))}
            </select>
          </>
        )}
        {anyFilter && (
          <button
            type="button"
            onClick={clearFilters}
            className="text-xs text-accent-400 hover:underline"
          >
            limpar filtros
          </button>
        )}
      </div>

      <p className="text-xs text-slate-500">
        {total} {total === 1 ? "execução" : "execuções"} · {groups.length}{" "}
        {groups.length === 1 ? "assinatura" : "assinaturas"}
      </p>

      {groups.length === 0 ? (
        <p className="text-sm text-slate-500">
          Nenhuma execução para esse filtro na janela carregada.
        </p>
      ) : (
        <div className="overflow-hidden rounded-xl border border-white/10 bg-slate-900/50">
          {groups.map((g) => (
            <ExecutionSignatureGroup
              key={g.signature}
              group={g}
              expanded={expanded.has(g.signature)}
              onToggle={() => toggle(g.signature)}
              onSelect={setSelected}
              selectedId={selected?.id}
            />
          ))}
        </div>
      )}

      <Drawer
        open={selected != null}
        onClose={() => setSelected(null)}
        title="Detalhe da execução"
      >
        {selected && (
          <ExecutionDetail exec={selected} onRecreated={() => undefined} />
        )}
      </Drawer>
    </div>
  );
}
