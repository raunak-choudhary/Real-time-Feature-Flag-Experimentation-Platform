import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FlagTable } from "../components/FlagTable";
import type { FlagRow } from "../lib/types";

const flags: FlagRow[] = [
  {
    id: 1,
    name: "dark_mode",
    description: "Dark theme",
    enabled: true,
    status: "ACTIVE",
    rolloutPercentage: 100,
    environment: "production",
  },
  {
    id: 2,
    name: "checkout_v2",
    description: null,
    enabled: false,
    status: "ACTIVE",
    rolloutPercentage: 25,
    environment: "production",
  },
];

describe("FlagTable", () => {
  it("renders a row per flag with its state and rollout", () => {
    render(<FlagTable flags={flags} />);

    expect(within(screen.getByTestId("flag-dark_mode")).getByText("On")).toBeInTheDocument();
    expect(within(screen.getByTestId("flag-checkout_v2")).getByText("Off")).toBeInTheDocument();
    expect(screen.getByText("25%")).toBeInTheDocument();
  });

  it("shows an empty state rather than a bare table when there are no flags", () => {
    render(<FlagTable flags={[]} />);

    expect(screen.getByText(/no flags in this environment/i)).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("a pushed decision overrides the server rendered value without a reload", () => {
    const live = new Map([["dark_mode", false]]);
    render(<FlagTable flags={flags} liveState={live} />);

    expect(within(screen.getByTestId("flag-dark_mode")).getByText("Off"))
      .toBeInTheDocument();
  });

  it("leaves rows untouched when no live decision has arrived for them", () => {
    const live = new Map([["dark_mode", false]]);
    render(<FlagTable flags={flags} liveState={live} />);

    expect(within(screen.getByTestId("flag-checkout_v2")).getByText("Off"))
      .toBeInTheDocument();
  });

  it("exposes rollout as an accessible progress bar rather than colour alone", () => {
    render(<FlagTable flags={flags} />);

    const meter = screen.getByRole("progressbar", { name: /checkout_v2 rollout/i });
    expect(meter).toHaveAttribute("aria-valuenow", "25");
    expect(meter).toHaveAttribute("aria-valuemax", "100");
  });

  it("omits the description line for a flag that has none", () => {
    render(<FlagTable flags={flags} />);

    expect(within(screen.getByTestId("flag-dark_mode")).getByText("Dark theme"))
      .toBeInTheDocument();
    expect(within(screen.getByTestId("flag-checkout_v2")).queryByText("Dark theme"))
      .not.toBeInTheDocument();
  });
});
