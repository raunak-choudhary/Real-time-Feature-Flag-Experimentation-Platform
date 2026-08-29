package com.rex.rollout;

/** The outcome of checking one guardrail against the exposed cohort. */
public record GuardrailVerdict(
    Guardrail guardrail, double observed, long observations, Status status) {

  public boolean blocksAdvance() {
    return status != Status.HEALTHY;
  }

  public String describe() {
    return switch (status) {
      case HEALTHY -> "%s at %.4f is within its threshold".formatted(guardrail.metric(), observed);
      case BREACHED ->
          "%s at %.4f breached its threshold of %.4f over %d observations"
              .formatted(guardrail.metric(), observed, guardrail.threshold(), observations);
      case INSUFFICIENT_DATA ->
          "%s has only %d observations, below the %d required"
              .formatted(guardrail.metric(), observations, guardrail.minimumObservations());
      case UNAVAILABLE -> "%s could not be measured".formatted(guardrail.metric());
    };
  }

  /** What the measurement permits us to conclude. */
  public enum Status {
    HEALTHY,
    BREACHED,
    /** Too few observations to judge. Blocks advancing, but does not trigger a rollback. */
    INSUFFICIENT_DATA,
    /**
     * The metric could not be read at all.
     *
     * <p>Blocks advancing rather than allowing it. Failing open here would mean a monitoring outage
     * silently turns a guarded rollout into an unguarded one.
     */
    UNAVAILABLE
  }
}
