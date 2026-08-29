import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AuditFeed } from "../components/AuditFeed";
import type { AuditEntry } from "../lib/types";

const toggleEntry: AuditEntry = {
  id: 1,
  actor: "operator",
  action: "TOGGLED",
  targetName: "dark_mode",
  beforeValue: null,
  afterValue: "enabled=true rollout=100%",
  reason: null,
  occurredAt: "2026-08-29T12:00:00",
};

const rollbackEntry: AuditEntry = {
  id: 2,
  actor: "system:rollout-scheduler",
  action: "ROLLED_BACK",
  targetName: "checkout_v2",
  beforeValue: "rollout=25%",
  afterValue: "rollout=5%",
  reason: "ERROR_RATE breached its threshold",
  occurredAt: "2026-08-29T12:05:00",
};

const entries: AuditEntry[] = [toggleEntry, rollbackEntry];

describe("AuditFeed", () => {
  it("renders an entry per change", () => {
    render(<AuditFeed entries={entries} />);

    expect(screen.getByTestId("audit-1")).toBeInTheDocument();
    expect(screen.getByTestId("audit-2")).toBeInTheDocument();
  });

  it("distinguishes an automated action from an operator one", () => {
    render(<AuditFeed entries={entries} />);

    expect(within(screen.getByTestId("audit-1")).getByText("operator")).toBeInTheDocument();
    expect(within(screen.getByTestId("audit-2")).getByText("automated")).toBeInTheDocument();
  });

  it("shows the reason a rollback happened", () => {
    render(<AuditFeed entries={entries} />);

    expect(screen.getByText(/ERROR_RATE breached/)).toBeInTheDocument();
  });

  it("shows an empty state rather than a bare list", () => {
    render(<AuditFeed entries={[]} />);

    expect(screen.getByText(/no changes recorded yet/i)).toBeInTheDocument();
  });

  it("falls back to the raw timestamp when it cannot be parsed", () => {
    render(
      <AuditFeed
        entries={[{ ...toggleEntry, id: 3, occurredAt: "not-a-date" }]}
      />,
    );

    expect(screen.getByText(/not-a-date/)).toBeInTheDocument();
  });
});
