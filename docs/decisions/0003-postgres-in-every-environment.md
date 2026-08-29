# 0003. The same database engine runs in every environment

**Status:** Accepted

## Context

The project began with an in-memory database for local work and tests, and a different engine
intended for deployment. This is a common arrangement, chosen because an in-memory database starts
instantly and needs no installation.

It produced a series of failures that all shared one shape: something worked locally and failed on
the real engine.

Index names that collided across tables were tolerated by the in-memory engine and rejected by the
real one, leaving a table with no indexes at all. Seed data used date arithmetic and a random
function that exist in one engine and not the other, and failed outright on first contact. Two
reporting queries formatted timestamps with a function absent from the real engine, and threw
there while passing every local test.

Each was found later and more expensively than it needed to be, and the last of them reached a
deployed environment.

## Decision

Run the same database engine everywhere, including in tests, using disposable containerised
instances so that no manual installation is required.

## Alternatives considered

**Keeping the in-memory database and restricting queries to a portable subset.** Rejected because
it depends on every author knowing which subset is portable, and the failures above show that
assumption does not survive contact with real development. The knowledge is not enforceable by
anything except the engine itself.

**Keeping the in-memory database for unit tests and the real engine only for a separate integration
suite.** Rejected because it locates the difference at exactly the boundary where these bugs live.
A query is either exercised against the engine that will run it or it is not tested.

**A shared development database.** Rejected because tests then interfere with each other and
cannot run in parallel or in isolation.

## Consequences

The test suite takes longer to start, since a container must be created. This is mitigated by
sharing one instance across the whole suite rather than creating one per class.

In exchange, a query that passes locally passes in deployment, because the same software evaluated
it both times. The verification that the deployed statistics engine produces results identical to
the local one, to full floating point precision, is a direct consequence of this decision.

An automated check additionally rejects engine-specific functions in queries, on the grounds that
this class of mistake was made repeatedly and is trivially detectable.
