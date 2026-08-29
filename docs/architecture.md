# Architecture

## The problem the shape solves

A feature flag platform sits in an awkward position. It is consulted on the hot path of every
request in every application that depends on it, so it has to be fast and it has to be available.
It is also a system of record for decisions that auditors and incident reviewers care about, so it
has to be durable and it has to remember. Those two demands pull in opposite directions, and most
of the structural decisions here are a response to that tension.

The resolution is a separation between *deciding* and *remembering*. Deciding whether a particular
user sees a particular feature is a pure computation over a small amount of configuration. It
involves no clock, no database, and no network. Remembering that the decision was made, and what
the configuration was at the time, is a durable write that happens around the decision rather than
inside it. Keeping those apart is what allows the decision to be tested exhaustively and reasoned
about with confidence, while the recording around it can be slow, batched, or temporarily absent
without changing what any user sees.

## Layers and the direction of dependency

The system is arranged in conventional layers, and the arrangement is enforced rather than merely
intended.

At the outside sits the HTTP and WebSocket surface: the endpoints applications call and the
channel over which changes are announced. Beneath it sits the service layer, which coordinates
work and owns transactions. Beneath that sit the repositories, which are the only components that
speak to the database. Alongside all of them sit the decision engines: assignment, evaluation and
statistics, which depend on nothing but the language itself.

Dependencies point inward only. A service may not reach outward into the transport layer, and the
engines may not reach anywhere at all. This is not a stylistic preference. A service that knows
about the shape of an HTTP response has quietly become untestable without a web server, and an
engine that knows about a database has become untestable without one. Both are easy to introduce
by accident and unpleasant to unwind later, so the rule is checked automatically on every change.
It has caught the violation three separate times, in each case within minutes of it being written.

## What crosses the boundary between deciding and announcing

When a flag changes, two things need to happen: the change is written down, and everyone currently
relying on the old answer is told. The obvious implementation calls the notification code directly
from the service that made the change. That couples them permanently, and it introduces a subtler
failure: the notification can be sent before the write has committed, so a client can be told about
a state that a subsequent read will not show.

Instead the service announces that something changed, in terms that carry no knowledge of how the
announcement travels. A separate component listens for those announcements and is responsible for
getting them onto the wire. The service does not know a socket exists. This makes the transport
replaceable, but the more immediate benefit is ordering: the announcement can be held until the
surrounding transaction has actually committed, so a client is never told about a change that a
subsequent read would contradict.

There is one deliberate exception, and it is worth understanding because it looks like an
inconsistency. Automated rollout advancement runs on a timer rather than inside a request, and the
transaction boundary there is different in a way that makes the deferred announcement never fire.
That path therefore writes and then announces directly, in that order. The exception is confined
to one component and exists because the general rule genuinely does not apply to it.

## Where state lives

The database holds configuration, assignments, recorded exposures, rollout schedules and the audit
trail. Its schema is versioned as an ordered sequence of migrations, applied automatically at
startup and verified against the mapping the application expects. If the two disagree the
application refuses to start rather than running against a schema it does not understand. That
strictness has paid for itself: it turns a class of subtle production misbehaviour into an
immediate and obvious startup failure.

Client-side, each connected application keeps a small cache of the decisions relevant to its user.
The cache exists so that consulting a flag is a local lookup rather than a network call. Keeping it
correct in the face of pushed updates is more subtle than it appears, and the reasoning is set out
in [real-time propagation](real-time-propagation.md).

## The dashboard's role

The dashboard is a thin surface over the same public interface any other client uses. It holds no
privileged access and no logic of its own beyond presentation. This is partly discipline and partly
evidence: because it consumes the platform the same way an application would, anything it can show
is something an application could have obtained, and a change appearing in the dashboard within
milliseconds is proof that the propagation path works rather than an illustration of it.

It is also, in engineering terms, the smallest part of the system. The substance is in the decision
engines and the delivery automation.

## Related reading

- [Deterministic assignment](assignment.md) covers the computation at the centre of the hot path.
- [Progressive delivery](progressive-delivery.md) covers the automation that changes configuration
  without a human present.
- The [decision log](decisions/README.md) records the individual choices behind this arrangement.
