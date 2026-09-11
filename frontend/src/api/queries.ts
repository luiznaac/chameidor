import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client.ts";
import type {
  OneTimeTaskCreation,
  PeriodicTaskCreation,
  TaskListFilter,
} from "./types.ts";

/** Task window loaded for the Tenant Portal / Execution Explorer joins. */
export const ALL_TASKS_LIMIT = 500;
/** Default execution window for the Execution Explorer. */
export const RECENT_EXECUTIONS_LIMIT = 200;

export const keys = {
  tasks: (filter?: TaskListFilter) => ["tasks", filter ?? {}] as const,
  allTasks: (limit: number) => ["tasks", { limit }] as const,
  task: (id: number) => ["tasks", id] as const,
  executions: (id: number) => ["tasks", id, "executions"] as const,
  history: (id: number) => ["tasks", id, "history"] as const,
  recentExecutions: (limit: number) => ["tasks", "executions", limit] as const,
  health: ["health"] as const,
};

export function useTasks(filter: TaskListFilter = {}) {
  return useQuery({
    queryKey: keys.tasks(filter),
    queryFn: () => api.listTasks(filter),
  });
}

export function useAllTasks(limit = ALL_TASKS_LIMIT) {
  return useQuery({
    queryKey: keys.allTasks(limit),
    queryFn: () => api.listTasks({ limit }),
  });
}

export function useTask(id: number) {
  return useQuery({
    queryKey: keys.task(id),
    queryFn: () => api.getTask(id),
  });
}

export function useExecutions(id: number) {
  return useQuery({
    queryKey: keys.executions(id),
    queryFn: () => api.listExecutions(id),
  });
}

export function useRecentExecutions(limit = RECENT_EXECUTIONS_LIMIT) {
  return useQuery({
    queryKey: keys.recentExecutions(limit),
    queryFn: () => api.listRecentExecutions(limit),
  });
}

export function useHistory(id: number) {
  return useQuery({
    queryKey: keys.history(id),
    queryFn: () => api.listHistory(id),
  });
}

export function useHealth() {
  return useQuery({
    queryKey: keys.health,
    queryFn: () => api.health(),
    refetchInterval: 30_000,
  });
}

type CreateArgs =
  | { kind: "one_time"; body: OneTimeTaskCreation; externalSystem: string }
  | { kind: "periodic"; body: PeriodicTaskCreation; externalSystem: string };

export function useCreateTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (args: CreateArgs) =>
      args.kind === "periodic"
        ? api.createPeriodicTask(args.body, args.externalSystem)
        : api.createOneTimeTask(args.body, args.externalSystem),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tasks"] }),
  });
}
