import type { ConnectionState } from "@rex/sdk";

const LABELS: Record<ConnectionState, string> = {
  connecting: "Connecting",
  live: "Live",
  reconnecting: "Reconnecting",
  closed: "Disconnected",
};

/**
 * Shows the socket state.
 *
 * Without this the difference between "nothing has changed" and "we stopped receiving changes"
 * is invisible, and a stale dashboard looks exactly like a quiet one.
 */
export function LiveIndicator({ state }: { state: ConnectionState }) {
  const modifier =
    state === "live" ? "live" : state === "closed" ? "closed" : "reconnecting";

  return (
    <span className={`indicator indicator-${modifier}`} role="status" aria-live="polite">
      <span className="dot" aria-hidden="true" />
      {LABELS[state]}
    </span>
  );
}
