# Progressive delivery

## Why automation rather than a checklist

A careful team releasing a risky change already knows the procedure. Turn it on for a small
fraction of traffic. Watch the error rate. If it holds, widen. If it does not, turn it off. The
procedure is not the hard part.

The hard part is that it depends on someone watching, and attention is the least reliable component
in any system. The watching happens on a Friday afternoon, or during a meeting, or at two in the
morning when the release was scheduled to avoid disruption. The step that gets skipped is never the
first one, when everyone is paying attention. It is the third, when the change has been fine for
two hours and confidence has replaced vigilance.

Automating the procedure removes the dependency on attention. The schedule advances whether or not
anyone is looking, and more importantly it retreats whether or not anyone is looking.

## What a schedule expresses

A rollout schedule is a sequence of stages, each pairing a traffic level with a length of time to
remain there. A change might sit at a small fraction for an hour, then a larger one for another
hour, then reach everyone.

The dwell time is the substance. Widening exposure immediately after widening it again would tell
you nothing, because problems need traffic and time to become visible. Rare failures need enough
requests to occur at all, and slow degradations such as a memory leak or a filling queue only
appear after the system has been under the new behaviour for a while. The dwell time is what
converts a sequence of numbers into an actual observation period.

Advancement is driven by a recurring check rather than by scheduled work created in advance. This
is a deliberate resilience choice. If the platform restarts, a timer that was counting down is
simply gone, whereas a recurring check reads the current state and works out what should happen
next from durable data. Recovery after a restart requires no special handling because there is
nothing to recover: the next check picks up wherever things were left.

## Guardrails, and the thing they measure against

A guardrail is a condition that must continue to hold for the rollout to continue. It names a
metric, a threshold, and whether the concern is the value rising or falling.

The direction is not redundant. An error rate is a problem when it rises. A conversion rate is a
problem when it falls. A single guardrail concept that only understood one direction would cover
half of what teams actually care about, and the half it missed is the half concerning whether the
change is doing harm that is not an outright failure.

The subtler question is what window the metric is measured over, and getting this wrong is easy.
The natural implementation measures a recent window, say the last half hour. It is also incorrect,
for a reason that only becomes visible under a specific and realistic condition.

Consider a stage with a two hour dwell time. Near the end of it, a trailing half hour window has
drifted entirely past the moment the stage began, and now covers a period during which nothing of
interest happened. A change that caused a burst of errors immediately on exposure would pass its
guardrail an hour later, because the evidence has scrolled out of view. The window has quietly
stopped measuring the thing it was created to measure.

Measuring from the moment the current stage began instead removes the problem completely. The
window always covers exactly the period during which this level of exposure has been in effect,
which is precisely the period the guardrail is asking about. It also grows as the stage progresses,
so the judgement becomes better informed with time rather than more forgetful.

This was not reasoned out in advance. It was found because tests that simulate the passage of time
in order to exercise dwell behaviour began failing in a way that made no sense, and the
investigation revealed that the window and the stage had drifted apart. The correction turned out
to be simpler than the thing it replaced, which is often the sign of a design that was slightly
wrong rather than merely incomplete.

## Retreating

When a guardrail is breached the rollout does not pause for review. It returns the flag to its
previous state immediately, records why, and marks the schedule as stopped.

Pausing for review would be the more cautious-sounding choice and it is the wrong one. The entire
value of the mechanism is that it works when nobody is watching, and a state requiring a human to
resolve it is a state that persists until a human arrives. If the automation is not trusted to act
alone, the automation is not doing anything.

The rollback carries an explanation naming the metric, the value observed, the threshold it
crossed, and the number of observations behind that judgement. The last of these matters more than
it appears. A rollback triggered by three requests is a different event from one triggered by
twelve hundred, and someone arriving afterward needs to distinguish them without reconstructing the
state of the world at the time.

## The record

Every configuration change is written to an append-only trail, whether a person or the scheduler
made it. Automated changes are attributed to the automation rather than left unattributed, so the
question of who turned something off always has an answer.

This is partly for incident review, where the useful question is usually what changed shortly
before things went wrong rather than what the state is now. It is also a straightforward
requirement in regulated environments, where a system that can change application behaviour without
a deployment is exactly the kind of system auditors ask about.

The trail also supports a quieter but persistent problem. Flags accumulate. A flag added for a
release two years ago, fully rolled out and forgotten, still sits in the configuration and still
has to be reasoned about by whoever reads it next. Because the platform records exposures, it can
identify flags nothing has evaluated recently and surface them for removal. A flag nobody has
consulted in months is either dead or a bug, and both are worth knowing about.

## Related reading

- [Architecture](architecture.md) explains why the scheduler announces changes differently.
- [Decision: guardrails measured from stage entry](decisions/0005-guardrails-measured-from-stage-entry.md)
