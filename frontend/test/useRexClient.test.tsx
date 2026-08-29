import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useRexClient } from "../hooks/useRexClient";

vi.mock("@stomp/stompjs", () => ({
  Client: class {
    activate(): void {
      /* no transport in a unit test */
    }
    async deactivate(): Promise<void> {
      /* no transport in a unit test */
    }
    subscribe(): void {
      /* no transport in a unit test */
    }
  },
}));

function Probe() {
  const { flags, connection } = useRexClient("test-operator");
  return (
    <div>
      <span data-testid="connection">{connection}</span>
      <span data-testid="count">{flags.length}</span>
    </div>
  );
}

describe("useRexClient", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              { flagName: "dark_mode", enabled: true, reason: "ROLLOUT_INCLUDED", bucket: 10 },
            ]),
        }),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("exposes flags once the client has bootstrapped", async () => {
    render(<Probe />);

    await waitFor(() => {
      expect(screen.getByTestId("count")).toHaveTextContent("1");
    });
  });

  it("reports the connection state so the indicator can render it", async () => {
    render(<Probe />);

    await waitFor(() => {
      expect(screen.getByTestId("connection")).not.toHaveTextContent("closed");
    });
  });

  it("a failed bootstrap leaves the component rendered rather than crashing the page", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve({ ok: false, status: 503 })));

    render(<Probe />);

    await waitFor(() => {
      expect(screen.getByTestId("count")).toHaveTextContent("0");
    });
  });
});
