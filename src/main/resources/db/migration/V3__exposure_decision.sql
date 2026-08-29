-- Records what decision an evaluation actually served.
--
-- Guardrails in the automated rollout compare the exposed cohort against the unexposed one. Without
-- knowing which users saw the new behaviour, an error rate breach cannot be attributed to the
-- rollout rather than to background noise, so this column is what makes automatic rollback possible.

ALTER TABLE metrics ADD COLUMN served_decision boolean;
ALTER TABLE metrics ADD COLUMN rollout_at_exposure integer;

COMMENT ON COLUMN metrics.served_decision IS
  'Whether the user was served the flag on or off at the moment of exposure';
COMMENT ON COLUMN metrics.rollout_at_exposure IS
  'The rollout percentage in force when this exposure was recorded';

-- The guardrail sweep filters by event type, flag and time window on every tick, so it needs a
-- composite index rather than relying on the single column indexes already present.
CREATE INDEX idx_metrics_guardrail_window
  ON metrics (feature_flag_id, event_type, "timestamp")
  WHERE feature_flag_id IS NOT NULL;
