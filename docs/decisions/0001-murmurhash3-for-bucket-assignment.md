# 0001. Bucket assignment uses a distribution-oriented hash

**Status:** Accepted

## Context

Placing a user into a rollout requires reducing their identifier to a position in a fixed range.
The result must be identical everywhere and forever, and the resulting distribution must be
indistinguishable from a fair sample.

The original implementation used the language's built-in hash for text, made it positive by
negating it where necessary, and reduced it into range. Two problems were found.

The built-in hash is designed to spread keys across the buckets of a hash table. It has poor
avalanche behaviour, so similar inputs produce similar outputs. Real user identifiers are
overwhelmingly similar, being sequential values or timestamps sharing a prefix, and the resulting
clustering means a stated rollout percentage does not match the fraction actually reached.

Separately, the range of signed integers is asymmetric: one value has no representable negation and
stays negative when negated. That value produced a negative position and an invalid result, for
roughly one identifier in four billion. It would not have been found by testing.

## Decision

Use a general-purpose hash designed for distribution quality, and treat its output as unsigned
throughout so the asymmetry cannot arise.

## Alternatives considered

**A cryptographic hash, truncated.** Distribution is excellent and the sign problem does not arise.
Rejected because it is substantially slower for no benefit at all here. Nothing about this needs
to resist an adversary; it needs to be uniform and fast, and it sits on the hot path of every
evaluation.

**Storing each assignment when first computed.** Removes the question of hash quality by never
recomputing. Rejected because it turns a stateless calculation into a storage problem growing with
the user base, adds a database read to the hot path, and requires coordination between machines
that the stateless approach avoids entirely.

**Keeping the built-in hash and fixing only the sign.** Rejected because it addresses the visible
defect and leaves the distribution problem, which is the more damaging of the two and the harder
to notice.

## Consequences

Distribution is verified by a statistical test of uniformity over a large synthetic population
rather than assumed, and the value that caused the original defect is covered by a specific case.
Assignment remains stateless, so any number of instances agree without communicating.

The cost is a hand-written implementation of a standard algorithm, which must match the published
specification exactly for assignments to remain stable across versions. Its behaviour is therefore
pinned by tests against known inputs.
