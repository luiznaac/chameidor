import type { RouteObject } from "react-router-dom";
import { Layout } from "./components/Layout.tsx";
import { Dashboard } from "./pages/Dashboard.tsx";
import { Tasks } from "./pages/Tasks.tsx";
import { Systems } from "./pages/Systems.tsx";
import { SystemDetail } from "./pages/SystemDetail.tsx";
import { Executions } from "./pages/Executions.tsx";
import { TaskNew } from "./pages/TaskNew.tsx";
import { TaskDetail } from "./pages/TaskDetail.tsx";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: "systems", element: <Systems /> },
      { path: "systems/:name", element: <SystemDetail /> },
      { path: "executions", element: <Executions /> },
      { path: "tasks", element: <Tasks /> },
      { path: "tasks/new", element: <TaskNew /> },
      { path: "tasks/:id", element: <TaskDetail /> },
    ],
  },
];
