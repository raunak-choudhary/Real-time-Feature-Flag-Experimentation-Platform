import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RexClient } from "../src/RexClient.js";
import type { ConnectionState, EvaluationResult, RexClientOptions } from "../src/types.js";

const bootstrap: EvaluationResult[] = [
  { flagName: "dark_mode", enabled: true, reason: "ROLLOUT_INCLUDED", bucket: 10 },
  { flagName: "checkout_v2", enabled: false, reason: "ROLLOUT_EXCLUDED", bucket: 9500 },
];

// stompjs opens a real socket on activate, which a unit test has no business doing.
vi.mock("@stomp/stompjs", () => ({
  Client: class {
    activate(): void {
      /* no transport in unit tests */
    }
    async deactivate(): Promise<void> {
      /* no transport in unit tests */
    }
    subscribe(): void {
      /* no transport in unit tests */
    }
  },
}));

function options(overrides: Partial<RexClientOptions> = {}): RexClientOptions {
  return {
    apiUrl: "http://api.test",
    wsUrl: "ws://api.test",
    environment: "production",
    userId: "user_1",
    ...overrides,
  };
}

describe("RexClient", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(bootstrap) })),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("serves bootstrapped decisions synchronously after start", async () => {
    const client = new RexClient(options());
    await client.start();

    expect(client.isEnabled("dark_mode")).toBe(true);
    expect(client.isEnabled("checkout_v2")).toBe(false);
  });

  it("returns the caller default for an unknown flag rather than throwing", async () => {
    const client = new RexClient(options());
    await client.start();

    expect(client.isEnabled("never_created")).toBe(false);
    expect(client.isEnabled("never_created", true)).toBe(true);
  });

  it("evaluates without a network call once bootstrapped", async () => {
    const client = new RexClient(options());
    await client.start();
    const callsAfterStart = vi.mocked(fetch).mock.calls.length;

    for (let i = 0; i < 50; i++) {
      client.isEnabled("dark_mode");
    }

    expect(vi.mocked(fetch).mock.calls.length).toBe(callsAfterStart);
  });

  it("requests the configured user and environment, not hardcoded values", async () => {
    const client = new RexClient(options({ userId: "user_42", environment: "staging" }));
    await client.start();

    const firstArgument = vi.mocked(fetch).mock.calls[0]?.[0];
    let requested = "";
    if (firstArgument instanceof URL) {
      requested = firstArgument.href;
    } else if (typeof firstArgument === "string") {
      requested = firstArgument;
    } else if (firstArgument) {
      requested = firstArgument.url;
    }
    expect(requested).toContain("userId=user_42");
    expect(requested).toContain("environment=staging");
  });

  it("notifies on change so a UI can re-render without polling", async () => {
    const onChange = vi.fn();
    const client = new RexClient(options({ onChange }));
    await client.start();

    expect(onChange).toHaveBeenCalledWith(
      expect.arrayContaining([expect.objectContaining({ flagName: "dark_mode" })]),
    );
  });

  it("reports connection state transitions", async () => {
    const states: ConnectionState[] = [];
    const client = new RexClient(options({ onConnectionStateChange: (s) => states.push(s) }));
    await client.start();

    expect(states).toContain("connecting");
  });

  it("surfaces a failed bootstrap rather than silently serving an empty cache", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve({ ok: false, status: 503 })));
    const client = new RexClient(options());

    await expect(client.start()).rejects.toThrow(/Failed to load flags: 503/);
  });

  it("keeps serving its last known values after being stopped", async () => {
    const client = new RexClient(options());
    await client.start();
    await client.stop();

    expect(client.isEnabled("dark_mode")).toBe(true);
    expect(client.connectionState).toBe("closed");
  });

  it("exposes the reason so a surprising decision can be explained", async () => {
    const client = new RexClient(options());
    await client.start();

    expect(client.reasonFor("checkout_v2")).toBe("ROLLOUT_EXCLUDED");
  });
});
