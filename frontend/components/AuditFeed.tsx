import type { AuditEntry } from "../lib/types";

const SYSTEM_ACTOR_PREFIX = "system:";

function formatTime(iso: string): string {
  const parsed = new Date(iso);
  return Number.isNaN(parsed.getTime()) ? iso : parsed.toLocaleString();
}

/** Recent configuration changes, human and automated, most recent first. */
export function AuditFeed({ entries }: { entries: readonly AuditEntry[] }) {
  if (entries.length === 0) {
    return <p className="empty">No changes recorded yet.</p>;
  }

  return (
    <ul className="feed">
      {entries.map((entry) => {
        const automated = entry.actor.startsWith(SYSTEM_ACTOR_PREFIX);
        return (
          <li key={entry.id} data-testid={`audit-${String(entry.id)}`}>
            <span>
              <strong>{entry.action.replace("_", " ").toLowerCase()}</strong>
              {entry.targetName ? ` on ${entry.targetName}` : ""}
            </span>
            {entry.reason ? <span className="actor">{entry.reason}</span> : null}
            <span className="actor">
              <span className={automated ? "automated" : undefined}>
                {automated ? "automated" : entry.actor}
              </span>
              {" · "}
              {formatTime(entry.occurredAt)}
            </span>
          </li>
        );
      })}
    </ul>
  );
}
