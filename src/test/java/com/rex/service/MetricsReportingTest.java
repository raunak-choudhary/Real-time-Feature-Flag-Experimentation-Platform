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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the reporting half of the metrics service.
 *
 * <p>These methods assemble a map from several queries at once, so the failure mode is a missing
 * key or a row read at the wrong index rather than a wrong number. Both are silent until something
 * downstream reads the map.
 */
@Transactional
class MetricsReportingTest extends PostgresIntegrationTest {

  private static final String ENV = "reporting-test";

  @Autowired private MetricsService metricsService;
  @Autowired private MetricsRepository metricsRepository;
  @Autowired private FeatureFlagRepository flagRepository;
  @Autowired private ExperimentRepository experimentRepository;

  private FeatureFlag flag;
  private Experiment experiment;

  @BeforeEach
  void setUp() {
    flag = flagRepository.save(new FeatureFlag("reporting_flag", "d", "suite@rex.com"));
    experiment =
        experimentRepository.save(
            new Experiment("reporting_experiment", "d", 100, "control", "test", "suite@rex.com"));
  }

  @Nested
  @DisplayName("tracking the remaining event types")
  class RemainingEventTypes {

    @Test
    @DisplayName("an experiment exposure records the variant the user saw")
    void experimentExposureRecordsTheVariant() {
      Metrics saved =
          metricsService.trackExperimentExposure(
              "user-1", experiment, "test", "s", ENV, "agent", "/page");

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.EXPERIMENT_EXPOSURE);
      assertThat(saved.getVariantName()).isEqualTo("test");
    }

    @Test
    @DisplayName("an assignment records how the user was assigned")
    void assignmentRecordsTheMethod() {
      Metrics saved =
          metricsService.trackExperimentAssignment(
              "user-1", experiment, "control", "HASH_BASED", ENV);

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.EXPERIMENT_ASSIGNMENT);
      assertThat(saved.getVariantName()).isEqualTo("control");
    }

    @Test
    @DisplayName("a conversion carries its value")
    void conversionCarriesItsValue() {
      Metrics saved = metricsService.trackConversion("user-1", experiment, "test", 12.5, "s", ENV);

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.CONVERSION);
      // The value lands in conversionValue, not eventValue. The two are separate columns and a
      // caller reading the wrong one gets null rather than an error.
      assertThat(saved.getConversionValue()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("a purchase records revenue separately from the event value")
    void purchaseRecordsRevenue() {
      Metrics saved = metricsService.trackPurchase("user-1", experiment, "test", 99.0, "s", ENV);

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.PURCHASE);
      assertThat(saved.getRevenue()).isEqualTo(99.0);
    }

    @Test
    @DisplayName("a page view records where the user came from")
    void pageViewRecordsTheReferrer() {
      Metrics saved =
          metricsService.trackPageView("user-1", "/checkout", "s", ENV, "agent", "/home");

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.PAGE_VIEW);
      assertThat(saved.getPageUrl()).isEqualTo("/checkout");
    }

    @Test
    @DisplayName("a click records the element and may attach to an experiment")
    void clickRecordsTheElement() {
      Metrics saved =
          metricsService.trackClick(
              "user-1", "buy_button", "/checkout", "s", ENV, experiment, "test");

      assertThat(saved.getEventType()).isEqualTo(Metrics.EventType.CLICK);
      assertThat(saved.getVariantName()).isEqualTo("test");
    }

    @Test
    @DisplayName("a performance sample records its duration")
    void performanceSampleRecordsDuration() {
      Metrics saved =
          metricsService.trackPerformance("user-1", "page_load", 1.5, 1500L, ENV, "/checkout");

      assertThat(saved.getEventValue()).isEqualTo(1.5);
      assertThat(saved.getDurationMs()).isEqualTo(1500L);
    }
  }

  @Nested
  @DisplayName("assembled reports")
  class AssembledReports {

    @Test
    @DisplayName("real-time flag stats carry exposures, usage and recent errors")
    void realTimeFlagStatsCarryEverySection() {
      metricsService.trackFlagExposure("user-1", flag, "s", ENV, "agent", "/");
      metricsService.trackError("user-1", "boom", "/", "s", ENV, flag);
      metricsRepository.flush();

      Map<String, Object> stats = metricsService.getRealTimeFlagStats(flag.getId());

      assertThat(stats).containsOnlyKeys("totalExposures", "usage", "recentErrors");
      assertThat(stats.get("totalExposures")).isEqualTo(2L);
    }

    @Test
    @DisplayName("real-time experiment stats carry exposures, variants and recent activity")
    void realTimeExperimentStatsCarryEverySection() {
      metricsService.trackExperimentExposure("user-1", experiment, "test", "s", ENV, "agent", "/");
      metricsRepository.flush();

      Map<String, Object> stats = metricsService.getRealTimeExperimentStats(experiment.getId());

      assertThat(stats).containsOnlyKeys("totalExposures", "variants", "recentActivity");
      assertThat(stats.get("totalExposures")).isEqualTo(1L);
      // The recent activity section reads the production environment whatever environment the
      // experiment runs in, so it does not describe this experiment. Asserted as present rather
      // than as a value, because its contents have nothing to do with the id passed in.
      assertThat(stats.get("recentActivity")).isNotNull();
    }

    @Test
    @DisplayName(
        "the dashboard overview reports zeroes for a quiet environment rather than failing")
    void dashboardOverviewHandlesAQuietEnvironment() {
      Map<String, Object> overview = metricsService.getDashboardOverview("nowhere");

      assertThat(overview)
          .containsEntry("totalEvents24h", 0L)
          .containsEntry("uniqueUsers24h", 0L)
          .containsKey("topPerformingVariants")
          .containsKey("platformDistribution");
      // errorCount24h is deliberately excluded from the zero assertions. The event counts are
      // scoped to the environment asked for but the error count is not, so an overview for one
      // environment reports errors raised in all of them.
      assertThat(overview).containsKey("errorCount24h");
    }

    @Test
    @DisplayName("the dashboard overview counts events in its own environment only")
    void dashboardOverviewIsScopedToItsEnvironment() {
      metricsService.trackPageView("user-1", "/", "s", ENV, "agent", null);
      metricsService.trackPageView("user-2", "/", "s", "somewhere-else", "agent", null);
      metricsRepository.flush();

      assertThat(metricsService.getDashboardOverview(ENV).get("totalEvents24h")).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("time bounded queries")
  class TimeBoundedQueries {

    @Test
    @DisplayName("a date range excludes what falls outside it")
    void dateRangeExcludesOutsideEvents() {
      metricsService.trackPageView("user-1", "/", "s", ENV, "agent", null);
      metricsRepository.flush();

      assertThat(
              metricsService.getMetricsByDateRange(
                  LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1)))
          .isNotEmpty();
      assertThat(
              metricsService.getMetricsByDateRange(
                  LocalDateTime.now().minusYears(2), LocalDateTime.now().minusYears(1)))
          .isEmpty();
    }

    @Test
    @DisplayName("high value events are filtered by the threshold given")
    void highValueEventsRespectTheThreshold() {
      metricsService.trackPerformance("user-1", "slow_page", 9_999_999.0, 10L, ENV, "/");
      metricsRepository.flush();
      LocalDateTime since = LocalDateTime.now().minusHours(1);

      assertThat(metricsService.getHighValueEvents(9_000_000.0, since)).hasSize(1);
      assertThat(metricsService.getHighValueEvents(10_000_000.0, since)).isEmpty();
    }

    @Test
    @DisplayName("a purchase does not surface as a high value event, whatever its revenue")
    void purchasesDoNotSurfaceAsHighValueEvents() {
      metricsService.trackPurchase("user-1", experiment, "test", 9_999_999.0, "s", ENV);
      metricsRepository.flush();

      // Pinned rather than fixed. The query filters on eventValue while a purchase records revenue
      // and conversionValue, so the highest value events in the system are exactly the ones this
      // never returns. Changing the column the query reads would alter what existing callers get.
      assertThat(metricsService.getHighValueEvents(9_000_000.0, LocalDateTime.now().minusHours(1)))
          .isEmpty();
    }

    @Test
    @DisplayName("the aggregate query methods answer without error on an empty window")
    void aggregateQueriesAnswerOnAnEmptyWindow() {
      LocalDateTime longAgo = LocalDateTime.now().minusYears(5);

      assertThat(metricsService.getDailyMetrics(longAgo)).isNotNull();
      assertThat(metricsService.getUserEngagement(longAgo, 1L)).isNotNull();
      assertThat(metricsService.getPlatformDistribution(longAgo)).isNotNull();
      assertThat(metricsService.getRevenueMetrics()).isNotNull();
      assertThat(metricsService.getTopPerformingVariants(1L)).isNotNull();
      assertThat(metricsService.getAllMetrics()).isNotNull();
      assertThat(metricsService.countByEventType(Metrics.EventType.PAGE_VIEW)).isNotNegative();
    }
  }
}
