import { Fragment } from "react";
import { Link } from "react-router-dom";

export interface Crumb {
  label: string;
  to?: string;
}

export function Breadcrumb({ items }: { items: Crumb[] }) {
  return (
    <nav className="mb-2 text-sm text-slate-500">
      {items.map((c, i) => (
        <Fragment key={i}>
          {i > 0 && <span className="px-1.5 text-slate-600">/</span>}
          {c.to ? (
            <Link to={c.to} className="text-accent-400 hover:underline">
              {c.label}
            </Link>
          ) : (
            <span className="text-slate-300">{c.label}</span>
          )}
        </Fragment>
      ))}
    </nav>
  );
}
