import { describe, expect, it } from "vitest";
import { isFlagOn } from "../src/evaluation.js";
import type { EvaluationResult } from "../src/types.js";

const results: readonly EvaluationResult[] = [
  { flagName: "dark_mode", enabled: true, reason: "ROLLOUT_INCLUDED", bucket: 1234 },
  { flagName: "new_checkout_flow", enabled: false, reason: "FLAG_DISABLED", bucket: null },
];

describe("isFlagOn", () => {
  it("returns the decision for a known enabled flag", () => {
    expect(isFlagOn(results, "dark_mode")).toBe(true);
  });

  it("returns the decision for a known disabled flag", () => {
    expect(isFlagOn(results, "new_checkout_flow")).toBe(false);
  });

  it("falls back to the caller default for an unknown flag", () => {
    expect(isFlagOn(results, "does_not_exist")).toBe(false);
    expect(isFlagOn(results, "does_not_exist", true)).toBe(true);
  });

  it("does not throw on an empty result set", () => {
    expect(isFlagOn([], "anything", true)).toBe(true);
  });
});
