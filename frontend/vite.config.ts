import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// The app is served under `/chameidor/` behind the unified dashboard reverse
// proxy, but runs at `/` in local dev. Override with VITE_BASE if needed.
export default defineConfig(({ mode }) => {
  // Vite does not inject `.env` into `process.env`; loadEnv is what makes the
  // committed fixtures in .env.example/.env.development actually take effect.
  const env = loadEnv(mode, process.cwd(), "");
  return {
    base: env.VITE_BASE ?? (mode === "production" ? "/chameidor/" : "/"),
    plugins: [react(), tailwindcss()],
    server: {
      // Project block role `frontend-dev`; strictPort makes a taken port fail
      // loudly instead of drifting off the block.
      port: Number(env.FRONTEND_DEV_PORT ?? 8103),
      strictPort: true,
      proxy: {
        // Dev-only: forward API calls to the chameidor backend to dodge CORS.
        "/api": {
          target: env.VITE_API_TARGET ?? "http://localhost:8100",
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, ""),
        },
      },
    },
  };
});
