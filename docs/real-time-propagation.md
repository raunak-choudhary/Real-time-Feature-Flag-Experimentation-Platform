# Real-time propagation

## What "real-time" has to mean here

The reason a feature flag platform exists is that turning something off should be faster than
deploying. If a change has to travel through a build and a release before it takes effect, the flag
has bought nothing over a configuration file.

Most implementations reach for polling: each client asks the platform for the current
configuration every thirty seconds or so. It is simple and it mostly works, and it fails in the
situation that matters most. During an incident, the average wait before a kill switch takes effect
is half the polling interval and the worst case is the whole of it. Fifteen seconds of continued
damage is a poor result for a mechanism whose entire justification is speed. Shortening the
interval trades that for constant load from every client asking a question whose answer is almost
always the same.

The alternative inverts the relationship. Clients hold an open connection and the platform tells
them when something changes. Nothing is transmitted while nothing is happening, and a change
reaches connected clients as fast as the network allows. Measured on a developer machine, the
delay from a change being applied to a client receiving it has a median of four milliseconds and a
ninety-fifth percentile of six. Those figures come from a local environment with no network between
the two ends and should be read as a floor rather than a production expectation, but the shape of
the result is the point: this is a different order of magnitude from polling, not an improvement on
it.

## Announcing without knowing who is listening

Changes are published to named channels organised by environment, and clients subscribe to the
channel for the environment they care about. Nothing tracks individual subscribers. A client that
disappears requires no cleanup, and one that arrives requires no registration.

The alternative, maintaining a list of interested clients and notifying each, sounds more precise
and creates a durable source of bugs. The list has to be kept accurate through disconnections,
restarts, and network partitions, and every inaccuracy is either a client that stops receiving
updates or a resource that is never released. Broadcasting to a channel sidesteps the entire
category by never holding the state that could go wrong.

## Why the announcement waits for the write

An announcement that a flag has changed is only true once the change has been committed. Sending it
earlier creates a window, brief but real, in which a client has been told about a state that the
database would not yet report. A client acting on the announcement by re-reading configuration
during that window receives the old value and concludes the announcement was wrong.

The service therefore does not send anything itself. It records that a change occurred, and the
delivery of that record is held until the surrounding transaction has committed. If the transaction
fails, nothing was announced, which is the correct outcome for a change that did not happen.

## Missed messages, and why the client refetches

A client that loses its connection and reconnects has a gap. Messages sent while it was away are
gone, because the channel holds no history.

Buffering messages for absent clients would require deciding how long to hold them and for whom,
which reintroduces exactly the per-client state that broadcasting was chosen to avoid. The client
instead refetches its full set of decisions every time it connects, including every reconnection.
The reasoning is that a client which has just reconnected does not know what it missed, and
therefore cannot know whether its cached state is stale. Fetching the current answer is cheap, and
it converts an unbounded uncertainty into a single request.

## The asymmetry in the client cache

This is the least obvious part of the design and the one most likely to be implemented incorrectly.

When a flag is switched off, every client can apply that immediately and locally. The flag is off
for everyone, so no further information is required to know what any user should now see.

When a flag is switched on, the announcement is not enough. Being enabled is a precondition, not a
conclusion: whether a particular user is included still depends on their position relative to the
rollout percentage and on any targeting rules. A client that treated the announcement as though it
meant "on" would show the feature to its entire user base at the moment a one percent rollout
began.

The cache therefore distinguishes the two cases explicitly. A change it can resolve on its own is
applied locally. A change it cannot resolve triggers a refetch of the decisions for its user. This
asymmetry is easy to miss when reading the code and disastrous to get wrong, so it is stated
directly at the point where it is implemented and covered by tests that assert the two cases behave
differently.

The choice of which side to be careful on is deliberate. Turning off is the incident path and must
be instant, so it is handled locally. Turning on is the rollout path and can afford a round trip.

## The client library as a boundary

The client-side logic lives in a library with no dependency on any user interface framework. The
dashboard binds it to a specific rendering approach in a thin layer that exists only for that
purpose.

The reason is that this logic, the cache, the connection handling, the refetch discipline, is not
about presentation. Allowing a framework into it would make it usable only from that framework and
would tie its correctness to the framework's lifecycle rules. Keeping the boundary means the same
library serves a server process or a differently built application without change, and that the
tricky parts can be tested without rendering anything.

## Related reading

- [Architecture](architecture.md) covers the boundary between deciding and announcing.
- [Decision: refetch when a flag is enabled](decisions/0008-refetch-when-a-flag-is-enabled.md)
