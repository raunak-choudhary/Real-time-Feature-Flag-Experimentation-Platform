package com.rex.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.model.FeatureFlag;
import com.rex.repository.FeatureFlagRepository;
import com.rex.repository.MetricsRepository;
import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises stale flag detection.
 *
 * <p>The interesting case is a flag nobody has ever evaluated. It has no last seen timestamp at
 * all, so an implementation that only looks at the gap since the last exposure would skip exactly
 * the flags most likely to be dead.
 */
@Transactional
class StaleFlagServiceTest extends PostgresIntegrationTest {

  @Autowired private StaleFlagService staleFlagService;
  @Autowired private MetricsService metricsService;
  @Autowired private FeatureFlagRepository flagRepository;
  @Autowired private MetricsRepository metricsRepository;

  private FeatureFlag flag(String name) {
    FeatureFlag created = new FeatureFlag(name, "d", "suite@rex.com");
    created.setEnvironment("stale-test");
    return flagRepository.saveAndFlush(created);
  }

  private boolean reportedStale(String name, int days) {
    return staleFlagService.findStale(days).stream().anyMatch(s -> s.name().equals(name));
  }

  @Test
  @DisplayName("a flag nobody has ever evaluated is reported")
  void neverEvaluatedFlagIsReported() {
    flag("stale_never_used");

    assertThat(reportedStale("stale_never_used", 30)).isTrue();
  }

  @Test
  @DisplayName("a flag evaluated just now is not reported")
  void recentlyEvaluatedFlagIsNotReported() {
    FeatureFlag subject = flag("stale_in_use");
    metricsService.trackFlagExposure("user-1", subject, "s", "stale-test", "agent", "/");
    metricsRepository.flush();

    assertThat(reportedStale("stale_in_use", 30)).isFalse();
  }

  @Test
  @DisplayName("a zero day window reports even a flag evaluated moments ago")
  void zeroDayWindowReportsEverything() {
    FeatureFlag subject = flag("stale_zero_window");
    metricsService.trackFlagExposure("user-1", subject, "s", "stale-test", "agent", "/");
    metricsRepository.flush();

    assertThat(reportedStale("stale_zero_window", 0)).isTrue();
  }

  @Test
  @DisplayName("an archived flag is left out, since it is already retired")
  void archivedFlagIsExcluded() {
    FeatureFlag subject = flag("stale_archived");
    subject.archive();
    flagRepository.saveAndFlush(subject);

    assertThat(reportedStale("stale_archived", 30)).isFalse();
  }

  @Test
  @DisplayName("a report carries the state needed to decide whether removing it is safe")
  void reportCarriesTheDecidingState() {
    FeatureFlag subject = flag("stale_detail");
    subject.setRolloutPercentage(25);
    flagRepository.saveAndFlush(subject);

    assertThat(staleFlagService.findStale(30))
        .filteredOn(s -> s.name().equals("stale_detail"))
        .singleElement()
        .satisfies(
            s -> {
              assertThat(s.id()).isEqualTo(subject.getId());
              assertThat(s.environment()).isEqualTo("stale-test");
              assertThat(s.rolloutPercentage()).isEqualTo(25);
              assertThat(s.lastEvaluatedAt()).isNull();
              assertThat(s.daysSinceLastEvaluation()).isNotNegative();
            });
  }
}
