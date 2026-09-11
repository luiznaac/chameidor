/** Tiny inline bar chart for a signature group's activity over the window. */
export function Sparkline({
  data,
  className,
}: {
  data: number[];
  className?: string;
}) {
  if (data.length < 2) return null;
  const max = Math.max(...data, 1);
  const gap = 1.5;
  const bw = (100 - gap * (data.length - 1)) / data.length;

  return (
    <svg
      viewBox="0 0 100 24"
      preserveAspectRatio="none"
      className={className ?? "h-4 w-20"}
      aria-hidden="true"
    >
      {data.map((v, i) => {
        const h = (v / max) * 22;
        return (
          <rect
            key={i}
            x={i * (bw + gap)}
            y={24 - h}
            width={bw}
            height={h || 0.5}
            fill="var(--color-accent-400)"
            opacity={v === 0 ? 0.25 : 0.9}
          />
        );
      })}
    </svg>
  );
}
