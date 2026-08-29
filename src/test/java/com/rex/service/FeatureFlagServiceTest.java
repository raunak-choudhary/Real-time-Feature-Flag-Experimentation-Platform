package com.rex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rex.exception.DuplicateResourceException;
import com.rex.model.FeatureFlag;
import com.rex.repository.FeatureFlagRepository;
import com.rex.support.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the flag service, which owns creation rules, toggling and rollout arithmetic. */
@Transactional
class FeatureFlagServiceTest extends PostgresIntegrationTest {

  private static final String ENV = "service-test";

  @Autowired private FeatureFlagService flagService;
  @Autowired private FeatureFlagRepository flagRepository;

  private FeatureFlag newFlag(String name) {
    return flagService.createFeatureFlag(name, "created by the suite", ENV, "suite@rex.com");
  }

  @Nested
  @DisplayName("creation")
  class Creation {

    @Test
    @DisplayName("a new flag starts off, inactive and at zero rollout")
    void newFlagStartsClosed() {
      FeatureFlag flag = newFlag("safe_default");

      assertThat(flag.getId()).isNotNull();
      assertThat(flag.getEnabled()).isFalse();
      assertThat(flag.getStatus()).isEqualTo(FeatureFlag.FlagStatus.INACTIVE);
      assertThat(flag.getRolloutPercentage()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "has spaces", "has.dots", "has/slash", ""})
    @DisplayName("an unusable name is rejected")
    void invalidNamesAreRejected(String name) {
      assertThatThrownBy(() -> newFlag(name)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a duplicate name in the same environment is refused")
    void duplicateNameInSameEnvironmentIsRefused() {
      newFlag("duplicate_subject");

      assertThatThrownBy(() -> newFlag("duplicate_subject"))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("a rollout outside zero to one hundred is refused")
    void rolloutOutsideRangeIsRefused() {
      assertThatThrownBy(
              () ->
                  flagService.createFeatureFlag(
                      "out_of_range",
                      "d",
                      true,
                      FeatureFlag.FlagStatus.ACTIVE,
                      ENV,
                      101,
                      "suite@rex.com"))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("state changes")
  class StateChanges {

    @Test
    @DisplayName("toggle flips the flag and toggling twice returns it")
    void toggleIsItsOwnInverse() {
      FeatureFlag flag = newFlag("toggle_subject");

      assertThat(flagService.toggleFlag(flag.getId()).getEnabled()).isTrue();
      assertThat(flagService.toggleFlag(flag.getId()).getEnabled()).isFalse();
    }

    @Test
    @DisplayName("enable and disable are reachable by name as well as id")
    void enableAndDisableByName() {
      newFlag("named_subject");

      assertThat(flagService.enableFlagByName("named_subject").getEnabled()).isTrue();
      assertThat(flagService.disableFlagByName("named_subject").getEnabled()).isFalse();
    }

    @Test
    @DisplayName("deleting archives rather than removes, so the history survives")
    void deleteArchives() {
      FeatureFlag flag = newFlag("archive_subject");

      flagService.deleteFeatureFlag(flag.getId());

      FeatureFlag archived = flagRepository.findById(flag.getId()).orElseThrow();
      assertThat(archived.getStatus()).isEqualTo(FeatureFlag.FlagStatus.ARCHIVED);
      assertThat(archived.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("acting on a flag that does not exist is rejected, not silently ignored")
    void unknownIdIsRejected() {
      assertThatThrownBy(() -> flagService.toggleFlag(999_999L))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> flagService.deleteFeatureFlag(999_999L))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("rollout arithmetic")
  class RolloutArithmetic {

    @Test
    @DisplayName("an increase stops at one hundred rather than overshooting")
    void increaseClampsAtOneHundred() {
      FeatureFlag flag = newFlag("clamp_subject");
      flagService.updateRolloutPercentage(flag.getId(), 90);

      FeatureFlag raised = flagService.increaseRollout(flag.getId(), 25);

      assertThat(raised.getRolloutPercentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("an increase from an unset rollout treats it as zero")
    void increaseFromNullTreatsItAsZero() {
      FeatureFlag flag = newFlag("null_rollout_subject");
      flag.setRolloutPercentage(null);
      flagRepository.saveAndFlush(flag);

      assertThat(flagService.increaseRollout(flag.getId(), 30).getRolloutPercentage())
          .isEqualTo(30);
    }

    @Test
    @DisplayName("a rollout update outside the range is refused before anything is written")
    void invalidRolloutLeavesTheFlagUntouched() {
      FeatureFlag flag = newFlag("guard_subject");
      flagService.updateRolloutPercentage(flag.getId(), 40);

      assertThatThrownBy(() -> flagService.updateRolloutPercentage(flag.getId(), -1))
          .isInstanceOf(IllegalArgumentException.class);

      assertThat(flagRepository.findById(flag.getId()).orElseThrow().getRolloutPercentage())
          .isEqualTo(40);
    }
  }

  @Nested
  @DisplayName("evaluation by user")
  class EvaluationByUser {

    private FeatureFlag activeFlag(String name, int rollout) {
      FeatureFlag flag =
          flagService.createFeatureFlag(
              name, "d", true, FeatureFlag.FlagStatus.ACTIVE, ENV, rollout, "suite@rex.com");
      flagRepository.flush();
      return flag;
    }

    @Test
    @DisplayName("an unknown flag is off rather than an error")
    void unknownFlagIsOff() {
      assertThat(flagService.isFlagEnabledForUser("no_such_flag", "user-1", ENV)).isFalse();
    }

    @Test
    @DisplayName("a flag in another environment is off")
    void environmentMismatchIsOff() {
      activeFlag("env_subject", 100);

      assertThat(flagService.isFlagEnabledForUser("env_subject", "user-1", "somewhere-else"))
          .isFalse();
    }

    @Test
    @DisplayName("zero rollout is off for everyone and one hundred is on for everyone")
    void rolloutBoundsAreAbsolute() {
      activeFlag("nobody_subject", 0);
      activeFlag("everybody_subject", 100);

      for (int i = 0; i < 25; i++) {
        String user = "user-" + i;
        assertThat(flagService.isFlagEnabledForUser("nobody_subject", user, ENV)).isFalse();
        assertThat(flagService.isFlagEnabledForUser("everybody_subject", user, ENV)).isTrue();
      }
    }

    @Test
    @DisplayName("an inactive flag is off even at full rollout")
    void inactiveFlagIsOff() {
      flagService.createFeatureFlag(
          "inactive_subject",
          "d",
          true,
          FeatureFlag.FlagStatus.INACTIVE,
          ENV,
          100,
          "suite@rex.com");
      flagRepository.flush();

      assertThat(flagService.isFlagEnabledForUser("inactive_subject", "user-1", ENV)).isFalse();
    }

    @Test
    @DisplayName("the same user gets the same answer every time")
    void evaluationIsDeterministic() {
      activeFlag("determinism_subject", 50);

      boolean first = flagService.isFlagEnabledForUser("determinism_subject", "user-stable", ENV);
      for (int i = 0; i < 20; i++) {
        assertThat(flagService.isFlagEnabledForUser("determinism_subject", "user-stable", ENV))
            .isEqualTo(first);
      }
    }

    @Test
    @DisplayName("a partial rollout admits roughly its share of users")
    void partialRolloutAdmitsRoughlyItsShare() {
      activeFlag("share_subject", 30);

      long admitted =
          java.util.stream.IntStream.range(0, 2000)
              .filter(i -> flagService.isFlagEnabledForUser("share_subject", "user-" + i, ENV))
              .count();

      // Three standard deviations for n=2000 at p=0.30 is about 6 points, so this band catches a
      // broken hash without failing on ordinary sampling noise.
      assertThat(admitted / 2000.0 * 100).isBetween(24.0, 36.0);
    }
  }

  @Nested
  @DisplayName("bulk operations")
  class BulkOperations {

    @Test
    @DisplayName("enabling and disabling in bulk applies to every flag named")
    void bulkEnableAndDisable() {
      List<Long> ids =
          List.of(
              newFlag("bulk_one").getId(),
              newFlag("bulk_two").getId(),
              newFlag("bulk_three").getId());

      assertThat(flagService.enableFlags(ids))
          .hasSize(3)
          .allSatisfy(f -> assertThat(f.getEnabled()).isTrue());
      assertThat(flagService.disableFlags(ids))
          .hasSize(3)
          .allSatisfy(f -> assertThat(f.getEnabled()).isFalse());
    }

    @Test
    @DisplayName("a bulk rollout change applies one percentage to every flag named")
    void bulkRolloutChange() {
      List<Long> ids =
          List.of(newFlag("bulk_rollout_one").getId(), newFlag("bulk_rollout_two").getId());

      assertThat(flagService.updateRolloutForFlags(ids, 65))
          .allSatisfy(f -> assertThat(f.getRolloutPercentage()).isEqualTo(65));
    }
  }
}
