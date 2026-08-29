package com.rex.rollout;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives every running rollout forward on a fixed tick.
 *
 * <p>Clock driven rather than timer driven: each sweep asks whether a stage's dwell time has
 * elapsed, so a restart resumes correctly instead of losing an in flight timer.
 *
 * <p>Single instance only. On more than one node every instance would sweep the same schedules
 * concurrently and advance a stage several times. Distributed locking, ShedLock or equivalent, is
 * the known fix and is recorded as a non goal rather than built.
 */
@Component
public class RolloutScheduler {

  private static final Logger logger = LoggerFactory.getLogger(RolloutScheduler.class);

  /**
   * Guardrails applied to every automated rollout.
   *
   * <p>Deliberately conservative defaults. A per flag configuration is the obvious extension, but a
   * rollout with no guardrails at all is the thing worth avoiding.
   */
  private static final List<Guardrail> DEFAULT_GUARDRAILS =
      List.of(
          new Guardrail(Guardrail.GuardrailMetric.ERROR_RATE, 0.02, Guardrail.Comparison.ABOVE),
          new Guardrail(
              Guardrail.GuardrailMetric.AVERAGE_LOAD_TIME_MS, 800.0, Guardrail.Comparison.ABOVE));

  private final RolloutService rolloutService;

  public RolloutScheduler(RolloutService rolloutService) {
    this.rolloutService = rolloutService;
  }

  @Scheduled(fixedDelayString = "${rex.rollout.sweep-interval-ms:30000}")
  public void sweep() {
    LocalDateTime now = LocalDateTime.now();

    for (RolloutSchedule schedule : rolloutService.running()) {
      try {
        rolloutService.advanceIfDue(schedule, now, DEFAULT_GUARDRAILS);
      } catch (RuntimeException exception) {
        // One broken schedule must not stop the others being advanced.
        logger.error("Rollout {} failed to advance", schedule.getId(), exception);
      }
    }
  }
}
