import type {
  HealthCheckResult,
  OneTimeTaskCreation,
  PeriodicTaskCreation,
  TaskExecutionResponse,
  TaskHistoryResponse,
  TaskListFilter,
  TaskResponse,
} from "./types.ts";

const BASE = (import.meta.env.VITE_API_BASE ?? "/api").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message ?? body.detail ?? detail;
    } catch {
      /* non-JSON body (Ktor's default error pages) */
    }
    throw new ApiError(res.status, detail);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

function query(filter: TaskListFilter): string {
  const p = new URLSearchParams();
  if (filter.status) p.set("status", filter.status);
  if (filter.created_by) p.set("created_by", filter.created_by);
  if (filter.limit != null) p.set("limit", String(filter.limit));
  if (filter.offset != null) p.set("offset", String(filter.offset));
  const s = p.toString();
  return s ? `?${s}` : "";
}

export const api = {
  // --- health ---
  health(): Promise<HealthCheckResult[]> {
    return request(`/health`);
  },

  // --- tasks (read) ---
  listTasks(filter: TaskListFilter = {}): Promise<TaskResponse[]> {
    return request(`/tasks${query(filter)}`);
  },
  getTask(id: number): Promise<TaskResponse> {
    return request(`/tasks/${id}`);
  },
  listExecutions(id: number, limit = 50): Promise<TaskExecutionResponse[]> {
    return request(`/tasks/${id}/executions?limit=${limit}`);
  },
  listRecentExecutions(limit = 200): Promise<TaskExecutionResponse[]> {
    return request(`/tasks/executions?limit=${limit}`);
  },
  listHistory(id: number): Promise<TaskHistoryResponse[]> {
    return request(`/tasks/${id}/history`);
  },

  // --- tasks (write) ---
  createOneTimeTask(body: OneTimeTaskCreation, externalSystem: string): Promise<TaskResponse> {
    return request(`/tasks/one-time`, {
      method: "POST",
      headers: { "X-External-System": externalSystem },
      body: JSON.stringify(body),
    });
  },
  createPeriodicTask(body: PeriodicTaskCreation, externalSystem: string): Promise<TaskResponse> {
    return request(`/tasks/periodic`, {
      method: "POST",
      headers: { "X-External-System": externalSystem },
      body: JSON.stringify(body),
    });
  },
};
