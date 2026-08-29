import type { EvaluationResult } from "./types.js";

/**
 * Reads a decision out of a set of evaluation results.
 *
 * <p>Returns the caller's default when the flag is absent, because an SDK that throws on an
 * unknown flag turns a configuration gap into an outage in the consuming application.
 */
export function isFlagOn(
  results: readonly EvaluationResult[],
  flagName: string,
  fallback = false,
): boolean {
  const match = results.find((result) => result.flagName === flagName);
  return match?.enabled ?? fallback;
}
