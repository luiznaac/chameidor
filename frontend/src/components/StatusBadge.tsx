import type { TaskStatus } from "../api/types.ts";
import { TASK_STATUS_META } from "../lib/taskStatus.ts";

export function StatusBadge({ status }: { status: TaskStatus }) {
  const meta = TASK_STATUS_META[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${meta.pill}`}
    >
      {meta.label}
    </span>
  );
}
