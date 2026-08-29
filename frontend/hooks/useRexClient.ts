"use client";

import { RexClient, type ConnectionState, type EvaluationResult } from "@rex/sdk";
import { useEffect, useMemo, useState } from "react";
import { config } from "../lib/config";

/**
 * React binding for the SDK.
 *
 * This hook is the only React aware code in the chain. The SDK itself has no React dependency, so
 * it stays usable from a plain page, a Node service, or any other framework. Letting React leak
 * into it would turn an SDK into a React library.
 */
export function useRexClient(userId: string): {
  flags: readonly EvaluationResult[];
  connection: ConnectionState;
} {
  const [flags, setFlags] = useState<readonly EvaluationResult[]>([]);
  const [connection, setConnection] = useState<ConnectionState>("closed");

  const client = useMemo(
    () =>
      new RexClient({
        apiUrl: config.apiUrl,
        wsUrl: config.wsUrl,
        environment: config.environment,
        userId,
        onChange: setFlags,
        onConnectionStateChange: setConnection,
      }),
    [userId],
  );

  useEffect(() => {
    void client.start().catch(() => {
      // The connection indicator already reflects the failure; there is nothing useful to add.
    });
    return () => {
      void client.stop();
    };
  }, [client]);

  return { flags, connection };
}
