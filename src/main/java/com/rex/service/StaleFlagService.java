package com.rex.service;

import com.rex.model.FeatureFlag;
import com.rex.model.Metrics;
import com.rex.repository.FeatureFlagRepository;
import com.rex.repository.MetricsRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds flags nobody is evaluating any more.
 *
 * <p>A flag whose code path is long gone still sits in the configuration, still gets read on every
 * bootstrap, and still has to be reasoned about by whoever comes next. Surfacing them is what stops
 * the platform accumulating permanent temporary flags.
 */
@Service
@Transactional(readOnly = true)
public class StaleFlagService {

  private final FeatureFlagRepository flagRepository;
  private final MetricsRepository metricsRepository;

  public StaleFlagService(
      FeatureFlagRepository flagRepository, MetricsRepository metricsRepository) {
    this.flagRepository = flagRepository;
    this.metricsRepository = metricsRepository;
  }

  public List<StaleFlag> findStale(int days) {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

    return flagRepository.findAll().stream()
        .filter(flag -> flag.getStatus() != FeatureFlag.FlagStatus.ARCHIVED)
        .map(flag -> assess(flag, cutoff))
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private StaleFlag assess(FeatureFlag flag, LocalDateTime cutoff) {
    LocalDateTime lastSeen =
        metricsRepository.findLastEventTimestamp(flag.getId(), Metrics.EventType.FLAG_EXPOSURE);

    if (lastSeen != null && lastSeen.isAfter(cutoff)) {
      return null;
    }

    long days =
        lastSeen == null
            ? Duration.between(flag.getCreatedAt(), LocalDateTime.now()).toDays()
            : Duration.between(lastSeen, LocalDateTime.now()).toDays();

    return new StaleFlag(
        flag.getId(),
        flag.getName(),
        flag.getEnvironment(),
        Boolean.TRUE.equals(flag.getEnabled()),
        flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0,
        lastSeen,
        days);
  }
}
