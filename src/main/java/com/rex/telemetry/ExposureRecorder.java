package com.rex.telemetry;

import com.rex.model.FeatureFlag;
import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes evaluation exposures.
 *
 * <p>Recording happens off the request thread, because telemetry must never be the reason a flag
 * evaluation is slow. It also runs in its own transaction: a telemetry failure should not roll back
 * the work that triggered it, and an evaluation is a read that has nothing to undo.
 */
@Component
public class ExposureRecorder {

  private static final Logger logger = LoggerFactory.getLogger(ExposureRecorder.class);

  private final MetricsRepository metricsRepository;

  public ExposureRecorder(MetricsRepository metricsRepository) {
    this.metricsRepository = metricsRepository;
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFlagExposure(
      FeatureFlag flag, String userId, boolean servedDecision, String environment) {
    try {
      Metrics exposure = new Metrics();
      exposure.setUserId(userId);
      exposure.setFeatureFlag(flag);
      exposure.setEventType(Metrics.EventType.FLAG_EXPOSURE);
      exposure.setEventName("flag_evaluated");
      exposure.setEnvironment(environment);
      exposure.setTimestamp(LocalDateTime.now());
      exposure.setServedDecision(servedDecision);
      exposure.setRolloutAtExposure(flag.getRolloutPercentage());
      metricsRepository.save(exposure);
    } catch (RuntimeException exception) {
      // Losing a telemetry row is preferable to failing the evaluation that produced it.
      logger.warn("Failed to record exposure for flag '{}'", flag.getName(), exception);
    }
  }
}
