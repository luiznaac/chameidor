import { useEffect, useRef, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ApiError } from "../api/client.ts";
import { useAllTasks, useCreateTask } from "../api/queries.ts";
import type { TaskResponse, TaskType } from "../api/types.ts";
import { distinctSystems } from "../lib/systems.ts";
import { isValidCron, normalizeCron } from "../lib/cron.ts";
import { Breadcrumb } from "../components/Breadcrumb.tsx";
import { CronField } from "../components/CronField.tsx";
import { SegmentedControl } from "../components/SegmentedControl.tsx";
import { SystemsDatalist } from "../components/SystemsDatalist.tsx";
import { TaskTemplatePicker } from "../components/TaskTemplatePicker.tsx";

const inputClass =
  "w-full rounded-md border border-white/10 bg-slate-900 px-2 py-1.5 text-sm text-slate-200 placeholder:text-slate-600";

const TYPE_OPTS = [
  { value: "one_time" as const, label: "Única" },
  { value: "periodic" as const, label: "Periódica" },
];

interface FieldErrors {
  host?: string;
  endpoint?: string;
  system?: string;
  data?: string;
  cron?: string;
}

export function TaskNew() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const create = useCreateTask();
  const tasksQ = useAllTasks();

  const [type, setType] = useState<TaskType>("periodic");
  const [host, setHost] = useState("");
  const [endpoint, setEndpoint] = useState("");
  const [system, setSystem] = useState(params.get("system") ?? "");
  const [dataText, setDataText] = useState("");
  const [cron, setCron] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  const fromId = params.get("from");
  const appliedFrom = useRef(false);
  const [fromMissing, setFromMissing] = useState(false);

  function applyTemplate(t: TaskResponse) {
    setType(t.type);
    setHost(t.host);
    setEndpoint(t.endpoint);
    setSystem(t.created_by);
    setDataText(t.data == null ? "" : JSON.stringify(t.data, null, 2));
    setCron(t.cron ?? "");
    setErrors({});
  }

  useEffect(() => {
    if (!fromId || appliedFrom.current || !tasksQ.data) return;
    appliedFrom.current = true;
    const t = tasksQ.data.find((x) => x.id === Number(fromId));
    if (t) applyTemplate(t);
    else setFromMissing(true);
  }, [fromId, tasksQ.data]);

  function validate(): { errors: FieldErrors; parsedData: unknown } {
    const e: FieldErrors = {};
    const h = host.trim();
    const ep = endpoint.trim();
    const sys = system.trim();
    if (!h) e.host = "obrigatório";
    else if (h.length > 50) e.host = "máx. 50 caracteres";
    if (!ep) e.endpoint = "obrigatório";
    else if (ep.length > 255) e.endpoint = "máx. 255 caracteres";
    if (!sys) e.system = "obrigatório";
    else if (sys.length > 50) e.system = "máx. 50 caracteres";

    let parsedData: unknown;
    if (dataText.trim()) {
      try {
        parsedData = JSON.parse(dataText);
      } catch {
        e.data = "JSON inválido";
      }
    }

    if (type === "periodic") {
      const c = normalizeCron(cron);
      if (!c) e.cron = "obrigatório";
      else if (c.length > 20) e.cron = "máx. 20 caracteres";
      else if (!isValidCron(c)) e.cron = "expressão cron inválida";
    }

    return { errors: e, parsedData };
  }

  function submit(ev: FormEvent) {
    ev.preventDefault();
    setSubmitError(null);
    const { errors: e, parsedData } = validate();
    setErrors(e);
    if (Object.keys(e).length > 0) return;

    const base = { host: host.trim(), endpoint: endpoint.trim() };
    const body =
      parsedData !== undefined ? { ...base, data: parsedData } : base;
    const externalSystem = system.trim();

    const args =
      type === "periodic"
        ? {
            kind: "periodic" as const,
            body: { ...body, cron: normalizeCron(cron) },
            externalSystem,
          }
        : { kind: "one_time" as const, body, externalSystem };

    create.mutate(args, {
      onSuccess: () =>
        navigate(`/systems/${encodeURIComponent(externalSystem)}`),
      onError: (err) =>
        setSubmitError(err instanceof ApiError ? err.message : String(err)),
    });
  }

  const systems = distinctSystems(tasksQ.data ?? []);

  return (
    <div className="space-y-4">
      <Breadcrumb
        items={[{ label: "Tasks", to: "/tasks" }, { label: "Nova task" }]}
      />
      <h1 className="text-lg font-semibold text-slate-100">Nova task</h1>

      <form
        onSubmit={submit}
        className="max-w-xl space-y-4 rounded-xl border border-white/10 bg-slate-900/50 p-5"
      >
        <TaskTemplatePicker
          tasks={tasksQ.data ?? []}
          onPick={applyTemplate}
        />
        {fromMissing && (
          <p className="text-xs text-slate-500">
            task #{fromId} fora da janela carregada — preencha manualmente.
          </p>
        )}

        <Field label="Tipo">
          <SegmentedControl
            options={TYPE_OPTS}
            value={type}
            onChange={(v) => v && setType(v)}
          />
        </Field>

        <Field label="Host" error={errors.host}>
          <input
            value={host}
            onChange={(e) => setHost(e.target.value)}
            maxLength={50}
            placeholder="billing.internal"
            className={inputClass}
          />
        </Field>

        <Field label="Endpoint" error={errors.endpoint}>
          <input
            value={endpoint}
            onChange={(e) => setEndpoint(e.target.value)}
            maxLength={255}
            placeholder="/invoices/close"
            className={inputClass}
          />
        </Field>

        <Field
          label="X-External-System"
          error={errors.system}
          hint="sugestões: sistemas existentes · pode digitar um novo"
        >
          <input
            value={system}
            onChange={(e) => setSystem(e.target.value)}
            list="known-systems"
            maxLength={50}
            placeholder="billing-svc"
            className={inputClass}
          />
          <SystemsDatalist id="known-systems" systems={systems} />
        </Field>

        <Field label="Payload (opcional, JSON)" error={errors.data}>
          <textarea
            value={dataText}
            onChange={(e) => setDataText(e.target.value)}
            rows={4}
            placeholder='{ "dry_run": false }'
            className={`${inputClass} font-mono`}
          />
        </Field>

        {type === "periodic" && (
          <Field label="Agenda (cron)">
            <CronField value={cron} onChange={setCron} error={errors.cron} />
          </Field>
        )}

        {submitError && (
          <div className="rounded-md border border-exec-failure/30 bg-exec-failure/10 p-3 text-sm text-exec-failure">
            {submitError}
            <p className="mt-1 text-xs text-slate-400">
              O backend responde 500 para header ausente, cron inválido ou campos
              longos — revise acima.
            </p>
          </div>
        )}

        <div className="flex items-center gap-2 pt-1">
          <button
            type="submit"
            disabled={create.isPending}
            className="rounded-md bg-accent-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-accent-500 disabled:opacity-50"
          >
            {create.isPending ? "Registrando…" : "Registrar"}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-md border border-white/10 px-3 py-1.5 text-sm text-slate-300 hover:bg-slate-800"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}

function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label className="block text-xs uppercase tracking-wide text-slate-500">
        {label}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-slate-500">{hint}</p>}
      {error && <p className="text-xs text-exec-failure">{error}</p>}
    </div>
  );
}
