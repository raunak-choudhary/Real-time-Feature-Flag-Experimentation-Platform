package com.rex.rollout;

import java.time.LocalDateTime;
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

  private final RolloutService rolloutService;

  public RolloutScheduler(RolloutService rolloutService) {
    this.rolloutService = rolloutService;
  }

  @Scheduled(fixedDelayString = "${rex.rollout.sweep-interval-ms:30000}")
  public void sweep() {
    LocalDateTime now = LocalDateTime.now();

    for (RolloutSchedule schedule : rolloutService.running()) {
      try {
        rolloutService.advanceIfDue(schedule, now);
      } catch (RuntimeException exception) {
        // One broken schedule must not stop the others being advanced.
        logger.error("Rollout {} failed to advance", schedule.getId(), exception);
      }
    }
  }
}
