# Experiment analysis

## The mistake this exists to prevent

Two versions of a checkout button are shown to different halves of an audience. The new one
converts at twenty-four percent, the old one at twenty. The new one wins, and the team ships it.

That conclusion may be entirely wrong, and the reason it may be wrong is not a matter of
arithmetic. Any two samples drawn from populations that behave identically will still differ,
because sampling is noisy. The question is never whether the numbers differ. It is whether they
differ by more than sampling noise comfortably explains. Answering that question requires knowing
how large the samples were, and a comparison of two percentages does not carry that information.

Most homegrown experimentation stops at the percentages. This is the part the platform exists to
do properly.

## Reasoning about the difference

The approach is to assume, temporarily, that the two variants are identical and that any observed
gap is coincidence. Under that assumption it is possible to work out how large a gap would be
expected purely from noise, given the sample sizes involved. The observed gap is then expressed as
a multiple of that expected variation. A gap twice the size of what noise typically produces is
surprising; a gap a quarter of that size is unremarkable.

That multiple is converted into a probability: the chance of seeing a gap at least this large if
the two variants really were identical. When that probability falls below a threshold the team set
in advance, the assumption of identity is abandoned and the difference is called real.

The threshold is a choice about tolerance for being wrong, not a fact about the data. A conventional
setting accepts being misled roughly one time in twenty. It is stored per experiment rather than
fixed globally, because a team changing a button colour and a team changing a payment flow have
legitimately different appetites for that risk.

## Why an interval accompanies every rate

Alongside each conversion rate the platform reports a range of plausible values. A rate of twenty
percent measured over a hundred users and the same rate measured over a hundred thousand are
different claims, and presenting both as "twenty percent" hides the difference.

The method used to construct these ranges is chosen deliberately over the more familiar textbook
formula. That formula behaves badly precisely where experimentation is most common: when the rate
is near zero or near one hundred percent, or when the sample is small. It can produce ranges that
extend below zero or above one hundred, which is not merely inelegant but visibly wrong to anyone
reading the dashboard. The method used here stays inside sensible bounds under those conditions,
which matters because early results are exactly when someone is most tempted to act.

## Two conditions, not one

An experiment can produce a statistically significant result while still being worthless, and this
is the failure mode the platform guards against most carefully.

The reason is a practice known as peeking. If results are watched continuously and the experiment
is stopped the moment the threshold is crossed, then the threshold has been tested many times
rather than once. Testing repeatedly means eventually crossing it by chance. A team that checks
daily and stops at the first favourable result will declare winners regularly regardless of
whether any real difference exists. The statistics are computed correctly and the conclusion is
still nonsense.

The defence is to require two independent conditions before a winner can be declared. The
difference must be statistically significant, and the experiment must have collected the sample
size decided upon before it started. Either alone is insufficient. Significance without the sample
size is very likely to be peeking. The sample size without significance simply means the difference
being looked for was not there.

The required sample size is derived from what the team said in advance they wanted to be able to
detect. Wanting to catch smaller improvements requires more data, and the relationship is steep:
halving the effect size roughly quadruples the sample needed. Making that requirement explicit
before the experiment starts is the whole mechanism. Once the number is fixed, the temptation to
stop early has something concrete to argue with.

The platform also declines to report a verdict at all below a minimum number of exposures per
variant, returning an explicit statement of insufficient data instead. Statistical tests of this
kind rest on approximations that stop holding for very small samples, and reporting a confident
number from six observations would be worse than reporting nothing.

## Correctness that does not depend on the implementation

The arithmetic involves functions with no closed form, which are computed here through published
numerical approximations. A test that compares the implementation against itself would be
worthless.

Instead the outputs are checked against worked examples from statistical literature and against
published tables, so a regression in the arithmetic fails against an external authority rather than
against a recorded expectation that might itself have drifted. The engine also has no dependency on
storage or transport, which means it can be exercised across a wide range of inputs cheaply. This
is why it carries the strictest coverage requirement in the project: it is the component where a
silent error is least likely to be noticed and most likely to matter, because a wrong number here
looks exactly like a right one.

## What is deliberately not attempted

The platform compares two variants on a single conversion metric. It does not support multi-armed
experiments, sequential testing designs that permit valid early stopping, or corrections for
running many experiments at once.

These are all real needs at sufficient scale, and each is a substantial piece of work in its own
right. Implementing any of them badly would be worse than not offering them, because the output
would carry the same authority as the parts that are correct. The boundary is drawn where the
platform can be confident.

## Related reading

- [Deterministic assignment](assignment.md) covers how the two populations are formed.
- [Decision: two conditions before declaring a winner](decisions/0006-two-conditions-before-declaring-a-winner.md)
