/** Domain types shared by the SDK and the dashboard. These mirror the API response contract. */

export type FlagStatus = "ACTIVE" | "INACTIVE" | "DEPRECATED" | "ARCHIVED";

export type EvaluationReason =
  | "FLAG_NOT_FOUND"
  | "FLAG_DISABLED"
  | "ENVIRONMENT_MISMATCH"
  | "TARGETING_RULE_MATCH"
  | "TARGETING_RULE_EXCLUDED"
  | "ROLLOUT_INCLUDED"
  | "ROLLOUT_EXCLUDED";

export type ChangeType = "CREATED" | "TOGGLED" | "ROLLOUT_CHANGED" | "UPDATED" | "ARCHIVED";

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
 * The reason is part of the contract rather than a debugging aid: without it a caller can see that
 * a flag is off but not whether that was the kill switch, the environment, or their bucket.
 */
export interface EvaluationResult {
  readonly flagName: string;
  readonly enabled: boolean;
  readonly reason: EvaluationReason;
  readonly bucket: number | null;
}

/** A change pushed over the socket. */
export interface FlagChangedEvent {
  readonly flagId: number;
  readonly flagName: string;
  readonly environment: string;
  readonly enabled: boolean;
  readonly rolloutPercentage: number;
  readonly changeType: ChangeType;
  readonly occurredAt: string;
}

export type ConnectionState = "connecting" | "live" | "reconnecting" | "closed";

export interface RexClientOptions {
  readonly apiUrl: string;
  readonly wsUrl: string;
  readonly environment: string;
  readonly userId: string;
  /** Called whenever the cache changes, so a UI can re-render without polling. */
  readonly onChange?: (flags: readonly EvaluationResult[]) => void;
  readonly onConnectionStateChange?: (state: ConnectionState) => void;
}
