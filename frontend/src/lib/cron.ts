import cronstrue from "cronstrue";
import "cronstrue/locales/pt_BR";
import { parseExpression } from "cron-parser";

/**
 * Cron helpers for the task registration form.
 *
 * The API contract is a **5-field** expression (`min hour day month weekday`);
 * the backend prepends `"0 "` to make it a 6-field Spring `CronExpression`.
 * Everything here works on the 5-field string the client sends.
 */

export interface CronPreset {
  label: string;
  expr: string;
}

export const CRON_PRESETS: CronPreset[] = [
  { label: "a cada 5 min", expr: "*/5 * * * *" },
  { label: "a cada 15 min", expr: "*/15 * * * *" },
  { label: "a cada 30 min", expr: "*/30 * * * *" },
  { label: "de hora em hora", expr: "0 * * * *" },
  { label: "a cada 6 horas", expr: "0 */6 * * *" },
  { label: "diário à meia-noite", expr: "0 0 * * *" },
  { label: "diário às 09:00", expr: "0 9 * * *" },
  { label: "semanal seg 09:00", expr: "0 9 * * 1" },
  { label: "1º dia do mês 00:00", expr: "0 0 1 * *" },
];

/** Weekday labels for the helper `<select>`, indexed by cron day-of-week (0 = domingo). */
export const WEEKDAYS = [
  "domingo",
  "segunda",
  "terça",
  "quarta",
  "quinta",
  "sexta",
  "sábado",
] as const;

/** `"09:30"` → `"30 9 * * *"`. */
export function dailyAt(hhmm: string): string {
  const [h, m] = hhmm.split(":");
  return `${Number(m)} ${Number(h)} * * *`;
}

/** `(1, "09:30")` → `"30 9 * * 1"`. */
export function weeklyAt(dow: number, hhmm: string): string {
  const [h, m] = hhmm.split(":");
  return `${Number(m)} ${Number(h)} * * ${dow}`;
}

export function normalizeCron(expr: string): string {
  return expr.trim().replace(/\s+/g, " ");
}

export function isFiveField(expr: string): boolean {
  const e = normalizeCron(expr);
  return e.length > 0 && e.split(" ").length === 5;
}

export function isValidCron(expr: string): boolean {
  const e = normalizeCron(expr);
  if (!isFiveField(e)) return false;
  try {
    parseExpression(e);
    return true;
  } catch {
    return false;
  }
}

export function describeCron(expr: string): string {
  try {
    return cronstrue.toString(normalizeCron(expr), {
      locale: "pt_BR",
      use24HourTimeFormat: true,
    });
  } catch {
    return "expressão inválida";
  }
}

/** Next `n` fire times for a valid expression; `[]` when it can't be parsed. */
export function nextRuns(expr: string, n: number): Date[] {
  try {
    const it = parseExpression(normalizeCron(expr));
    return Array.from({ length: n }, () => it.next().toDate());
  } catch {
    return [];
  }
}
