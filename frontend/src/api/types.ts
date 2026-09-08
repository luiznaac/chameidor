// Mirrors chameidor's http-api edge DTOs
// (backend/http-api/.../controller/TaskResponse.kt) plus the request bodies
// accepted by TaskController and the HealthCheckResult it serialises.
// Keep both sides in sync in the same commit.

export type TaskStatus = "WAITING" | "QUEUED" | "EXECUTING" | "EXECUTED";
export type TaskType = "one_time" | "periodic";

export interface TaskResponse {
  id: number;
  type: TaskType;
  host: string;
  endpoint: string;
  data: unknown | null;
  cron: string | null; // present iff type === "periodic"
  status: TaskStatus;
  created_by: string;
  created_at: string; // ISO
  executed_at: string | null;
  next_execution_at: string | null;
}

export interface TaskExecutionResponse {
  id: number;
  task_id: number;
  executed_at: string;
  duration_ms: number;
  status: string; // "SUCCESS" | "FAILURE"
  result: unknown | null;
}

export interface TaskHistoryResponse {
  status: string;
  executed_at: string | null;
  next_execution_at: string | null;
  created_at: string;
}

// One entry of GET /health — the JSON shape of usecase/health/HealthCheckResult.
export interface HealthCheckResult {
  service_name: string;
  is_healthy: boolean;
  timestamp: string;
}

// --- request bodies (POST /tasks/one-time, POST /tasks/periodic) ---

export interface OneTimeTaskCreation {
  host: string;
  endpoint: string;
  data?: unknown;
}

export interface PeriodicTaskCreation extends OneTimeTaskCreation {
  cron: string; // 5-field cron expression, e.g. "*/5 * * * *"
}

export interface TaskListFilter {
  status?: TaskStatus;
  created_by?: string;
  limit?: number;
  offset?: number;
}
