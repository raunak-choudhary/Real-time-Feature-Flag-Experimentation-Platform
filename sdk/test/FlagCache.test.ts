import { describe, expect, it } from "vitest";
import { FlagCache } from "../src/FlagCache.js";
import type { EvaluationResult, FlagChangedEvent } from "../src/types.js";

const darkMode: EvaluationResult = {
  flagName: "dark_mode",
  enabled: true,
  reason: "ROLLOUT_INCLUDED",
  bucket: 100,
};
const checkoutV2: EvaluationResult = {
  flagName: "checkout_v2",
  enabled: false,
  reason: "ROLLOUT_EXCLUDED",
  bucket: 9000,
};
const seeded: readonly EvaluationResult[] = [darkMode, checkoutV2];

function changeEvent(overrides: Partial<FlagChangedEvent>): FlagChangedEvent {
  return {
    flagId: 1,
    flagName: "dark_mode",
    environment: "production",
    enabled: false,
    rolloutPercentage: 0,
    changeType: "TOGGLED",
    occurredAt: new Date(0).toISOString(),
    ...overrides,
  };
}

describe("FlagCache", () => {
  it("serves the bootstrapped decision", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);

    expect(cache.isEnabled("dark_mode")).toBe(true);
    expect(cache.isEnabled("checkout_v2")).toBe(false);
  });

  it("returns the caller default for a flag it has never seen", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);

    expect(cache.isEnabled("unknown_flag")).toBe(false);
    expect(cache.isEnabled("unknown_flag", true)).toBe(true);
  });

  it("applies a kill switch locally, since the decision is unambiguous", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);

    const applied = cache.applyChange(changeEvent({ enabled: false }));

    expect(applied).toBe(true);
    expect(cache.isEnabled("dark_mode")).toBe(false);
    expect(cache.reasonFor("dark_mode")).toBe("FLAG_DISABLED");
  });

  it("refuses to apply a flag turning on, because inclusion depends on the user bucket", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);

    const applied = cache.applyChange(changeEvent({ flagName: "checkout_v2", enabled: true }));

    expect(applied).toBe(false);
    expect(cache.isEnabled("checkout_v2")).toBe(false);
  });

  it("replaces rather than merges, so a removed flag does not linger", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);
    cache.replaceAll([darkMode]);

    expect(cache.size).toBe(1);
    expect(cache.isEnabled("checkout_v2", true)).toBe(true);
  });

  it("reports the reason so a surprising decision can be explained", () => {
    const cache = new FlagCache();
    cache.replaceAll(seeded);

    expect(cache.reasonFor("checkout_v2")).toBe("ROLLOUT_EXCLUDED");
    expect(cache.reasonFor("unknown_flag")).toBeUndefined();
  });

  it("starts empty and serves defaults before any bootstrap", () => {
    const cache = new FlagCache();

    expect(cache.size).toBe(0);
    expect(cache.isEnabled("anything", true)).toBe(true);
  });
});
