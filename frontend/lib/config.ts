/**
 * Runtime configuration.
 *
 * Read from the environment rather than hardcoded, because the dashboard is deployed separately
 * from the API and points at a different host in every environment.
 */
export const config = {
  apiUrl: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  wsUrl: process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080",
  environment: process.env.NEXT_PUBLIC_ENVIRONMENT ?? "production",
} as const;
