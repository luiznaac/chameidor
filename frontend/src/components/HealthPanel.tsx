import { useHealth } from "../api/queries.ts";
import { fromNow } from "../lib/format.ts";
import { Panel } from "./Panel.tsx";

export function HealthPanel() {
  const { data, isLoading, error } = useHealth();

  return (
    <Panel title="Saúde">
      {isLoading && <p className="text-sm text-slate-500">Carregando…</p>}
      {error && <p className="text-sm text-exec-failure">Sem resposta do backend.</p>}
      {data && (
        <ul className="space-y-2">
          {data.map((check) => (
            <li key={check.service_name} className="flex items-center gap-3 text-sm">
              <span
                className="inline-block h-2.5 w-2.5 shrink-0 rounded-full"
                style={{
                  backgroundColor: check.is_healthy
                    ? "var(--color-exec-success)"
                    : "var(--color-exec-failure)",
                }}
              />
              <span className="font-medium text-slate-200">{check.service_name}</span>
              <span className="ml-auto text-slate-500">{fromNow(check.timestamp)}</span>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
