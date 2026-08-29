import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RolloutTimeline } from "../components/RolloutTimeline";
import type { RolloutView } from "../lib/types";

function rollout(overrides: Partial<RolloutView> = {}): RolloutView {
  return {
    id: 1,
    flagName: "checkout_v2",
    status: "RUNNING",
    currentStageIndex: 1,
    haltedReason: null,
    stages: [
      { stageOrder: 0, targetPercentage: 5, dwellMinutes: 60 },
      { stageOrder: 1, targetPercentage: 25, dwellMinutes: 60 },
      { stageOrder: 2, targetPercentage: 100, dwellMinutes: 60 },
    ],
    ...overrides,
  };
}

describe("RolloutTimeline", () => {
  it("renders every stage with its percentage and dwell time", () => {
    render(<RolloutTimeline rollout={rollout()} />);

    expect(screen.getByText("5% for 60 min")).toBeInTheDocument();
    expect(screen.getByText("25% for 60 min")).toBeInTheDocument();
    expect(screen.getByText("100% for 60 min")).toBeInTheDocument();
  });

  it("marks completed stages differently from the current one", () => {
    const { container } = render(<RolloutTimeline rollout={rollout()} />);

    expect(container.querySelectorAll(".stage-done")).toHaveLength(1);
    expect(container.querySelectorAll(".stage-current")).toHaveLength(1);
  });

  it("advancing a stage moves the current marker without a reload", () => {
    const { container, rerender } = render(<RolloutTimeline rollout={rollout()} />);
    expect(container.querySelectorAll(".stage-done")).toHaveLength(1);

    rerender(<RolloutTimeline rollout={rollout({ currentStageIndex: 2 })} />);

    expect(container.querySelectorAll(".stage-done")).toHaveLength(2);
  });

  it("renders a rolled back rollout distinctly from a normal stage change", () => {
    const { container } = render(
      <RolloutTimeline
        rollout={rollout({
          status: "ROLLED_BACK",
          haltedReason: "ERROR_RATE at 0.0850 breached its threshold of 0.0200",
        })}
      />,
    );

    expect(container.querySelectorAll(".stage-rolled-back")).toHaveLength(1);
    expect(screen.getByText(/rolled back:/i)).toBeInTheDocument();
    expect(screen.getByText(/ERROR_RATE/)).toBeInTheDocument();
  });

  it("shows no rollback banner while the rollout is healthy", () => {
    render(<RolloutTimeline rollout={rollout()} />);

    expect(screen.queryByText(/rolled back:/i)).not.toBeInTheDocument();
  });

  it("names the flag and its status", () => {
    render(<RolloutTimeline rollout={rollout({ status: "COMPLETED" })} />);

    const panel = screen.getByTestId("rollout-checkout_v2");
    expect(within(panel).getByText("checkout_v2")).toBeInTheDocument();
    expect(within(panel).getByText("completed")).toBeInTheDocument();
  });
});
