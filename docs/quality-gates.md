# Quality gates

## The argument for automation over review

Every check described here could in principle be performed by a person reading a change. In
practice none of them would be, consistently, because the failures they catch are exactly the kind
that survive careful reading.

A reviewer looking at a change to a query does not usually ask whether the function it calls exists
in the database the project actually deploys against. A reviewer looking at a new method that saves
a record does not usually ask whether every other method that saves a similar record also announces
it. These are true observations that require holding the whole system in mind at once, which is
precisely what a reviewer cannot do and what an automated check does trivially.

The checks are therefore not a substitute for review. They handle the class of question that review
is bad at, so that review can spend its attention on the class of question it is good at.

## What is enforced, and why each exists

**Formatting** is applied mechanically rather than discussed. The value is not that the chosen style
is superior but that the question is closed. A diff that contains only meaningful changes is easier
to review than one where they are mixed with reindentation.

**Static analysis and null safety** operate on the observation that a large share of runtime
failures in this language come from dereferencing something absent. Treating the absence of a value
as a compile-time concern rather than a runtime surprise moves those failures from production to
the build.

**Architecture rules** encode the dependency direction described in the [architecture](architecture.md).
This check has caught the same violation three separate times, each in a different feature, each
written by someone who knew the rule. That record is the argument for it: the rule is easy to
believe in and easy to breach while concentrating on something else.

**Schema verification** compares the database schema against what the application expects and
refuses to start if they disagree. It converts a category of subtle misbehaviour into an immediate
and unmissable failure.

**Coverage floors** are enforced at two levels. The project as a whole carries a moderate
requirement, while the decision engines carry a much stricter one. The difference is intentional.
Those engines are pure computation with no external dependencies, so exercising them thoroughly is
cheap, and they are the components where a silent error is least likely to be noticed. Applying the
strict figure everywhere would push effort toward code where the additional tests would assert very
little.

**Portable query checking** rejects database functions that exist in one engine and not another.
This exists because of a specific failure described below.

**Behavioural contract checks** verify properties that ordinary tests do not naturally cover, most
notably that every operation changing a flag also announces the change. A method that saves without
announcing passes any test that reads the value back afterwards, because the value is correct. The
only symptom is a client that never finds out, which no unit test observes.

## What the gates have actually caught

The following were all found by automation or by the act of writing tests, not by review.

The database migration set declared several indexes with names that collided across tables. The
development database tolerated it; the production engine rejected the duplicates, leaving one table
with no indexes at all. It would have surfaced as gradual and unexplained slowness.

Seed data used date arithmetic and a random function specific to the development database. Neither
exists in the production engine, and the migration failed outright on first contact with it.

Two reporting queries formatted timestamps using a function that exists only in the development
database. Both threw on the deployed database, and one of them was called by the dashboard summary.
The comments above them still described them as the development-compatible versions, left over from
an earlier stage of the project. The portable query check now rejects that entire class of mistake,
and it finds six instances in the file as it stood before the fix.

Six operations changed a flag's state without announcing it. Only the ones the routes happened to
call were announcing, so nothing appeared wrong. Any future route calling one of the others would
have produced clients that silently never updated.

The interface exposed no way to move an experiment from draft into a startable state, while the
start operation refused anything not in that state. An experiment created through the interface
could therefore never be started through it. This went unnoticed because the demonstration data
sets the status directly.

Unmapped paths returned a server error rather than a not-found, and a missing or malformed request
body did the same. Both are things a caller did wrong, being reported as though the platform had
failed. The cause in both cases was a catch-all handler doing its job a little too enthusiastically.

Configuration for the deployed environment pointed the live update channel at an address one path
segment too long, so the dashboard reconnected indefinitely while appearing otherwise healthy. No
test could have caught it, because the value only takes its real form at deployment. The client
library now accepts either form.

## The pattern worth noticing

Nearly every item above shares a shape: something worked in one environment and failed in another,
or worked through one path and failed through another, and the failure was silent rather than loud.

That is the argument for this kind of investment. The failures that automation catches are rarely
dramatic. They are the ones where the system continues to appear healthy while doing the wrong
thing, which is also the category that takes longest to diagnose once it reaches production.

## Related reading

- [Architecture](architecture.md) covers the dependency rules the architecture check enforces.
- The [decision log](decisions/README.md) records why several of these gates were chosen.
