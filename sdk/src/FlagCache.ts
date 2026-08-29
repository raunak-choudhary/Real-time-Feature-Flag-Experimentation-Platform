import type { EvaluationResult, FlagChangedEvent } from "./types.js";

/**
 * The local decision cache.
 *
 * Evaluations read from here and are synchronous, which is what real SDKs do and why they are
 * fast. A network call per flag check would put the platform in the critical path of every page
 * render in the consuming application.
 */
export class FlagCache {
  private readonly entries = new Map<string, EvaluationResult>();

  replaceAll(results: readonly EvaluationResult[]): void {
    this.entries.clear();
    for (const result of results) {
      this.entries.set(result.flagName, result);
    }
  }

  /**
   * Applies a pushed change.
   *
   * A change that turns a flag off is applied directly, since the decision is unambiguous. A change
   * that turns one on cannot be: whether this particular user is included depends on their bucket,
   * which only the server knows. Returning false asks the caller to refetch rather than guessing.
   */
  applyChange(event: FlagChangedEvent): boolean {
    if (!event.enabled) {
      this.entries.set(event.flagName, {
        flagName: event.flagName,
        enabled: false,
        reason: "FLAG_DISABLED",
        bucket: null,
      });
      return true;
    }
    return false;
  }

  isEnabled(flagName: string, fallback = false): boolean {
    return this.entries.get(flagName)?.enabled ?? fallback;
  }

  reasonFor(flagName: string): EvaluationResult["reason"] | undefined {
    return this.entries.get(flagName)?.reason;
  }

  snapshot(): readonly EvaluationResult[] {
    return [...this.entries.values()];
  }

  get size(): number {
    return this.entries.size;
  }
}
