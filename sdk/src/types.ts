/** Domain types shared by the SDK and the dashboard. These mirror the API response contract. */

export type FlagStatus = "ACTIVE" | "INACTIVE" | "DEPRECATED" | "ARCHIVED";

export type EvaluationReason =
  | "FLAG_DISABLED"
  | "ENVIRONMENT_MISMATCH"
  | "TARGETING_RULE_MATCH"
  | "ROLLOUT_INCLUDED"
  | "ROLLOUT_EXCLUDED"
  | "FLAG_NOT_FOUND";

export interface FeatureFlag {
  readonly name: string;
  readonly enabled: boolean;
  readonly status: FlagStatus;
  readonly environment: string;
  readonly rolloutPercentage: number;
}

/**
 * The outcome of evaluating one flag for one user.
 *
 * <p>The reason is part of the contract rather than a debugging aid: without it a caller can see
 * that a flag is off but not whether that was the kill switch, the environment, or their bucket.
 */
export interface EvaluationResult {
  readonly flagName: string;
  readonly enabled: boolean;
  readonly reason: EvaluationReason;
}

export interface RexClientOptions {
  readonly apiUrl: string;
  readonly wsUrl: string;
  readonly environment: string;
}
