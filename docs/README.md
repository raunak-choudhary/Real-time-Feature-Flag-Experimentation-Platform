# REX Platform documentation

This directory explains why the platform works the way it does.

It is deliberately not a walkthrough of the source. Code already states what it does, and a
document that restates it becomes wrong the moment either one changes. What code cannot state is
the reasoning behind it: the alternative that was tried and abandoned, the constraint that made an
obvious approach unworkable, the failure that a piece of apparent over-engineering exists to
prevent. That reasoning is what lives here.

The material is separated by purpose rather than by package, following the convention that
orientation, instruction, specification and explanation are four different jobs that read badly
when mixed together.

## Understanding the system

Read these to understand how the platform thinks. They assume no familiarity with the codebase.

| Document | Subject |
|---|---|
| [Architecture](architecture.md) | The shape of the system, its boundaries, and what flows across them |
| [Deterministic assignment](assignment.md) | How a user is placed into a rollout or a variant, and why the answer never changes |
| [Experiment analysis](experiment-analysis.md) | How the platform decides whether a difference between two variants is real |
| [Progressive delivery](progressive-delivery.md) | How a release advances on its own and how it retreats when something goes wrong |
| [Real-time propagation](real-time-propagation.md) | How a change reaches a running client without anyone asking for it |
| [Quality gates](quality-gates.md) | What the automated checks are for, and what each of them has actually caught |

## Decisions

Significant technical choices are recorded individually, each with the alternatives that were
rejected and the reason. See the [decision log](decisions/README.md).

## Running the platform

Setup, configuration and deployment are covered in the [project README](../README.md).
