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
    return evaluate(flag, userId, environment, java.util.Map.of());
  }

  /** Evaluates with user attributes available for targeting rules. */
  public static EvaluationResult evaluate(
      FlagContext flag,
      String userId,
      String environment,
      java.util.Map<String, String> attributes) {
    if (flag == null) {
      return EvaluationResult.off(EvaluationReason.FLAG_NOT_FOUND);
    }
    if (!flag.enabled()) {
      return EvaluationResult.off(EvaluationReason.FLAG_DISABLED);
    }
    if (flag.environment() != null && !flag.environment().equals(environment)) {
      return EvaluationResult.off(EvaluationReason.ENVIRONMENT_MISMATCH);
    }

    // Targeting rules take precedence over the percentage, so a rule can admit a whole segment
    // regardless of bucket, and the percentage remains the fallback for everyone else.
    java.util.Optional<Boolean> ruled = RuleEvaluator.firstMatch(flag.rules(), attributes);
    if (ruled.isPresent()) {
      return ruled.get()
          ? new EvaluationResult(true, EvaluationReason.TARGETING_RULE_MATCH, null)
          : new EvaluationResult(false, EvaluationReason.TARGETING_RULE_EXCLUDED, null);
    }

    int bucket = BucketHasher.bucketFor(flag.name(), userId);
    return BucketHasher.isInRollout(flag.name(), userId, flag.rolloutPercentage())
        ? EvaluationResult.included(bucket)
        : EvaluationResult.excluded(bucket);
  }
}
