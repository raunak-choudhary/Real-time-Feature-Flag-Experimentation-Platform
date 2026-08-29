"use client";

import { useMemo } from "react";
import { useRexClient } from "../hooks/useRexClient";
import type { FlagRow } from "../lib/types";
import { FlagTable } from "./FlagTable";
import { LiveIndicator } from "./LiveIndicator";

/**
 * The live half of the dashboard.
 *
 * Receives the server rendered rows as a starting point, then overlays decisions pushed over the
 * socket. The first paint therefore carries real data and every later change arrives without a
 * reload or a poll.
 */
export function LiveFlagPanel({ initialFlags }: { initialFlags: readonly FlagRow[] }) {
  const { flags, connection } = useRexClient("dashboard-operator");

  const liveState = useMemo(
    () => new Map(flags.map((result) => [result.flagName, result.enabled] as const)),
    [flags],
  );

  return (
    <section className="panel">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h2>Feature flags</h2>
        <LiveIndicator state={connection} />
      </div>
      <FlagTable flags={initialFlags} liveState={liveState} />
    </section>
  );
}
