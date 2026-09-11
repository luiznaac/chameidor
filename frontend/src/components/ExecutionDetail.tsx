import { useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../api/client.ts";
import { useCreateTask } from "../api/queries.ts";
import type { EnrichedExecution } from "../lib/executions.ts";
import { formatDateTime, formatDuration } from "../lib/format.ts";
import { ConfirmDialog } from "./ConfirmDialog.tsx";
import { JsonPanel } from "./JsonPanel.tsx";

export function ExecutionDetail({
  exec,
  onRecreated,
}: {
  exec: EnrichedExecution;
  onRecreated?: () => void;
}) {
  const create = useCreateTask();
  const [confirming, setConfirming] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canRecreate = exec.task != null && exec.host != null && exec.endpoint != null;

  function recreate() {
    if (!canRecreate || !exec.task) return;
    setError(null);
    create.mutate(
      {
        kind: "one_time",
        body: {
          host: exec.host!,
          endpoint: exec.endpoint!,
          data: exec.task.data ?? undefined,
        },
        externalSystem: exec.system!,
      },
      {
        onSuccess: () => {
          setConfirming(false);
          setDone(true);
          onRecreated?.();
        },
        onError: (e) => {
          setConfirming(false);
          setError(e instanceof ApiError ? e.message : String(e));
        },
      },
    );
  }

  return (
    <div className="space-y-4 text-sm">
      <div>
        <div className="font-mono text-xs text-slate-500">
          execução #{exec.id} · task #{exec.task_id}
        </div>
        <div className="mt-1 text-slate-300">
          {formatDateTime(exec.executed_at)}
        </div>
      </div>

      <dl className="divide-y divide-white/5 rounded-md border border-white/10">
        <Row label="Sistema">
          {exec.system ? (
            <Link
              to={`/systems/${encodeURIComponent(exec.system)}`}
              className="text-accent-400 hover:underline"
            >
              {exec.system}
            </Link>
          ) : (
            <span className="text-slate-500">—</span>
          )}
        </Row>
        <Row label="Alvo">
          {exec.host ? (
            <span className="font-mono text-xs text-slate-300">
              {exec.host}
              {exec.endpoint}
            </span>
          ) : (
            <span className="text-slate-500">task fora da janela carregada</span>
          )}
        </Row>
        <Row label="Duração">
          <span className="tabular-nums">{formatDuration(exec.duration_ms)}</span>
        </Row>
        <Row label="Resultado">
          <span className={exec.success ? "text-exec-success" : "text-exec-failure"}>
            {exec.success ? "SUCESSO" : "FALHA"}
          </span>
        </Row>
      </dl>

      <div className="grid gap-3 sm:grid-cols-2">
        <JsonPanel label="Requisição (data)" value={exec.task?.data} />
        <JsonPanel label="Resultado" value={exec.result} />
      </div>

      {done ? (
        <p className="text-sm text-exec-success">
          Nova task one-time registrada.
        </p>
      ) : (
        <button
          type="button"
          disabled={!canRecreate}
          onClick={() => setConfirming(true)}
          title={
            canRecreate ? undefined : "A task original não está na janela carregada"
          }
          className="w-full rounded-md bg-accent-600 px-3 py-2 text-sm font-medium text-white hover:bg-accent-500 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Recriar como one-time
        </button>
      )}
      {error && <p className="text-sm text-exec-failure">{error}</p>}

      <ConfirmDialog
        open={confirming}
        title="Recriar como one-time"
        body={
          <>
            Cria uma task <strong>única</strong> chamando{" "}
            <span className="font-mono text-xs">
              {exec.host}
              {exec.endpoint}
            </span>{" "}
            com o mesmo payload, como <strong>{exec.system}</strong>.
          </>
        }
        confirmLabel="Recriar"
        pending={create.isPending}
        onConfirm={recreate}
        onCancel={() => setConfirming(false)}
      />
    </div>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-3 px-3 py-2">
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="text-right">{children}</dd>
    </div>
  );
}
