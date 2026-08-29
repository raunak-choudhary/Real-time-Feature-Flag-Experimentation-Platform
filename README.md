# REX Platform

A feature flag and experimentation service. Toggle a flag and connected clients see it in
milliseconds. Run an experiment and get a verdict that refuses to declare a winner before the data
earns one. Set a staged rollout and it advances on its own, watching error rates as it goes and
reverting itself if they spike.

[![CI](https://github.com/raunak-choudhary/Real-time-Feature-Flag-Experimentation-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/raunak-choudhary/Real-time-Feature-Flag-Experimentation-Platform/actions/workflows/ci.yml)

## What makes this different from a flag CRUD app

Feature flag services are a crowded category. Three things here are done carefully rather than
quickly.

**Bucketing is provably correct.** User assignment uses MurmurHash3 over `flagKey:userId`, not
`String.hashCode`. Two reasons the JDK hash was unusable: it has poor avalanche behaviour, so
sequential user IDs land in adjacent buckets and skew a rollout badly; and `Math.abs(Integer.MIN_VALUE)`
returns a negative number, so one unlucky ID produced a negative bucket index. The test suite
asserts determinism across 1,000 calls, chi-square uniformity over 100,000 users below the 0.001
critical value, independence across experiments, and monotonicity so raising a rollout never drops
a user who already had the feature.

**Real-time means measured, not claimed.** Changes propagate over STOMP to subscribed clients.
Measured p50 4ms, p95 6ms, max 88ms over 50 trials on a 4-core machine. That number comes from a
test that toggles a flag and times a real subscriber receiving it.

**Statistics are gated, not just computed.** A two-proportion z-test with Wilson score intervals,
and a sample-size gate that reports *inconclusive* when an experiment has not collected the data
it planned for, even when the raw p-value looks significant. Stopping the moment a p-value dips
below a threshold inflates the false positive rate well past the nominal 5%, and that guard is the
difference between an experimentation tool and a random number generator with a dashboard.

Every statistical expectation in the test suite is a hand-derived worked example. `200/1000` against
`240/1000` gives z = 2.1592 and p = 0.0308, checked against the formula rather than against the
implementation's own output.

## Automated rollout with guardrails

The part that does something a person would otherwise sit and watch.

A rollout holds a flag at each percentage for a dwell time, then advances. Before each advance it
measures guardrail metrics against **the exposed cohort only**, filtering on the decision actually
served. A breach halts the rollout, reverts to the last percentage that ran cleanly, broadcasts
the revert, and writes an audit row attributed to the scheduler rather than a person.

Three details that stop it misfiring:

- A **minimum observation count**, so one early error at 100% error rate on ten users cannot roll
  back a healthy release.
- **Exposed-cohort filtering**, so a pre-existing background error rate is not mistaken for the
  rollout causing harm.
- **Unreadable metrics hold rather than allow**, so a monitoring outage cannot quietly turn a
  guarded rollout into an unguarded one.

## Architecture

```
Dashboard (Next.js)          SDK (TypeScript)
       |                            |
       | REST bootstrap             | local cache, synchronous reads
       +------------+---------------+
                    |
              Spring Boot API
       +------------+------------+-------------------+
       |            |            |                   |
  Evaluation   Statistics    Rollout scheduler   STOMP broker
  engine       engine        + guardrails             |
       |            |            |                   | push
       +------------+------------+-------------------+
                    |
               PostgreSQL (Flyway)
```

The evaluation and statistics engines are pure: no Spring, no repository, no clock. An ArchUnit
test fails the build if either ever imports a framework class. That is what makes the correctness
properties testable without a database.

## Running it

Requires Java 17, Node 22, and a container runtime. Colima works; Docker Desktop is not needed.

```bash
cp .env.example .env          # set DB_PASSWORD
colima start                  # or any Docker daemon
docker compose up -d          # PostgreSQL

./mvnw spring-boot:run        # API on :8080, docs at /swagger-ui
npm ci && npm run dev --workspace @rex/dashboard   # dashboard on :3000
```

The demo seed produces an experiment with a real, significant result, so the dashboard opens on
something rather than empty charts.

## Testing and quality gates

```bash
./mvnw clean verify           # always clean: jacoco.exec accumulates across runs otherwise
npm run verify                # tsc strict, type-aware ESLint, Vitest
./mvnw test -Pperformance     # latency measurement, excluded from the default gate
```

**234 backend tests, 44 frontend tests.** Sixteen independent CI checks, so a failure names what
broke rather than reporting a generic build error:

| Backend | Frontend |
|---|---|
| Spotless, Checkstyle, SpotBugs | `tsc --strict`, type-aware ESLint |
| ArchUnit layering rules | Vitest |
| Flyway vs entity validation | Dashboard production build |
| JUnit + JaCoCo floor | SDK isolated typecheck |
| Statistics vs worked examples | |
| WebSocket end to end | |
| Rollout, guardrails, audit | |
| Container builds and starts | |

Static analysis is Error Prone with NullAway, which is the closest Java gets to `mypy --strict`.
Coverage floors are enforced, with a stricter per-package rule on the pure engines: they sit above
95% line because pure functions have no excuse for gaps.

## Deliberately not built

Each of these is a decision, not an oversight.

- **No authentication.** This is an operator console, not a hosted SaaS. Auth would consume a phase
  and demonstrate nothing the rest of the project does not.
- **No horizontal scale-out.** The broker broadcasts in-process and the scheduler assumes a single
  runner. On multiple nodes the broker needs a Redis relay and the scheduler needs distributed
  locking such as ShedLock. Both are known, neither is built.
- **No streaming ingestion.** Metrics are written through JPA. Kafka would be architecture theatre
  at this volume.
- **No Bayesian analysis.** Fixed-horizon frequentist testing only, labelled as such.

## Honest limitations

The service layer inherited from the original build is the least tested part of the codebase.
Overall line coverage is 54%, and the gap is concentrated there rather than spread evenly: the
packages written during this work sit between 87% and 96%.

The latency figure is measured on one machine and is not a production SLA.

## Stack

Java 17, Spring Boot 3.4, PostgreSQL 16, Flyway, STOMP over WebSocket, springdoc OpenAPI.
TypeScript 5, Next.js 16 App Router, React 19, Vitest. Maven, npm workspaces, Docker, GitHub Actions.
