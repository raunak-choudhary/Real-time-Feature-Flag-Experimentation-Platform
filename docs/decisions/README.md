# Decision log

Each record here covers one technical decision: what was chosen, what was rejected, and why.

Records are written once and not revised. A decision that no longer holds is superseded by a new
record that references it, so the reasoning at the time remains legible rather than being quietly
overwritten by the reasoning that replaced it. Knowing that an approach was considered and rejected
is often more useful than knowing what was adopted, because it stops the same ground being covered
twice.

| Record | Decision |
|---|---|
| [0001](0001-murmurhash3-for-bucket-assignment.md) | Bucket assignment uses a distribution-oriented hash |
| [0002](0002-basis-points-instead-of-percentages.md) | Rollout is expressed in hundredths of a percent |
| [0003](0003-postgres-in-every-environment.md) | The same database engine runs in every environment |
| [0004](0004-events-between-service-and-transport.md) | Services announce changes rather than transmitting them |
| [0005](0005-guardrails-measured-from-stage-entry.md) | Guardrails measure from stage entry, not a trailing window |
| [0006](0006-two-conditions-before-declaring-a-winner.md) | A winner requires significance and a sample size |
| [0007](0007-architecture-rules-are-enforced.md) | Layering is enforced by a test, not by convention |
| [0008](0008-refetch-when-a-flag-is-enabled.md) | Clients refetch when a flag is enabled, not when disabled |
