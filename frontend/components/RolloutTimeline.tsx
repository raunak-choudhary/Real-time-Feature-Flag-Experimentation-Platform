import type { RolloutView } from "../lib/types";

/**
 * Shows a staged rollout's progress.
 *
 * The automation is otherwise invisible: a flag simply changes percentage and nobody can tell
 * whether that was a person or the scheduler, nor how much further it intends to go.
 */
export function RolloutTimeline({ rollout }: { rollout: RolloutView }) {
  const rolledBack = rollout.status === "ROLLED_BACK";

  return (
    <div data-testid={`rollout-${rollout.flagName}`}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10 }}>
        <strong>{rollout.flagName}</strong>
        <span className={`pill ${rolledBack ? "pill-off" : "pill-neutral"}`}>
          {rollout.status.replace("_", " ").toLowerCase()}
        </span>
      </div>

      <ol className="timeline">
        {rollout.stages.map((stage, index) => {
          const done = index < rollout.currentStageIndex;
          const current = index === rollout.currentStageIndex;
          const modifier = rolledBack && current ? "rolled-back" : done ? "done" : current ? "current" : "";

          return (
            <li key={stage.stageOrder} className={`stage ${modifier ? `stage-${modifier}` : ""}`}>
              <span className="marker" aria-hidden="true" />
              <span>
                {stage.targetPercentage}% for {stage.dwellMinutes} min
              </span>
            </li>
          );
        })}
      </ol>

      {rollout.haltedReason ? (
        <p className="pill pill-off" style={{ marginTop: 12, whiteSpace: "normal" }}>
          Rolled back: {rollout.haltedReason}
        </p>
      ) : null}
    </div>
  );
}
