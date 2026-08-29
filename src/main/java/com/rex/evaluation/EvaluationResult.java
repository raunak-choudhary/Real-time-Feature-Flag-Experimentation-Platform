package com.rex.evaluation;

/** The outcome of evaluating one flag for one user, with the bucket that produced it. */
public record EvaluationResult(boolean enabled, EvaluationReason reason, Integer bucket) {

  public static EvaluationResult off(EvaluationReason reason) {
    return new EvaluationResult(false, reason, null);
  }

  public static EvaluationResult included(int bucket) {
    return new EvaluationResult(true, EvaluationReason.ROLLOUT_INCLUDED, bucket);
  }

  public static EvaluationResult excluded(int bucket) {
    return new EvaluationResult(false, EvaluationReason.ROLLOUT_EXCLUDED, bucket);
  }
}
