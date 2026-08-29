package com.rex.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.model.FeatureFlag;
import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import com.rex.service.FeatureFlagService;
import com.rex.support.PostgresIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** The flagship behaviour: a bad release halts and reverts itself. */
@Transactional
class AutomaticRollbackIntegrationTest extends PostgresIntegrationTest {

  private static final List<Guardrail> ERROR_GUARDRAIL =
      List.of(
          new Guardrail(
              Guardrail.GuardrailMetric.ERROR_RATE, 0.02, Guardrail.Comparison.ABOVE, 100));

  @Autowired private RolloutService rolloutService;
  @Autowired private FeatureFlagService flagService;
  @Autowired private MetricsRepository metricsRepository;

  private FeatureFlag newFlag() {
    return flagService.createFeatureFlag(
        "guardrail_probe_" + System.nanoTime(),
        "guarded rollout target",
        false,
        FeatureFlag.FlagStatus.ACTIVE,
        "production",
        0,
        "suite@rex.com");
  }

  private RolloutSchedule startedRollout(FeatureFlag flag) {
    RolloutSchedule schedule =
        rolloutService.createSchedule(
            flag.getId(),
            List.of(
                new RolloutStage(0, 5, 60),
                new RolloutStage(1, 25, 60),
                new RolloutStage(2, 100, 60)),
            "suite@rex.com");
    return rolloutService.start(schedule.getId());
  }

  /** Records exposures that were served the flag on, which is the cohort a guardrail watches. */
  private void seedExposures(FeatureFlag flag, int count, boolean servedDecision) {
    for (int i = 0; i < count; i++) {
      Metrics exposure = new Metrics();
      exposure.setUserId("guard_user_" + i + "_" + System.nanoTime());
      exposure.setFeatureFlag(flag);
      exposure.setEventType(Metrics.EventType.FLAG_EXPOSURE);
      exposure.setEnvironment("production");
      exposure.setTimestamp(LocalDateTime.now());
      exposure.setServedDecision(servedDecision);
      exposure.setRolloutAtExposure(flag.getRolloutPercentage());
      metricsRepository.save(exposure);
    }
  }

  private void seedErrors(FeatureFlag flag, int count) {
    for (int i = 0; i < count; i++) {
      Metrics error = new Metrics();
      error.setUserId("guard_error_" + i + "_" + System.nanoTime());
      error.setFeatureFlag(flag);
      error.setEventType(Metrics.EventType.ERROR);
      error.setEnvironment("production");
      error.setTimestamp(LocalDateTime.now());
      metricsRepository.save(error);
    }
  }

  @Test
  @DisplayName("a healthy rollout advances normally")
  void healthyRolloutAdvances() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule = startedRollout(flag);
    seedExposures(flag, 200, true);
    seedErrors(flag, 1);

    boolean advanced =
        rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2), ERROR_GUARDRAIL);

    assertThat(advanced).isTrue();
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.RUNNING);
    assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
        .isEqualTo(25);
  }

  @Test
  @DisplayName("an error rate breach halts the rollout and reverts to the last safe percentage")
  void breachTriggersRollback() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule = startedRollout(flag);
    seedExposures(flag, 200, true);
    seedErrors(flag, 40); // 20 percent, well past the 2 percent threshold

    rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2), ERROR_GUARDRAIL);

    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.ROLLED_BACK);
    assertThat(schedule.getHaltedReason()).contains("ERROR_RATE");
    assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
        .as("reverted to the percentage that ran without a breach")
        .isZero();
  }

  @Test
  @DisplayName("a breach below the minimum observation count does not trigger a rollback")
  void breachBelowMinimumObservationsIsIgnored() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule = startedRollout(flag);
    seedExposures(flag, 10, true);
    seedErrors(flag, 10); // a 100 percent error rate, on ten users

    boolean advanced =
        rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2), ERROR_GUARDRAIL);

    assertThat(advanced).as("held rather than advanced").isFalse();
    assertThat(schedule.getStatus())
        .as("one early error must not roll back a rollout")
        .isEqualTo(RolloutSchedule.RolloutStatus.RUNNING);
  }

  @Test
  @DisplayName("errors among users who never saw the flag do not count against the rollout")
  void unexposedCohortDoesNotCauseFalseRollback() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule = startedRollout(flag);
    // A large unexposed population, plus a healthy exposed one.
    seedExposures(flag, 500, false);
    seedExposures(flag, 200, true);
    seedErrors(flag, 2); // one percent of the exposed cohort

    boolean advanced =
        rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2), ERROR_GUARDRAIL);

    assertThat(advanced).as("the denominator must be the exposed cohort, not everyone").isTrue();
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.RUNNING);
  }

  @Test
  @DisplayName("a rollout with no guardrails advances without measuring anything")
  void noGuardrailsMeansNoChecks() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule = startedRollout(flag);
    seedErrors(flag, 1_000);

    boolean advanced = rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2));

    assertThat(advanced).isTrue();
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.RUNNING);
  }
}
