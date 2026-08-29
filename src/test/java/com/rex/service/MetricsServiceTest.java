package com.rex.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.model.Experiment;
import com.rex.model.FeatureFlag;
import com.rex.model.Metrics;
import com.rex.repository.ExperimentRepository;
import com.rex.repository.FeatureFlagRepository;
import com.rex.repository.MetricsRepository;
import com.rex.support.PostgresIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the metrics service against a real database.
 *
 * <p>These run against Postgres rather than mocks because most of what the service does is shape
 * the result of a grouped query. A mocked repository would let the test agree with an assumption
 * about the row layout that the query does not actually produce, which is the one failure mode
 * worth guarding against here.
 */
@Transactional
class MetricsServiceTest extends PostgresIntegrationTest {

  @Autowired private MetricsService metricsService;
  @Autowired private MetricsRepository metricsRepository;
  @Autowired private FeatureFlagRepository flagRepository;
  @Autowired private ExperimentRepository experimentRepository;

  private FeatureFlag flag;
  private Experiment experiment;

  @BeforeEach
  void setUp() {
    flag = flagRepository.save(new FeatureFlag("metrics_subject", "under test", "suite@rex.com"));
    experiment =
        experimentRepository.save(
            new Experiment(
                "metrics_experiment", "under test", 100, "control", "test", "suite@rex.com"));
  }

  private Metrics conversionRow(String variant, double revenue) {
    Metrics row =
        new Metrics(
            "user-" + variant + "-" + revenue, experiment, Metrics.EventType.CONVERSION, variant);
    row.setRevenue(revenue);
    row.setEventValue(revenue);
    row.setEnvironment("test");
    return row;
  }

  @Nested
  @DisplayName("event tracking")
  class EventTracking {

    @Test
    @DisplayName("a flag exposure is persisted with its context")
    void flagExposureIsPersisted() {
      Metrics saved =
          metricsService.trackFlagExposure(
              "user-1", flag, "session-9", "test", "Mozilla/5.0", "/checkout");

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.FLAG_EXPOSURE);
      assertThat(saved.getSessionId()).isEqualTo("session-9");
      assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
      assertThat(saved.getPageUrl()).isEqualTo("/checkout");
      assertThat(saved.getEventName()).isEqualTo("flag_exposure_metrics_subject");
    }

    @Test
    @DisplayName("a toggle records which direction it went")
    void toggleDirectionDeterminesEventType() {
      Metrics on = metricsService.trackFlagToggle("user-1", flag, true, "test", "operator@rex.com");
      Metrics off =
          metricsService.trackFlagToggle("user-1", flag, false, "test", "operator@rex.com");

      assertThat(on.getEventType()).isEqualTo(Metrics.EventType.FLAG_ENABLED);
      assertThat(on.getEventValue()).isEqualTo(1.0);
      assertThat(off.getEventType()).isEqualTo(Metrics.EventType.FLAG_DISABLED);
      assertThat(off.getEventValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("an error event carries its message")
    void errorEventIsPersisted() {
      Metrics saved =
          metricsService.trackError(
              "user-1", "checkout failed", "/checkout", "session-2", "test", flag);

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.ERROR);
      assertThat(saved.getErrorMessage()).isEqualTo("checkout failed");
      assertThat(saved.getFeatureFlag().getId()).isEqualTo(flag.getId());
      assertThat(metricsRepository.findById(saved.getId())).isPresent();
    }
  }

  @Nested
  @DisplayName("experiment performance summary")
  class PerformanceSummary {

    @Test
    @DisplayName("conversion rate is a percentage of that variant's own events")
    void conversionRateIsPerVariant() {
      metricsRepository.saveAll(
          List.of(
              conversionRow("control", 10.0),
              conversionRow("test", 20.0),
              conversionRow("test", 30.0)));
      metricsRepository.flush();

      Map<String, Object> summary =
          metricsService.getExperimentPerformanceSummary(experiment.getId());

      assertThat(summary).containsOnlyKeys("control", "test");
      @SuppressWarnings("unchecked")
      Map<String, Object> test = (Map<String, Object>) summary.get("test");
      assertThat(test.get("totalEvents")).isEqualTo(2L);
      assertThat(test.get("conversions")).isEqualTo(2L);
      assertThat((Double) test.get("conversionRate")).isEqualTo(100.0);
      assertThat((Double) test.get("totalRevenue")).isEqualTo(50.0);
    }

    @Test
    @DisplayName("an experiment with no metrics summarises to nothing rather than failing")
    void unknownExperimentSummarisesEmpty() {
      assertThat(metricsService.getExperimentPerformanceSummary(999_999L)).isEmpty();
    }
  }

  @Nested
  @DisplayName("flag usage summary")
  class UsageSummary {

    @Test
    @DisplayName("a flag with no events reports zeroes instead of an empty map")
    void unusedFlagReportsZeroes() {
      Map<String, Object> summary = metricsService.getFeatureFlagUsageSummary(flag.getId());

      assertThat(summary)
          .containsEntry("totalExposures", 0L)
          .containsEntry("uniqueUsers", 0L)
          .containsEntry("enabledEvents", 0L)
          .containsEntry("disabledEvents", 0L);
    }

    @Test
    @DisplayName("enabled and disabled events are counted separately")
    void togglesAreCountedByDirection() {
      metricsService.trackFlagToggle("user-1", flag, true, "test", "operator@rex.com");
      metricsService.trackFlagToggle("user-2", flag, false, "test", "operator@rex.com");
      metricsService.trackFlagExposure("user-1", flag, "s", "test", "ua", "/");
      metricsRepository.flush();

      Map<String, Object> summary = metricsService.getFeatureFlagUsageSummary(flag.getId());

      assertThat(summary.get("totalExposures")).isEqualTo(3L);
      assertThat(summary.get("uniqueUsers")).isEqualTo(2L);
      assertThat(summary.get("enabledEvents")).isEqualTo(1L);
      assertThat(summary.get("disabledEvents")).isEqualTo(1L);
    }

    @Test
    @DisplayName("the populated shape carries a flagId that the empty shape does not")
    void populatedAndEmptyShapesDiffer() {
      Map<String, Object> empty = metricsService.getFeatureFlagUsageSummary(flag.getId());
      metricsService.trackFlagExposure("user-1", flag, "s", "test", "ua", "/");
      metricsRepository.flush();
      Map<String, Object> populated = metricsService.getFeatureFlagUsageSummary(flag.getId());

      // Pinned deliberately. A caller reading flagId has to tolerate its absence, and that is only
      // discoverable from a test since both shapes are a bare Map.
      assertThat(empty).doesNotContainKey("flagId");
      assertThat(populated).containsKey("flagId");
    }
  }

  @Nested
  @DisplayName("conversion funnel summary")
  class FunnelSummary {

    @Test
    @DisplayName("events are grouped by variant then by event type")
    void funnelGroupsByVariantThenEventType() {
      Metrics exposure =
          new Metrics("user-1", experiment, Metrics.EventType.EXPERIMENT_EXPOSURE, "control");
      Metrics click = new Metrics("user-1", experiment, Metrics.EventType.CLICK, "control");
      metricsRepository.saveAll(List.of(exposure, click, conversionRow("test", 5.0)));
      metricsRepository.flush();

      Map<String, Map<String, Long>> funnel =
          metricsService.getConversionFunnelSummary(experiment.getId());

      assertThat(funnel).containsOnlyKeys("control", "test");
      assertThat(funnel.get("control"))
          .containsEntry("EXPERIMENT_EXPOSURE", 1L)
          .containsEntry("CLICK", 1L);
      assertThat(funnel.get("test")).containsEntry("CONVERSION", 1L);
    }
  }

  @Nested
  @DisplayName("queries and retention")
  class QueriesAndRetention {

    @Test
    @DisplayName("metrics are retrievable by user, type and experiment")
    void metricsAreRetrievableByTheUsualAxes() {
      metricsService.trackFlagExposure("user-axis", flag, "s", "test", "ua", "/");
      metricsRepository.saveAll(List.of(conversionRow("control", 1.0)));
      metricsRepository.flush();

      assertThat(metricsService.getMetricsByUserId("user-axis")).hasSize(1);
      assertThat(metricsService.getMetricsByEventType(Metrics.EventType.FLAG_EXPOSURE))
          .isNotEmpty();
      assertThat(metricsService.getMetricsByExperiment(experiment.getId())).hasSize(1);
      assertThat(metricsService.getMetricsByFeatureFlag(flag.getId())).hasSize(1);
      assertThat(metricsService.countByFeatureFlag(flag.getId())).isEqualTo(1L);
      assertThat(metricsService.countByExperiment(experiment.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("a batch save returns every row with an identity")
    void batchSaveAssignsIdentities() {
      List<Metrics> saved =
          metricsService.saveMetricsBatch(
              List.of(conversionRow("control", 1.0), conversionRow("test", 2.0)));

      assertThat(saved).hasSize(2).allSatisfy(row -> assertThat(row.getId()).isNotNull());
    }

    @Test
    @DisplayName("cleanup removes only rows older than the cutoff")
    void cleanupRespectsTheCutoff() {
      metricsService.trackFlagExposure("user-recent", flag, "s", "test", "ua", "/");
      metricsRepository.flush();
      long before = metricsService.countByFeatureFlag(flag.getId());

      metricsService.cleanupOldMetrics(LocalDateTime.now().minusYears(1));
      metricsRepository.flush();

      assertThat(metricsService.countByFeatureFlag(flag.getId())).isEqualTo(before);
    }
  }
}
