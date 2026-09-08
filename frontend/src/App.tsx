import type { RouteObject } from "react-router-dom";
import { Layout } from "./components/Layout.tsx";
import { Dashboard } from "./pages/Dashboard.tsx";
import { Tasks } from "./pages/Tasks.tsx";
import { ComingSoon } from "./pages/ComingSoon.tsx";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: "tasks", element: <Tasks /> },
      // Second pass: full task page + registration form.
      { path: "tasks/new", element: <ComingSoon title="Nova task" /> },
      { path: "tasks/:id", element: <ComingSoon title="Detalhe da task" /> },
    ],
  },
];
