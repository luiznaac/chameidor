function render(value: unknown): string | null {
  if (value == null) return null;
  try {
    if (typeof value === "string") return value;
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

export function JsonPanel({ label, value }: { label: string; value: unknown }) {
  const text = render(value);
  return (
    <div>
      <div className="mb-1.5 text-xs uppercase tracking-wide text-slate-500">
        {label}
      </div>
      {text == null ? (
        <p className="text-sm text-slate-500">— sem payload</p>
      ) : (
        <pre className="overflow-x-auto rounded-md border border-white/10 bg-slate-950 p-3 text-xs leading-relaxed text-slate-300">
          {text}
        </pre>
      )}
    </div>
  );
}
