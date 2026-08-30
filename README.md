# REX Platform

A feature flag and experimentation service. Toggle a flag and connected clients see it in
milliseconds. Run an experiment and get a verdict that refuses to declare a winner before the data
earns one. Set a staged rollout and it advances on its own, watching error rates as it goes and
reverting itself if they spike.

[![CI](https://github.com/raunak-choudhary/Real-time-Feature-Flag-Experimentation-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/raunak-choudhary/Real-time-Feature-Flag-Experimentation-Platform/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/tests-381%20passing-brightgreen)
![Coverage](https://img.shields.io/badge/line%20coverage-79.8%25-brightgreen)
![CI checks](https://img.shields.io/badge/CI%20checks-20-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6)

**[Live dashboard](https://rex-platform-iota.vercel.app)** · **[API](https://rex-platform-api.onrender.com/swagger-ui/index.html)** · **[Documentation](docs/README.md)**

The API runs on a free instance that sleeps when idle, so the first request after a quiet period
takes about a minute to wake.

## By the numbers

Every figure below is measured, not estimated. The ones that are environment-dependent say so.

| | |
|---|---|
| **381 tests passing** | 332 backend, 49 frontend and SDK |
| **79.8% line coverage** | Enforced as a build floor at 78%, not reported after the fact |
| **96.4% and 95.6% on the decision engines** | Evaluation and statistics, under a stricter per-package rule |
| **20 independent CI checks** | A failure names the subsystem rather than the build |
| **25 REST endpoints** | Full OpenAPI coverage, plus two STOMP endpoints |
| **7 ordered migrations** | Validated against the JPA mapping at startup |
| **p95 6ms propagation** | Toggle to client receipt, measured over 50 trials, 4-core machine |
| **100,000-user uniformity test** | Chi-square on bucket distribution, below the 0.001 critical value |
| **8 targeting operators** | First-match-wins ordering proven by test |
| **~7,650 lines of production Java** | Across 77 source files and 13 packages |
| **~4,680 lines of test Java** | Across 36 test classes |

The coverage split is deliberate rather than uniform. The evaluation and statistics engines are
pure functions with no framework, clock or storage, so exercising them exhaustively is cheap and
they carry the strictest requirement. Applying the same figure everywhere would push effort toward
code where the extra tests would assert very little.

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
implementation's own output. The deployed instance returns the same values to full floating point
precision on PostgreSQL 18 as the local build does on 16.

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
- **Measurement from stage entry**, so a long dwell time cannot let a trailing window drift past
  the evidence it was created to examine.

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

## Documentation

The reasoning behind the design is written up separately, in explanation rather than walkthrough
form.

| Document | Subject |
|---|---|
| [Architecture](docs/architecture.md) | System shape, boundaries, and what crosses them |
| [Deterministic assignment](docs/assignment.md) | How a user is placed, and why the answer never changes |
| [Experiment analysis](docs/experiment-analysis.md) | How a difference is judged real |
| [Progressive delivery](docs/progressive-delivery.md) | Staged rollout and automatic rollback |
| [Real-time propagation](docs/real-time-propagation.md) | How changes reach running clients |
| [Quality gates](docs/quality-gates.md) | What the checks are for, and what they caught |
| [Decision log](docs/decisions/README.md) | Eight technical decisions and the alternatives rejected |

## Running it

Requires Java 17, Node 22, and a container runtime. Colima works; Docker Desktop is not needed.

```bash
cp .env.example .env          # set DB_PASSWORD
colima start                  # or any Docker daemon
docker compose up -d          # PostgreSQL

./mvnw spring-boot:run        # API on :8080, docs at /swagger-ui
npm ci && npm run dev --workspace @rex/dashboard   # dashboard on :3000
```

The seed data includes an experiment with a significant result and a flag partway through a
staged rollout, so a first run opens on something rather than empty panels.

## Testing and quality gates

```bash
./mvnw clean verify           # always clean: jacoco.exec accumulates across runs otherwise
npm run verify                # tsc strict, type-aware ESLint, Vitest
./mvnw test -Pperformance     # latency measurement, excluded from the default gate
```

Twenty independent CI checks run on every push, so a failure names what broke rather than
reporting a generic build error:

| Backend | Frontend |
|---|---|
| Spotless, Checkstyle, SpotBugs | `tsc --strict`, type-aware ESLint |
| ArchUnit layering rules | Vitest |
| Flyway vs entity validation | Dashboard production build |
| JUnit + JaCoCo floor | SDK isolated typecheck |
| Statistics vs worked examples | |
| WebSocket end to end | |
| Rollout, guardrails, audit | |
| Service layer | |
| Broadcast contract | |
| API status codes and errors | |
| Portable SQL, no vendor functions | |
| Container builds and starts | |

Static analysis is Error Prone with NullAway, which is the closest Java gets to `mypy --strict`.
Coverage floors are enforced, with a stricter per-package rule on the pure engines: they sit above
95% line because pure functions have no excuse for gaps.

Two of these checks exist because of specific bugs. The broadcast contract check exists because six
operations changed a flag without telling connected clients, and a method that saves without
announcing passes any test that reads the value back. The portable SQL check exists because queries
written against an in-memory database shipped to a PostgreSQL deployment and threw there.

## Deployment

The dashboard runs on Vercel, the API on Render as a container, and the database on Neon. Nothing
in the source refers to a host: the two halves find each other through environment variables, so
either can move without touching the other.

## Deliberately not built

Each of these is a decision, not an oversight.

- **No authentication.** This is an operator console, not a hosted SaaS. Auth would consume a phase
  and show nothing the rest of the project does not.
- **No horizontal scale-out.** The broker broadcasts in-process and the scheduler assumes a single
  runner. On multiple nodes the broker needs a Redis relay and the scheduler needs distributed
  locking such as ShedLock. Both are known, neither is built.
- **No streaming ingestion.** Metrics are written through JPA. Kafka would be architecture theatre
  at this volume.
- **No Bayesian analysis.** Fixed-horizon frequentist testing only, labelled as such.

The latency figure is measured on one machine and is not a production service level objective.

## Stack

**Backend.** Java 17, Spring Boot 3.4, Spring Data JPA, Hibernate, PostgreSQL, Flyway, STOMP over
WebSocket, springdoc OpenAPI, Maven.

**Frontend and client.** TypeScript 5, Next.js 16 App Router, React 19, npm workspaces, a framework
independent client library over STOMP.

**Quality.** JUnit 5, Testcontainers, ArchUnit, JaCoCo, Spotless, Checkstyle, SpotBugs, Error Prone
with NullAway, Vitest, React Testing Library, ESLint.

**Infrastructure.** Docker, GitHub Actions, Vercel, Render, Neon.

## Author

**Raunak Choudhary**

The interesting half of a feature flag system is the half most implementations skip: proving a
difference is real rather than noise, and trusting automation to roll a release back with nobody
watching. That is the half this was built for.

[Email](mailto:raunakchoudhary17@gmail.com) · [LinkedIn](https://www.linkedin.com/in/raunak-choudhary)
