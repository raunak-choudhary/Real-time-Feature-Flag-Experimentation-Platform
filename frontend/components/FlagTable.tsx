import type { FlagRow } from "../lib/types";

interface Props {
  readonly flags: readonly FlagRow[];
  /** Live decisions keyed by flag name, applied over the server rendered rows as they arrive. */
  readonly liveState?: ReadonlyMap<string, boolean>;
}

export function FlagTable({ flags, liveState }: Props) {
  if (flags.length === 0) {
    return <p className="empty">No flags in this environment yet.</p>;
  }

  return (
    <table>
      <caption className="empty" style={{ captionSide: "bottom", textAlign: "left" }}>
        {flags.length} flag{flags.length === 1 ? "" : "s"}
      </caption>
      <thead>
        <tr>
          <th scope="col">Flag</th>
          <th scope="col">State</th>
          <th scope="col">Rollout</th>
        </tr>
      </thead>
      <tbody>
        {flags.map((flag) => {
          // A pushed decision wins over the server rendered value, which is what makes the
          // table update without a reload.
          const enabled = liveState?.get(flag.name) ?? flag.enabled;
          return (
            <tr key={flag.id} data-testid={`flag-${flag.name}`}>
              <td>
                <strong>{flag.name}</strong>
                {flag.description ? (
                  <div className="actor">{flag.description}</div>
                ) : null}
              </td>
              <td>
                <span className={`pill ${enabled ? "pill-on" : "pill-off"}`}>
                  {enabled ? "On" : "Off"}
                </span>
              </td>
              <td>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <div
                    className="meter"
                    role="progressbar"
                    aria-valuenow={flag.rolloutPercentage}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-label={`${flag.name} rollout`}
                  >
                    <span style={{ width: `${String(flag.rolloutPercentage)}%` }} />
                  </div>
                  <span className="actor">{flag.rolloutPercentage}%</span>
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
