# 0005. Guardrails measure from stage entry, not a trailing window

**Status:** Accepted

## Context

A guardrail asks whether a metric has stayed within an acceptable range while a rollout stage has
been in effect. Answering it requires choosing a period to measure over.

The original implementation used a fixed trailing window of thirty minutes. This is wrong under a
condition that is neither exotic nor unlikely.

A stage with a two hour dwell time spends most of its life in a state where a trailing thirty
minute window has drifted entirely past the moment the stage began. The window then covers a
period during which the exposure level did not change and nothing of interest occurred. A change
that caused a burst of errors immediately on exposure passes its guardrail an hour later, because
the evidence has scrolled out of the window.

The defect was found because tests that simulate the passage of time in order to exercise dwell
behaviour began failing in a way that initially appeared to be a fault in the tests.

## Decision

Measure from the moment the current stage was entered, up to the present.

## Alternatives considered

**A longer trailing window.** Reduces the frequency of the problem without removing it, and
requires the window to be tuned against the longest dwell time any team might configure. A rule
that depends on a coincidence between two independently chosen numbers is not a rule.

**A window matched to the dwell time of the stage.** Effectively the same as the decision but
computed indirectly, and it breaks if a stage is entered late or the schedule is paused.

## Consequences

The measured period always corresponds exactly to the period the guardrail is asking about, and it
widens as the stage progresses, so the judgement becomes better informed with time rather than
more forgetful.

Because early in a stage the window is short and may contain very little traffic, verdicts carry
the number of observations behind them. A breach supported by three requests and one supported by
twelve hundred are different events, and the record makes them distinguishable afterwards.

The implementation is simpler than the one it replaced, having one fewer configured value.
