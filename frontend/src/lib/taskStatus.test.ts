import { describe, expect, it } from "vitest";
import { TASK_STATUSES, TASK_STATUS_META, isExecutionSuccess } from "./taskStatus.ts";

describe("TASK_STATUS_META", () => {
  it("covers every status the backend can emit", () => {
    expect(Object.keys(TASK_STATUS_META).sort()).toEqual([
      "EXECUTED",
      "EXECUTING",
      "QUEUED",
      "WAITING",
    ]);
  });

  it("gives each status a label, a pill class and a colour token", () => {
    for (const meta of Object.values(TASK_STATUS_META)) {
      expect(meta.label).toBeTruthy();
      expect(meta.pill).toContain("ring-inset");
      expect(meta.color).toMatch(/^var\(--color-status-/);
    }
  });
});

describe("TASK_STATUSES", () => {
  it("is derived from the meta map rather than duplicated by hand", () => {
    expect(TASK_STATUSES).toEqual(Object.keys(TASK_STATUS_META));
  });
});

describe("isExecutionSuccess", () => {
  it("accepts SUCCESS in any case", () => {
    expect(isExecutionSuccess("SUCCESS")).toBe(true);
    expect(isExecutionSuccess("success")).toBe(true);
    expect(isExecutionSuccess("Success")).toBe(true);
  });

  it("rejects every other terminal status", () => {
    expect(isExecutionSuccess("FAILED")).toBe(false);
    expect(isExecutionSuccess("ERROR")).toBe(false);
    expect(isExecutionSuccess("")).toBe(false);
  });
});
