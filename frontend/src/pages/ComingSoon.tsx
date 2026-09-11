import { Link } from "react-router-dom";

export function ComingSoon({ title }: { title: string }) {
  return (
    <div className="rounded-xl border border-white/10 bg-slate-900/50 p-10 text-center">
      <h1 className="text-lg font-semibold text-slate-200">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">
        Em breve — esta tela entra na próxima leva do frontend.
      </p>
      <Link to="/tasks" className="mt-4 inline-block text-sm text-accent-400 hover:underline">
        ← Voltar para as tasks
      </Link>
    </div>
  );
}
