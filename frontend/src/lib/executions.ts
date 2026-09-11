import type { TaskExecutionResponse, TaskResponse } from "../api/types.ts";
import { isExecutionSuccess } from "./taskStatus.ts";

/**
 * Executions come back with only `task_id` â no host/endpoint/system. We join
 * each one to the task window loaded elsewhere so the Execution Explorer can
 * group, filter and label them.
 */

export type TaskMap = Map<number, TaskResponse>;

export function buildTaskMap(tasks: TaskResponse[]): TaskMap {
  return new Map(tasks.map((t) => [t.id, t]));
}

export interface EnrichedExecution extends TaskExecutionResponse {
  /** `undefined` when the task fell outside the loaded window. */
  task: TaskResponse | undefined;
  success: boolean;
  host: string | null;
  endpoint: string | null;
  /** `task.created_by` â the X-External-System that owns the task. */
  system: string | null;
  signature: string;
  signatureLabel: string;
}

const SEP = String.fromCharCode(31); // unit separator: collision-free key, never in a host/endpoint

export function enrichExecutions(
  execs: TaskExecutionResponse[],
  map: TaskMap,
): EnrichedExecution[] {
  return execs.map((e) => {
    const task = map.get(e.task_id);
    const success = isExecutionSuccess(e.status);
    const outcome = success ? "OK" : "FAIL";
    const host = task?.host ?? null;
    const endpoint = task?.endpoint ?? null;
    const signature = task
      ? `${host}${SEP}${endpoint}${SEP}${outcome}`
      : `t#${e.task_id}${SEP}?${SEP}${outcome}`;
    const signatureLabel = task
      ? `${host}${endpoint} Â· ${success ? "SUCESSO" : "FALHA"}`
      : `task #${e.task_id} (fora da janela) Â· ${success ? "SUCESSO" : "FALHA"}`;
    return {
      ...e,
      task,
      success,
      host,
      endpoint,
      system: task?.created_by ?? null,
      signature,
      signatureLabel,
    };
  });
}

export interface SignatureGroup {
  signature: string;
  label: string;
  host: string | null;
  endpoint: string | null;
  success: boolean;
  system: string | null;
  count: number;
  /** Newest first. */
  executions: EnrichedExecution[];
  lastAt: string;
  /** 12-bucket histogram of execution times across the filtered set. */
  spark: number[];
}

const SPARK_BUCKETS = 12;

export function groupBySignature(execs: EnrichedExecution[]): SignatureGroup[] {
  if (execs.length === 0) return [];

  const times = execs.map((e) => new Date(e.executed_at).getTime());
  const tMin = Math.min(...times);
  const tMax = Math.max(...times);
  const width = Math.max(1, (tMax - tMin) / SPARK_BUCKETS);

  const byKey = new Map<string, EnrichedExecution[]>();
  for (const e of execs) {
    const list = byKey.get(e.signature);
    if (list) list.push(e);
    else byKey.set(e.signature, [e]);
  }

  const groups: SignatureGroup[] = [];
  for (const [signature, list] of byKey) {
    list.sort(
      (a, b) => new Date(b.executed_at).getTime() - new Date(a.executed_at).getTime(),
    );
    const spark = new Array<number>(SPARK_BUCKETS).fill(0);
    for (const e of list) {
      const i = Math.min(
        SPARK_BUCKETS - 1,
        Math.max(0, Math.floor((new Date(e.executed_at).getTime() - tMin) / width)),
      );
      spark[i] += 1;
    }
    const head = list[0];
    groups.push({
      signature,
      label: head.signatureLabel,
      host: head.host,
      endpoint: head.endpoint,
      success: head.success,
      system: head.system,
      count: list.length,
      executions: list,
      lastAt: head.executed_at,
      spark,
    });
  }

  groups.sort(
    (a, b) =>
      b.count - a.count ||
      new Date(b.lastAt).getTime() - new Date(a.lastAt).getTime(),
  );
  return groups;
}

/** Nearest-rank percentile (`p` in 0..1). `null` for an empty input. */
export function percentile(values: number[], p: number): number | null {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const idx = Math.min(
    sorted.length - 1,
    Math.max(0, Math.ceil(p * sorted.length) - 1),
  );
  return sorted[idx];
}

/** Fraction of successful executions, `null` when there are none. */
export function successRate(execs: { success: boolean }[]): number | null {
  if (execs.length === 0) return null;
  return execs.filter((e) => e.success).length / execs.length;
}
