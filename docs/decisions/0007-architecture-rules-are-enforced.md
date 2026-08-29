# 0007. Layering is enforced by a test, not by convention

**Status:** Accepted

## Context

The system depends on dependencies pointing inward: the transport layer may know about services,
services may know about repositories, and the decision engines know about nothing. A service that
reaches outward into the transport layer has become untestable without a web server, and an engine
that reaches into storage has become untestable without a database.

Both are easy to introduce while concentrating on something else. The natural way to return data
from a service is to return the type the caller ultimately wants, and the type the caller wants is
frequently a transport-layer type. The violation is one import and it looks like tidiness.

## Decision

Express the layering as an automated check over the compiled code, run on every change alongside
the tests.

## Alternatives considered

**Documenting the rule and relying on review.** This was the initial position. It failed three
times, in three different features, each written with full knowledge of the rule. That record is
the evidence: the rule is easy to believe in and easy to breach without noticing.

**Separate build modules with enforced boundaries.** Stronger, since a violation would not compile.
Rejected as disproportionate for a project of this size: it multiplies build configuration and
makes ordinary changes spanning layers more tedious, for an improvement over a check that already
catches every case.

## Consequences

Violations are caught within minutes of being written, when the context is still in the author's
head, rather than during a later refactor when the reason for the dependency has been forgotten.

Each of the three occurrences was resolved the same way: the service returns a type belonging to
the domain, and the transport layer translates it. That translation is a small amount of
additional code and is the price of the boundary.

The check is stated as a rule about packages rather than about individual classes, so it continues
to apply to code that does not exist yet.
