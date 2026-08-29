# 0002. Rollout is expressed in hundredths of a percent

**Status:** Accepted

## Context

A rollout percentage has to be compared against a user's computed position. The natural choice is
a scale of one hundred positions matching the percentage directly.

That scale cannot express any exposure below one percent. The only value beneath it is zero.

## Decision

Compute positions on a scale of ten thousand and express rollout in hundredths of a percent.

## Alternatives considered

**A scale of one hundred.** Simpler and matches how the value is displayed. Rejected because the
first stage of a genuinely cautious release is often well below one percent, and a scale that
cannot express it forces the first step to be a hundred times larger than intended.

**Floating point percentages.** Arbitrarily fine and avoids choosing a scale. Rejected because
comparisons become subject to representation error, which is an unwelcome property in the
calculation deciding what a user sees. Integer arithmetic gives an exact answer.

**A scale of one million.** Finer still. Rejected as precision beyond any plausible use. Ten
thousand supports exposure to one in ten thousand users, which is already below the level at which
a stage produces enough traffic to observe anything.

## Consequences

The smallest expressible exposure is one hundredth of a percent, which is well below what any
practical first stage requires. The scale difference between stored values and displayed
percentages is a conversion that must be applied consistently, and is confined to the boundary
where values enter and leave the system.
