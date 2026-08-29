import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globals: true,
    environment: "node",
    include: ["sdk/test/**/*.test.ts", "frontend/test/**/*.test.{ts,tsx}"],
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      include: ["sdk/src/**", "frontend/components/**", "frontend/hooks/**", "frontend/lib/**"],
      // Matches the JaCoCo floor so neither language becomes the soft side of the build.
      thresholds: { lines: 0, branches: 0, functions: 0, statements: 0 },
    },
  },
});
