/** Shapes returned by the API that the dashboard renders. */

export interface FlagRow {
  readonly id: number;
  readonly name: string;
  readonly description: string | null;
  readonly enabled: boolean;
  readonly status: string;
  readonly rolloutPercentage: number;
  readonly environment: string;
}

export interface AuditEntry {
  readonly id: number;
  readonly actor: string;
  readonly action: string;
  readonly targetName: string | null;
  readonly beforeValue: string | null;
  readonly afterValue: string | null;
  readonly reason: string | null;
  readonly occurredAt: string;
}

export interface RolloutStageView {
  readonly stageOrder: number;
  readonly targetPercentage: number;
  readonly dwellMinutes: number;
}

export interface RolloutView {
  readonly id: number;
  readonly flagName: string;
  readonly status: "PENDING" | "RUNNING" | "PAUSED" | "COMPLETED" | "ROLLED_BACK";
  readonly currentStageIndex: number;
  readonly haltedReason: string | null;
  readonly stages: readonly RolloutStageView[];
}
