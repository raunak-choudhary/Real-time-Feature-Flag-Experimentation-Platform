import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const css = readFileSync(resolve(process.cwd(), "frontend/app/globals.css"), "utf8");

/**
 * Guards the two rules that are easy to break later and expensive to retrofit: every colour is a
 * token, and both themes define the same set.
 */
describe("theme tokens", () => {
  const tokenNames = (block: string): string[] =>
    [...block.matchAll(/(--[a-z-]+):/g)].map((match) => match[1] ?? "");

  const lightBlock = css.slice(css.indexOf(":root {"), css.indexOf('[data-theme="dark"]'));
  const darkBlock = css.slice(css.indexOf('[data-theme="dark"]'), css.indexOf("* {"));

  it("defines a light palette and a dark palette", () => {
    expect(lightBlock).toContain("--bg:");
    expect(darkBlock).toContain("--bg:");
  });

  it("dark theme redefines every token the light theme declares", () => {
    const light = tokenNames(lightBlock).filter((t) => t !== "--radius");
    const dark = new Set(tokenNames(darkBlock));

    const missing = light.filter((token) => !dark.has(token));
    expect(missing, `tokens missing from the dark theme: ${missing.join(", ")}`).toEqual([]);
  });

  it("declares the three responsive breakpoints the design targets", () => {
    expect(css).toContain("@media (min-width: 768px)");
    expect(css).toContain("@media (min-width: 1280px)");
    // 375 is the mobile-first base, so it is the unqualified rule set rather than a media query.
    expect(css).toContain(".shell {");
  });

  it("uses tokens rather than raw colours in component rules", () => {
    const componentRules = css.slice(css.indexOf("* {"));
    const rawColours = componentRules.match(/:\s*#[0-9a-fA-F]{3,8}\b/g) ?? [];

    expect(rawColours, `hardcoded colours outside the token blocks: ${rawColours.join(", ")}`)
      .toEqual([]);
  });
});
