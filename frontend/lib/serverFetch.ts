import { config } from "./config";
import type { AuditEntry, FlagRow } from "./types";

/**
 * Server side data loading for the initial paint.
 *
 * Fetching here rather than in the browser means the dashboard opens with real rows instead of an
 * empty table that fills in a moment later. The socket takes over for every subsequent change.
 *
 * A failure returns an empty list rather than throwing. The API being briefly unavailable should
 * degrade the dashboard to an empty state, not replace it with an error page.
 */
async function loadJson<T>(path: string, fallback: T): Promise<T> {
  try {
    const response = await fetch(`${config.apiUrl}${path}`, { cache: "no-store" });
    if (!response.ok) {
      return fallback;
    }
    return (await response.json()) as T;
  } catch {
    return fallback;
  }
}

export function loadFlags(): Promise<FlagRow[]> {
  return loadJson<FlagRow[]>(`/api/v1/flags?environment=${config.environment}`, []);
}

export function loadAuditFeed(limit = 20): Promise<AuditEntry[]> {
  return loadJson<AuditEntry[]>(`/api/v1/audit?limit=${String(limit)}`, []);
}
