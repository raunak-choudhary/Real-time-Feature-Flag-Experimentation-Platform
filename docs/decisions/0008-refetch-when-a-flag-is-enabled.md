# 0008. Clients refetch when a flag is enabled, not when disabled

**Status:** Accepted

## Context

A connected client is told when a flag changes and must update its local cache. The two directions
are not symmetric, and treating them as though they were produces a serious defect.

When a flag is switched off it is off for everyone. The client has everything it needs and can
apply the change locally.

When a flag is switched on, being enabled is a precondition rather than a conclusion. Whether a
particular user is included still depends on their position relative to the rollout percentage and
on any targeting rules. A client treating the announcement as meaning the feature is on would show
it to its entire user base at the moment a one percent rollout began.

## Decision

The cache distinguishes the two cases. A change it can resolve on its own is applied locally. A
change it cannot resolve triggers a refetch of the decisions for that user.

## Alternatives considered

**Including the evaluated decision in the announcement.** Removes the refetch entirely. Rejected
because the announcement is broadcast to every subscriber and the decision differs per user. It
would require either a message per user or moving evaluation into the transport layer.

**Refetching on every change in either direction.** Simpler and uniformly correct. Rejected
because turning a flag off is the incident path, where the delay of a round trip is least
acceptable. The asymmetry exists to make the urgent direction the fast one.

**Applying every change optimistically and correcting later.** Rejected because the incorrect
intermediate state is visible to users, and in the enabling direction it is precisely the failure
the rollout percentage exists to prevent.

## Consequences

Disabling a flag takes effect locally and immediately, with no round trip. Enabling costs one
request per connected client, which is acceptable because enabling is not the urgent direction.

The asymmetry is not obvious from reading the cache in isolation, so it is stated explicitly where
it is implemented and covered by tests asserting that the two directions behave differently. Without
that, a later simplification making the two cases uniform would look like an improvement.
