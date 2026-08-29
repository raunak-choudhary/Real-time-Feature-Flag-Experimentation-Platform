"use client";

import { useEffect, useState } from "react";

type Theme = "light" | "dark";
const STORAGE_KEY = "rex-theme";

/**
 * Light and dark switch.
 *
 * Reads the stored preference on mount rather than during render, because the server has no access
 * to localStorage and rendering a different theme there would cause a hydration mismatch.
 */
export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>("light");

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    const preferred: Theme =
      stored === "dark" || stored === "light"
        ? stored
        : window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";
    setTheme(preferred);
    document.documentElement.dataset["theme"] = preferred;
  }, []);

  function toggle(): void {
    const next: Theme = theme === "light" ? "dark" : "light";
    setTheme(next);
    document.documentElement.dataset["theme"] = next;
    window.localStorage.setItem(STORAGE_KEY, next);
  }

  return (
    <button type="button" className="theme-toggle" onClick={toggle}>
      {theme === "light" ? "Dark" : "Light"} theme
    </button>
  );
}
