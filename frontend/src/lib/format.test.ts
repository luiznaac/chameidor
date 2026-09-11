import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { formatDate, formatDateTime, formatDuration, fromNow } from "./format.ts";

const FIXED_NOW = new Date("2026-09-11T12:00:00.000Z");

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(FIXED_NOW);
});

afterEach(() => {
  vi.useRealTimers();
});

describe("fromNow", () => {
  it("renders the sub-minute bucket as the fixed word", () => {
    expect(fromNow(new Date(FIXED_NOW.getTime() - 30_000).toISOString())).toBe("agora");
    expect(fromNow(new Date(FIXED_NOW.getTime() + 30_000).toISOString())).toBe("agora");
  });

  it("switches to minutes right at the one-minute boundary", () => {
    const justUnder = new Date(FIXED_NOW.getTime() - 59_999).toISOString();
    const justOver = new Date(FIXED_NOW.getTime() - 60_000).toISOString();

    expect(fromNow(justUnder)).toBe("agora");
    expect(fromNow(justOver)).not.toBe("agora");
  });

  it("keeps the past and the future symmetric in magnitude", () => {
    const past = new Date(FIXED_NOW.getTime() - 5 * 3_600_000).toISOString();
    const future = new Date(FIXED_NOW.getTime() + 5 * 3_600_000).toISOString();

    expect(fromNow(past)).toContain("5");
    expect(fromNow(future)).toContain("5");
    expect(fromNow(past)).not.toBe(fromNow(future));
  });

  it("falls back to months beyond the 30-day window", () => {
    const sixtyDaysAgo = new Date(FIXED_NOW.getTime() - 60 * 86_400_000).toISOString();

    expect(fromNow(sixtyDaysAgo)).toContain("2");
  });
});

describe("formatDuration", () => {
  it("renders sub-second durations in whole milliseconds", () => {
    expect(formatDuration(0)).toBe("0 ms");
    expect(formatDuration(999)).toBe("999 ms");
  });

  it("renders sub-minute durations in seconds with two decimals", () => {
    expect(formatDuration(1000)).toBe("1.00 s");
    expect(formatDuration(2500)).toBe("2.50 s");
    expect(formatDuration(59_999)).toBe("60.00 s");
  });

  it("renders minute-scale durations as `Xm Ys`", () => {
    expect(formatDuration(60_000)).toBe("1m 0s");
    expect(formatDuration(90_500)).toBe("1m 31s");
    expect(formatDuration(3_600_000)).toBe("60m 0s");
  });
});

describe("formatDate / formatDateTime", () => {
  it("formats absolute timestamps without throwing on either helper", () => {
    const iso = "2026-09-11T12:00:00.000Z";

    expect(formatDate(iso)).toBeTypeOf("string");
    expect(formatDate(iso).length).toBeGreaterThan(0);
    expect(formatDateTime(iso)).toBeTypeOf("string");
    expect(formatDateTime(iso).length).toBeGreaterThan(0);
  });
});
