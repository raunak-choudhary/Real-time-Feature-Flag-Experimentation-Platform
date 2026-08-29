package com.rex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import com.rex.repository.ExperimentRepository;
import com.rex.support.PostgresIntegrationTest;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the experiment service, which owns the lifecycle and the assignment of users to
 * variants.
 *
 * <p>The entity carries its own constraints, so two branches of the service's readiness check
 * cannot be reached through normal persistence. The test asserts where the refusal actually happens
 * rather than contriving a way past the entity to reach the second guard.
 *
 * <p>The lifecycle matters more than it looks. An experiment that accepts assignments before it
 * starts, or keeps accepting them after it stops, produces a result computed over a population that
 * was never actually exposed to the thing being measured.
 */
@Transactional
class ExperimentServiceTest extends PostgresIntegrationTest {

  private static final String ENV = "experiment-test";

  @Autowired private ExperimentService experimentService;
  @Autowired private ExperimentRepository experimentRepository;
  @Autowired private Validator validator;

  private Experiment draft(String name, int trafficPercentage) {
    return experimentService.createExperiment(
        name,
        "created by the suite",
        "the test variant converts better",
        "control",
        "test",
        trafficPercentage,
        ENV,
        "suite@rex.com");
  }

  private Experiment running(String name, int trafficPercentage) {
    Experiment experiment = draft(name, trafficPercentage);
    experimentService.markExperimentReady(experiment.getId());
    return experimentService.startExperiment(experiment.getId());
  }

  @Nested
  @DisplayName("lifecycle")
  class Lifecycle {

    @Test
    @DisplayName("a new experiment starts as a draft")
    void newExperimentIsADraft() {
      assertThat(draft("lifecycle_draft", 100).getStatus())
          .isEqualTo(Experiment.ExperimentStatus.DRAFT);
    }

    @Test
    @DisplayName("draft to ready to running is the accepted path")
    void theAcceptedPathIsDraftReadyRunning() {
      Experiment experiment = draft("lifecycle_path", 100);

      assertThat(experimentService.markExperimentReady(experiment.getId()).getStatus())
          .isEqualTo(Experiment.ExperimentStatus.READY);
      assertThat(experimentService.startExperiment(experiment.getId()).getStatus())
          .isEqualTo(Experiment.ExperimentStatus.RUNNING);
    }

    @Test
    @DisplayName("an experiment cannot be started straight out of draft")
    void draftCannotStartWithoutBeingMarkedReady() {
      Experiment experiment = draft("lifecycle_skip", 100);

      assertThatThrownBy(() -> experimentService.startExperiment(experiment.getId()))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("only a draft can be marked ready, so ready is not reachable twice")
    void readyIsNotReachableTwice() {
      Experiment experiment = draft("lifecycle_twice", 100);
      experimentService.markExperimentReady(experiment.getId());

      assertThatThrownBy(() -> experimentService.markExperimentReady(experiment.getId()))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("an unusable configuration is refused at the entity, before readiness is asked")
    void invalidConfigurationIsRefusedAtTheEntity() {
      Experiment noTraffic =
          new Experiment("cfg_traffic", "d", 0, "control", "test", "suite@rex.com");
      Experiment noVariant =
          new Experiment("cfg_variant", "d", 100, "control", null, "suite@rex.com");

      assertThat(validator.validate(noTraffic))
          .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("trafficPercentage"));
      assertThat(validator.validate(noVariant))
          .anySatisfy(v -> assertThat(v.getPropertyPath()).hasToString("testVariantName"));
    }

    @Test
    @DisplayName("pause applies only to a running experiment")
    void pauseAppliesOnlyToRunning() {
      Experiment draft = draft("lifecycle_pause_draft", 100);
      assertThatThrownBy(() -> experimentService.pauseExperiment(draft.getId()))
          .isInstanceOf(IllegalStateException.class);

      Experiment live = running("lifecycle_pause_running", 100);
      assertThat(experimentService.pauseExperiment(live.getId()).getStatus())
          .isEqualTo(Experiment.ExperimentStatus.PAUSED);
    }

    @Test
    @DisplayName("archiving is refused until the experiment has finished")
    void archiveRequiresAFinishedExperiment() {
      Experiment live = running("lifecycle_archive", 100);

      assertThatThrownBy(() -> experimentService.archiveExperiment(live.getId()))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("acting on an experiment that does not exist is rejected")
    void unknownIdIsRejected() {
      assertThatThrownBy(() -> experimentService.startExperiment(999_999L))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("user assignment")
  class UserAssignment {

    @Test
    @DisplayName("a user cannot be assigned before the experiment runs")
    void assignmentRequiresARunningExperiment() {
      Experiment experiment = draft("assign_not_running", 100);

      assertThatThrownBy(
              () -> experimentService.assignUserToExperiment("user-1", experiment.getId(), "s"))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("assignment is idempotent, so a returning user keeps their variant")
    void assignmentIsIdempotent() {
      Experiment experiment = running("assign_idempotent", 100);

      UserCohort first =
          experimentService.assignUserToExperiment("user-1", experiment.getId(), "s1");
      UserCohort second =
          experimentService.assignUserToExperiment("user-1", experiment.getId(), "s2");

      assertThat(second.getId()).isEqualTo(first.getId());
      assertThat(second.getVariantName()).isEqualTo(first.getVariantName());
    }

    @Test
    @DisplayName("an assigned user lands on one of the two declared variants")
    void assignmentUsesTheDeclaredVariants() {
      Experiment experiment = running("assign_variants", 100);

      UserCohort cohort =
          experimentService.assignUserToExperiment("user-1", experiment.getId(), "s");

      assertThat(cohort.getVariantName()).isIn("control", "test");
      assertThat(cohort.getCohortType())
          .isIn(UserCohort.CohortType.CONTROL, UserCohort.CohortType.TREATMENT);
      assertThat(cohort.getAssignmentMethod()).isEqualTo(UserCohort.AssignmentMethod.HASH_BASED);
    }

    @Test
    @DisplayName("traffic percentage keeps users out, recorded as excluded rather than dropped")
    void excludedUsersAreRecorded() {
      Experiment experiment = running("assign_excluded", 1);

      List<UserCohort> cohorts =
          java.util.stream.IntStream.range(0, 60)
              .mapToObj(
                  i ->
                      experimentService.assignUserToExperiment(
                          "user-" + i, experiment.getId(), "s"))
              .toList();

      assertThat(cohorts).anyMatch(c -> c.getCohortType() == UserCohort.CohortType.EXCLUDED);
      assertThat(cohorts)
          .filteredOn(c -> c.getCohortType() == UserCohort.CohortType.EXCLUDED)
          .allSatisfy(c -> assertThat(c.getVariantName()).isEqualTo("excluded"));
    }

    @Test
    @DisplayName("at full traffic every user is included")
    void fullTrafficIncludesEveryone() {
      Experiment experiment = running("assign_full_traffic", 100);

      assertThat(
              java.util.stream.IntStream.range(0, 50)
                  .mapToObj(
                      i ->
                          experimentService.assignUserToExperiment(
                              "user-" + i, experiment.getId(), "s"))
                  .toList())
          .noneMatch(c -> c.getCohortType() == UserCohort.CohortType.EXCLUDED);
    }

    @Test
    @DisplayName("the split between control and treatment is roughly even")
    void splitIsRoughlyEven() {
      Experiment experiment = running("assign_split", 100);

      long control =
          java.util.stream.IntStream.range(0, 600)
              .mapToObj(
                  i ->
                      experimentService.assignUserToExperiment(
                          "split-user-" + i, experiment.getId(), "s"))
              .filter(c -> c.getCohortType() == UserCohort.CohortType.CONTROL)
              .count();

      // Three standard deviations for n=600 at p=0.5 is about 37 users, so this band catches a
      // broken split without failing on ordinary sampling noise.
      assertThat(control).isBetween(263L, 337L);
    }

    @Test
    @DisplayName("only included users count toward the sample size")
    void sampleSizeCountsIncludedUsersOnly() {
      Experiment experiment = running("assign_sample_size", 100);
      for (int i = 0; i < 10; i++) {
        experimentService.assignUserToExperiment("counted-user-" + i, experiment.getId(), "s");
      }

      assertThat(
              experimentRepository
                  .findById(experiment.getId())
                  .orElseThrow()
                  .getCurrentSampleSize())
          .isEqualTo(10);
    }

    @Test
    @DisplayName("an assignment is retrievable afterwards")
    void assignmentIsRetrievable() {
      Experiment experiment = running("assign_lookup", 100);
      experimentService.assignUserToExperiment("user-lookup", experiment.getId(), "s");

      assertThat(experimentService.isUserAssignedToExperiment("user-lookup", experiment.getId()))
          .isTrue();
      assertThat(experimentService.getUserAssignment("user-lookup", experiment.getId()))
          .isPresent();
      assertThat(experimentService.getUserAssignments("user-lookup")).hasSize(1);
      assertThat(experimentService.isUserAssignedToExperiment("nobody", experiment.getId()))
          .isFalse();
    }
  }

  @Nested
  @DisplayName("sample size gate")
  class SampleSizeGate {

    @Test
    @DisplayName("an experiment with no minimum is treated as satisfied")
    void noMinimumIsSatisfied() {
      Experiment experiment = draft("gate_no_minimum", 100);
      experiment.setMinimumSampleSize(null);
      experimentRepository.saveAndFlush(experiment);

      assertThat(experimentService.hasReachedMinimumSampleSize(experiment.getId())).isTrue();
    }

    @Test
    @DisplayName("the gate opens only once the minimum is met")
    void gateOpensAtTheMinimum() {
      Experiment experiment = running("gate_minimum", 100);
      experiment.setMinimumSampleSize(3);
      experimentRepository.saveAndFlush(experiment);

      assertThat(experimentService.hasReachedMinimumSampleSize(experiment.getId())).isFalse();

      for (int i = 0; i < 3; i++) {
        experimentService.assignUserToExperiment("gate-user-" + i, experiment.getId(), "s");
      }

      assertThat(experimentService.hasReachedMinimumSampleSize(experiment.getId())).isTrue();
    }

    @Test
    @DisplayName("completion is reported as a percentage of the minimum")
    void completionIsAPercentage() {
      Experiment experiment = running("gate_completion", 100);
      experiment.setMinimumSampleSize(4);
      experimentRepository.saveAndFlush(experiment);
      experimentService.assignUserToExperiment("completion-user-1", experiment.getId(), "s");

      assertThat(experimentService.getExperimentCompletionPercentage(experiment.getId()))
          .isEqualTo(25.0);
    }
  }
}
