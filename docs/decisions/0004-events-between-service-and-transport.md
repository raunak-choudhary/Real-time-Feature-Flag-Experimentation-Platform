# 0004. Services announce changes rather than transmitting them

**Status:** Accepted

## Context

When a flag changes, connected clients must be told. The direct implementation calls the
notification code from the service that made the change.

That creates two problems. The service acquires a dependency on the transport, so it can no longer
be tested or reused without one. More seriously, the notification is sent at the moment the code
runs, which is before the surrounding transaction commits. A client acting on the notification by
re-reading configuration can therefore observe the old value and conclude the notification was
wrong. If the transaction subsequently fails, clients have been told about a change that never
happened.

## Decision

Services publish a description of what changed, expressed without reference to any transport. A
separate component subscribes to those descriptions and is responsible for delivery. Delivery is
deferred until the surrounding transaction has committed.

## Alternatives considered

**Calling the transport directly and committing before notifying.** Rejected because it requires
every author of every mutating operation to remember the ordering, and forgetting produces a race
that appears only under load.

**A message broker between the two.** Correct in principle and appropriate at larger scale.
Rejected as disproportionate: it introduces an operational dependency, a delivery-guarantee
discussion, and a failure mode for a problem that the in-process mechanism already solves within a
single instance.

## Consequences

Services are testable without a transport, and the transport is replaceable without touching them.
Notifications cannot describe a state that a subsequent read would contradict.

There is one deliberate exception. Automated rollout advancement runs on a timer rather than inside
a request, and the transaction boundary there is such that the deferred delivery never fires. That
path writes and then announces directly, in that order. The exception is confined to one component
and documented where it occurs, because an undocumented inconsistency of this kind reads as a bug.

Notifications are not durable. A client disconnected at the moment of a change does not receive it,
which is addressed by having clients refetch on every connection rather than by adding persistence
here.
