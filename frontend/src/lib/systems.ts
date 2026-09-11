import type { TaskResponse, TaskStatus } from "../api/types.ts";
import { TASK_STATUSES } from "./taskStatus.ts";
import { percentile, successRate, type EnrichedExecution } from "./executions.ts";

/**
 * Per-system (X-External-System / `created_by`) aggregation, all derived
 * client-side from the loaded task + execution windows.
 */

const SMALL_SAMPLE = 20;
const DAY_MS = 86_400_000;

/** Distinct `created_by` values, exact match (mirrors the backend `= ?`). */
export function distinctSystems(tasks: TaskResponse[]): string[] {
  return [...new Set(tasks.map((t) => t.created_by))].sort((a, b) =>
    a.localeCompare(b, "pt-BR"),
  );
}

function emptyStatusMix(): Record<TaskStatus, number> {
  return { WAITING: 0, QUEUED: 0, EXECUTING: 0, EXECUTED: 0 };
}

export interface SystemSummary {
  name: string;
  taskCount: number;
  periodic: number;
  oneTime: number;
  statusMix: Record<TaskStatus, number>;
  execCount: number;
  successRate: number | null;
  lastFailureAt: string | null;
}

export function summarizeSystems(
  tasks: TaskResponse[],
  execs: EnrichedExecution[],
): SystemSummary[] {
  const byName = new Map<string, SystemSummary>();
  const ensure = (name: string): SystemSummary => {
    let s = byName.get(name);
    if (!s) {
      s = {
        name,
        taskCount: 0,
        periodic: 0,
        oneTime: 0,
        statusMix: emptyStatusMix(),
        execCount: 0,
        successRate: null,
        lastFailureAt: null,
      };
      byName.set(name, s);
    }
    return s;
  };

  for (const t of tasks) {
    const s = ensure(t.created_by);
    s.taskCount += 1;
    if (t.type === "periodic") s.periodic += 1;
    else s.oneTime += 1;
    if (TASK_STATUSES.includes(t.status)) s.statusMix[t.status] += 1;
  }

  const execsBySystem = new Map<string, EnrichedExecution[]>();
  for (const e of execs) {
    if (e.system == null) continue;
    const list = execsBySystem.get(e.system);
    if (list) list.push(e);
    else execsBySystem.set(e.system, [e]);
  }

  for (const [name, list] of execsBySystem) {
    const s = ensure(name);
    s.execCount = list.length;
    s.successRate = successRate(list);
    for (const e of list) {
      if (
        !e.success &&
        (s.lastFailureAt == null ||
          new Date(e.executed_at).getTime() > new Date(s.lastFailureAt).getTime())
      ) {
        s.lastFailureAt = e.executed_at;
      }
    }
  }

  return [...byName.values()].sort(
    (a, b) => b.taskCount - a.taskCount || a.name.localeCompare(b.name, "pt-BR"),
  );
}

export interface SystemDetailStats {
  taskCount: number;
  successRate24h: number | null;
  execCount: number;
  failedCount: number;
  p95Ms: number | null;
  smallSample: boolean;
}

export function systemDetailStats(
  tasks: TaskResponse[],
  execs: EnrichedExecution[],
): SystemDetailStats {
  const cutoff = Date.now() - DAY_MS;
  const last24h = execs.filter(
    (e) => new Date(e.executed_at).getTime() >= cutoff,
  );
  return {
    taskCount: tasks.length,
    successRate24h: successRate(last24h),
    execCount: execs.length,
    failedCount: execs.filter((e) => !e.success).length,
    p95Ms: percentile(
      execs.map((e) => e.duration_ms),
      0.95,
    ),
    smallSample: execs.length < SMALL_SAMPLE,
  };
}
