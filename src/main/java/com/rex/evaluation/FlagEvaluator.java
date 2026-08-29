package com.rex.evaluation;

/**
 * Decides whether a flag is on for a user, and says why.
 *
 * <p>Precedence is deliberate and ordered cheapest first: the kill switch, then the environment,
 * then the bucket. Each step returns a distinct reason, so an operator can tell a disabled flag
 * apart from one the user simply is not bucketed into.
 *
 * <p>Flags are evaluated statelessly. A decision is recomputed from current configuration on every
 * call, so changing a rollout percentage takes effect immediately. Experiment assignments are the
 * opposite, sticky and persisted, because moving a user between variants mid experiment would
 * invalidate the result. Conflating the two is the most common way flag platforms corrupt their own
 * experiment data.
 */
public final class FlagEvaluator {

  private FlagEvaluator() {}

  public static EvaluationResult evaluate(FlagContext flag, String userId, String environment) {
    if (flag == null) {
      return EvaluationResult.off(EvaluationReason.FLAG_NOT_FOUND);
    }
    if (!flag.enabled()) {
      return EvaluationResult.off(EvaluationReason.FLAG_DISABLED);
    }
    if (flag.environment() != null && !flag.environment().equals(environment)) {
      return EvaluationResult.off(EvaluationReason.ENVIRONMENT_MISMATCH);
    }

    int bucket = BucketHasher.bucketFor(flag.name(), userId);
    return BucketHasher.isInRollout(flag.name(), userId, flag.rolloutPercentage())
        ? EvaluationResult.included(bucket)
        : EvaluationResult.excluded(bucket);
  }
}
