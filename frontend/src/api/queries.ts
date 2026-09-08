import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client.ts";
import type {
  OneTimeTaskCreation,
  PeriodicTaskCreation,
  TaskListFilter,
} from "./types.ts";

export const keys = {
  tasks: (filter?: TaskListFilter) => ["tasks", filter ?? {}] as const,
  task: (id: number) => ["tasks", id] as const,
  executions: (id: number) => ["tasks", id, "executions"] as const,
  history: (id: number) => ["tasks", id, "history"] as const,
  recentExecutions: ["tasks", "executions"] as const,
  health: ["health"] as const,
};

export function useTasks(filter: TaskListFilter = {}) {
  return useQuery({
    queryKey: keys.tasks(filter),
    queryFn: () => api.listTasks(filter),
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

export function useRecentExecutions() {
  return useQuery({
    queryKey: keys.recentExecutions,
    queryFn: () => api.listRecentExecutions(),
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
