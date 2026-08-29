package com.rex.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rex.exception.DuplicateResourceException;
import com.rex.exception.InvalidStateTransitionException;
import com.rex.model.FeatureFlag;
import com.rex.service.FeatureFlagService;
import com.rex.support.PostgresIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RolloutServiceIntegrationTest extends PostgresIntegrationTest {

  @Autowired private RolloutService rolloutService;
  @Autowired private FeatureFlagService flagService;

  private FeatureFlag newFlag() {
    return flagService.createFeatureFlag(
        "rollout_probe_" + System.nanoTime(),
        "staged rollout target",
        false,
        FeatureFlag.FlagStatus.ACTIVE,
        "production",
        0,
        "suite@rex.com");
  }

  private static List<RolloutStage> stages() {
    return List.of(
        new RolloutStage(0, 5, 60),
        new RolloutStage(1, 25, 60),
        new RolloutStage(2, 50, 60),
        new RolloutStage(3, 100, 60));
  }

  @Test
  @DisplayName("starting a rollout moves the flag to the first stage percentage immediately")
  void startEntersFirstStage() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com");

    RolloutSchedule started = rolloutService.start(schedule.getId());

    assertThat(started.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.RUNNING);
    assertThat(started.getCurrentStageIndex()).isZero();
    assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
        .isEqualTo(5);
  }

  @Test
  @DisplayName("a stage whose dwell time has not elapsed is left alone")
  void dwellNotElapsedDoesNothing() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com").getId());

    boolean advanced = rolloutService.advanceIfDue(schedule, LocalDateTime.now());

    assertThat(advanced).isFalse();
    assertThat(schedule.getCurrentStageIndex()).isZero();
  }

  @Test
  @DisplayName("a rollout advances through every stage and then completes")
  void advancesThroughAllStages() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com").getId());

    // Simulated time, so the test does not wait an hour per stage.
    LocalDateTime later = LocalDateTime.now().plusHours(2);
    int[] expected = {25, 50, 100};
    for (int percentage : expected) {
      assertThat(rolloutService.advanceIfDue(schedule, later)).isTrue();
      assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
          .isEqualTo(percentage);
      later = later.plusHours(2);
    }

    assertThat(rolloutService.advanceIfDue(schedule, later)).isTrue();
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.COMPLETED);
  }

  @Test
  @DisplayName("a completed rollout is not advanced again by a later sweep")
  void completedScheduleIsNotAdvancedAgain() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService
                .createSchedule(flag.getId(), List.of(new RolloutStage(0, 100, 1)), "s")
                .getId());

    LocalDateTime later = LocalDateTime.now().plusHours(1);
    assertThat(rolloutService.advanceIfDue(schedule, later)).isTrue();
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.COMPLETED);

    assertThat(rolloutService.advanceIfDue(schedule, later.plusHours(1)))
        .as("a completed rollout must be inert")
        .isFalse();
  }

  @Test
  @DisplayName("a paused rollout does not advance")
  void pausedScheduleDoesNotAdvance() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com").getId());
    rolloutService.pause(schedule.getId());

    assertThat(rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(5))).isFalse();
  }

  @Test
  @DisplayName("rolling back returns the flag to the previous stage percentage")
  void rollbackReturnsToLastSafePercentage() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com").getId());
    rolloutService.advanceIfDue(schedule, LocalDateTime.now().plusHours(2));

    assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
        .isEqualTo(25);

    rolloutService.rollBack(schedule, "error rate breached", LocalDateTime.now());

    assertThat(flagService.getFlagById(flag.getId()).orElseThrow().getRolloutPercentage())
        .as("reverts to the percentage that ran without a breach")
        .isEqualTo(5);
    assertThat(schedule.getStatus()).isEqualTo(RolloutSchedule.RolloutStatus.ROLLED_BACK);
    assertThat(schedule.getHaltedReason()).isEqualTo("error rate breached");
  }

  @Test
  @DisplayName(
      "stages that do not increase are rejected, since a rollout must not revoke a feature")
  void descendingStagesRejected() {
    FeatureFlag flag = newFlag();

    assertThatThrownBy(
            () ->
                rolloutService.createSchedule(
                    flag.getId(),
                    List.of(new RolloutStage(0, 50, 60), new RolloutStage(1, 25, 60)),
                    "suite@rex.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must increase");
  }

  @Test
  @DisplayName("a second schedule for the same flag is rejected")
  void oneSchedulePerFlag() {
    FeatureFlag flag = newFlag();
    rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com");

    assertThatThrownBy(() -> rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com"))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  @DisplayName("a rollout with no stages is rejected")
  void emptyScheduleRejected() {
    FeatureFlag flag = newFlag();

    assertThatThrownBy(() -> rolloutService.createSchedule(flag.getId(), List.of(), "s"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one stage");
  }

  @Test
  @DisplayName("starting an already running rollout is rejected")
  void doubleStartRejected() {
    FeatureFlag flag = newFlag();
    RolloutSchedule schedule =
        rolloutService.start(
            rolloutService.createSchedule(flag.getId(), stages(), "suite@rex.com").getId());

    assertThatThrownBy(() -> rolloutService.start(schedule.getId()))
        .isInstanceOf(InvalidStateTransitionException.class);
  }
}
