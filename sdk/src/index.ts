/**
 * Public surface of the REX SDK.
 *
 * Imports are extensionless because the package ships TypeScript source rather than a built
 * bundle, so every consumer compiles it and resolves through their own bundler.
 */
export { FlagCache } from "./FlagCache";
export { RexClient } from "./RexClient";
export { isFlagOn } from "./evaluation";
export type {
  ChangeType,
  ConnectionState,
  EvaluationReason,
  EvaluationResult,
  FeatureFlag,
  FlagChangedEvent,
  FlagStatus,
  RexClientOptions,
} from "./types";
