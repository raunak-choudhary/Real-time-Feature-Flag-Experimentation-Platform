package com.rex.evaluation;

/**
 * Why a particular decision was served.
 *
 * <p>Part of the contract rather than a debugging aid. Without it a caller can see that a flag is
 * off but not whether that was the kill switch, the environment, or their bucket, which is the
 * difference between a diagnosable system and a mysterious one.
 */
public enum EvaluationReason {
  FLAG_NOT_FOUND,
  FLAG_DISABLED,
  ENVIRONMENT_MISMATCH,
  ROLLOUT_INCLUDED,
  ROLLOUT_EXCLUDED
}
