package com.rex.api.dto;

/**
 * The outcome of evaluating one flag for one user.
 *
 * <p>The reason is part of the contract, not a debugging extra. Without it a caller can see that a
 * flag is off but not whether that was the kill switch, the environment, or their bucket, which is
 * the difference between a diagnosable system and a mysterious one.
 */
public record EvaluationResponse(
    String flagName, boolean enabled, EvaluationReason reason, Integer bucket) {

  /** Why a particular decision was served. */
  public enum EvaluationReason {
    FLAG_DISABLED,
    ENVIRONMENT_MISMATCH,
    TARGETING_RULE_MATCH,
    TARGETING_RULE_EXCLUDED,
    ROLLOUT_INCLUDED,
    ROLLOUT_EXCLUDED,
    FLAG_NOT_FOUND
  }
}
