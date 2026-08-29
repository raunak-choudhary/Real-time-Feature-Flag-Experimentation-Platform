# 0006. A winner requires significance and a sample size

**Status:** Accepted

## Context

An experiment comparing two variants can produce a statistically significant result that is
nonetheless meaningless.

If results are watched continuously and the experiment is stopped the moment significance appears,
the threshold has been tested many times rather than once. Testing repeatedly means eventually
crossing it by chance. A team checking daily and stopping at the first favourable result will
declare winners regularly whether or not any real difference exists. The arithmetic is correct at
every step and the conclusion is still wrong.

This behaviour is not hypothetical. It is the default behaviour of anyone with a dashboard and an
incentive.

## Decision

Declaring a winner requires two independent conditions: the difference must be statistically
significant, and the experiment must have reached the sample size decided before it started.

Below a minimum number of exposures per variant, no verdict is offered at all and the state is
reported explicitly as insufficient data.

## Alternatives considered

**Significance alone.** Rejected for the reason above.

**A sequential testing design that permits valid early stopping.** These exist and are the
statistically proper answer to the problem. Rejected as disproportionate for this project: they
require careful implementation to be valid, and implementing one incorrectly would be worse than
not offering it, because the output would carry the same authority as the parts that are correct.

**Hiding results until the sample size is reached.** Prevents peeking by preventing observation.
Rejected because teams have legitimate reasons to watch an experiment in progress, most obviously
to notice that it is harming users and should be stopped.

## Consequences

Teams must state in advance what size of improvement they want to be able to detect, since the
required sample size derives from it. This is a real burden and it is the mechanism: once the
number is fixed, the temptation to stop early has something concrete to argue with.

Experiments take longer to conclude than they would if significance alone were sufficient. That is
the intended effect.

The relationship between detectable effect and required sample size is steep, so a team asking to
detect very small improvements will be told they need a great deal of data. This is accurate and
is better learned before the experiment than after.
