package com.rex.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import com.rex.service.ExperimentService;
import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sample size counter feeds the Phase 4 significance gate, so a drift here would silently
 * change whether an experiment is allowed to declare a winner.
 */
@Transactional
class SampleSizeAccountingTest extends PostgresIntegrationTest {

  @Autowired private ExperimentService experimentService;

  private Experiment runningExperiment() {
    Experiment experiment =
        experimentService.createExperiment(
            "sample_size_probe_" + System.nanoTime(),
            "counts enrolments",
            "counting works",
            "control",
            "test",
            100,
            "production",
            "suite@rex.com");
    experimentService.markExperimentReady(experiment.getId());
    return experimentService.startExperiment(experiment.getId());
  }

  @Test
  @DisplayName("assigning a user increments the sample size exactly once")
  void assignmentIncrementsOnce() {
    Experiment experiment = runningExperiment();
    int before = experiment.getCurrentSampleSize();

    experimentService.assignUserToExperiment("size_user_a", experiment.getId(), "session-a");

    Experiment reloaded = experimentService.getExperimentById(experiment.getId()).orElseThrow();
    assertThat(reloaded.getCurrentSampleSize()).isEqualTo(before + 1);
  }

  @Test
  @DisplayName("re-assigning the same user does not double count")
  void repeatedAssignmentDoesNotDoubleCount() {
    Experiment experiment = runningExperiment();

    experimentService.assignUserToExperiment("size_user_b", experiment.getId(), "session-b");
    int afterFirst =
        experimentService
            .getExperimentById(experiment.getId())
            .orElseThrow()
            .getCurrentSampleSize();

    experimentService.assignUserToExperiment("size_user_b", experiment.getId(), "session-b");
    int afterSecond =
        experimentService
            .getExperimentById(experiment.getId())
            .orElseThrow()
            .getCurrentSampleSize();

    assertThat(afterSecond)
        .as("a returning user is already enrolled and must not be counted again")
        .isEqualTo(afterFirst);
  }

  @Test
  @DisplayName("a returning user keeps the variant they were first given")
  void assignmentIsSticky() {
    Experiment experiment = runningExperiment();

    UserCohort first =
        experimentService.assignUserToExperiment("size_user_c", experiment.getId(), "session-c");
    UserCohort second =
        experimentService.assignUserToExperiment("size_user_c", experiment.getId(), "session-c");

    assertThat(second.getVariantName()).isEqualTo(first.getVariantName());
  }
}
