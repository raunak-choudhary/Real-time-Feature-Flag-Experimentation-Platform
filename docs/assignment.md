# Deterministic assignment

## What the problem actually is

Releasing a feature to a quarter of users sounds like a matter of picking a random quarter. It is
not, and the difference matters enormously.

If the choice were genuinely random, a user would land in the release on one page load and outside
it on the next. They would watch the new checkout appear and disappear as they moved through it.
Worse, an experiment measuring that release would be comparing two populations that both contain
the same people, which makes the resulting numbers meaningless. What is needed is not randomness
but the *appearance* of randomness combined with absolute repeatability: the same user must receive
the same answer every time, on every machine, forever, and yet the set of users receiving it must
be indistinguishable from a fair sample.

This is a hashing problem rather than a random number problem, and the distinction is the single
most important idea in this part of the system.

## Turning an identifier into a position

Every user identifier is combined with the name of the thing being decided and reduced to a number
in a fixed range. That number is the user's position, and it is stable because hashing is stable:
the same inputs always produce the same output, with no stored state and no coordination between
machines. Two servers on different continents, consulted a year apart, agree without ever having
communicated.

Including the flag or experiment name in what gets hashed is not incidental. If only the user
identifier were hashed, every flag would sort users into the same order, and a user unlucky enough
to sit high in that order would be excluded from every gradual release the organisation ever ran.
Mixing the name in gives each flag its own independent ordering, so exclusion from one release
tells you nothing about the next.

## Why the obvious approach fails

The language provides a built-in hash for text, and using it is the natural first move. It was in
fact the original implementation here, and it was wrong in two distinct ways.

The first is a matter of quality. That built-in hash was designed to spread keys across the buckets
of a hash table, where mediocre distribution costs a few extra comparisons and nothing more. It has
poor avalanche behaviour, meaning similar inputs produce similar outputs. Real user identifiers are
overwhelmingly similar to one another, being sequential numbers or timestamps with a shared prefix.
Feeding them through such a hash produces visible clustering, and clustering in this context means
a release described as reaching a quarter of users reaches some quite different fraction, silently.

The second is an outright defect. Converting the hash to a positive number before reducing it into
range relies on negation, and the range of signed integers is asymmetric: there is one value whose
negation is not representable and which therefore stays negative. One identifier in roughly four
billion produced a negative position and, from there, an invalid result. It would have been
essentially impossible to find by testing and extremely difficult to diagnose in production.

The replacement uses an algorithm designed for distribution rather than for hash tables, and treats
its output as unsigned throughout, so the asymmetry that caused the defect cannot arise. Both
properties are verified rather than assumed: distribution is checked with a statistical test of
uniformity across a large synthetic population, and the sign problem is covered by a case that
exercises precisely the value that used to fail.

## Why the scale is ten thousand rather than one hundred

Positions run from zero to nine thousand nine hundred and ninety-nine rather than zero to
ninety-nine, and a rollout is expressed in hundredths of a percent rather than whole percent.

The reason is that the interesting part of a gradual release is the beginning. Exposing a change to
one percent of traffic is a large step when the intent is to check that nothing catches fire.
Teams running genuinely risky changes want to start far below that, and a scale of a hundred
positions simply cannot express it: below one percent, the only available value is zero.

The finer scale costs nothing. It is the same arithmetic on a larger number, and it removes a
limitation that would otherwise be discovered at exactly the wrong moment, by someone trying to be
careful.

## The order in which a decision is reached

Evaluating a flag for a user is a sequence of questions, and the order is deliberate.

Whether the flag exists and is switched on is settled first, because both are absolute and neither
depends on who is asking. Environment is checked next, so a flag configured for one environment
cannot leak into another regardless of any other setting. Only then does anything user-specific
happen.

At that point targeting rules are consulted before the percentage. A rule is an explicit statement
about a category of user, and an explicit statement should outrank a statistical one: a team that
has said internal staff always receive the feature means always, not usually. Rules are evaluated
in order and the first match wins, which makes a specific exception placed above a general rule
behave the way anyone reading the list would expect. Only users unaddressed by any rule fall
through to the percentage.

Every decision is returned with the reason it was reached, not merely the outcome. This turns the
commonest support question in any flag system, which is why a particular user is or is not seeing
something, from an investigation into a lookup.

## What is deliberately not solved

Assignment here is stateless by design, which means the platform cannot guarantee that a user keeps
a feature if the rollout percentage is *reduced*. Shrinking a release from half to a quarter will
remove the feature from some users who previously had it. Preserving them would require
remembering every past decision, which trades a stateless computation for a storage problem that
grows with the user base.

The trade is deliberate. Rollouts almost always move upward, and a rollout moving downward is
usually an incident response where removing the feature from everyone affected is the point.

## Related reading

- [Experiment analysis](experiment-analysis.md) covers what happens once users are split.
- [Decision: MurmurHash3 for bucket assignment](decisions/0001-murmurhash3-for-bucket-assignment.md)
- [Decision: basis points instead of percentages](decisions/0002-basis-points-instead-of-percentages.md)
