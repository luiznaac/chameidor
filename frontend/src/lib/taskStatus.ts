import type { TaskStatus } from "../api/types.ts";

interface StatusMeta {
  label: string;
  /** Tailwind classes for a filled pill. */
  pill: string;
  /** CSS colour token, for dots and charts. */
  color: string;
}

export const TASK_STATUS_META: Record<TaskStatus, StatusMeta> = {
  WAITING: {
    label: "Aguardando",
    pill: "bg-slate-500/15 text-slate-300 ring-1 ring-inset ring-slate-500/30",
    color: "var(--color-status-waiting)",
  },
  QUEUED: {
    label: "Na fila",
    pill: "bg-amber-500/15 text-amber-300 ring-1 ring-inset ring-amber-500/30",
    color: "var(--color-status-queued)",
  },
  EXECUTING: {
    label: "Executando",
    pill: "bg-sky-500/15 text-sky-300 ring-1 ring-inset ring-sky-500/30",
    color: "var(--color-status-executing)",
  },
  EXECUTED: {
    label: "Executada",
    pill: "bg-emerald-500/15 text-emerald-300 ring-1 ring-inset ring-emerald-500/30",
    color: "var(--color-status-executed)",
  },
};

export const TASK_STATUSES = Object.keys(TASK_STATUS_META) as TaskStatus[];

export function isExecutionSuccess(status: string): boolean {
  return status.toUpperCase() === "SUCCESS";
}
