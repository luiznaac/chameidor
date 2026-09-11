export interface SegmentOption<T extends string> {
  value: T;
  label: string;
}

export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  allowClear = false,
}: {
  options: SegmentOption<T>[];
  value: T | null;
  onChange: (value: T | null) => void;
  /** When set, clicking the active segment clears the selection. */
  allowClear?: boolean;
}) {
  return (
    <div className="inline-flex rounded-md border border-white/10 bg-slate-900 p-0.5">
      {options.map((opt) => {
        const active = opt.value === value;
        return (
          <button
            key={opt.value}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(active && allowClear ? null : opt.value)}
            className={[
              "rounded px-3 py-1 text-sm font-medium transition-colors",
              active
                ? "bg-slate-700 text-white"
                : "text-slate-400 hover:text-slate-200",
            ].join(" ")}
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
