package com.rex.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FlagEvaluatorTest {

  private static final String ENV = "production";
  private static final int SAMPLE = 10_000;

  private static FlagContext flag(boolean enabled, int rollout) {
    return new FlagContext("sample_flag", enabled, ENV, rollout);
  }

  @Test
  @DisplayName("a flag at full rollout is on for every user")
  void fullRolloutIncludesEveryone() {
    for (int i = 0; i < 1_000; i++) {
      EvaluationResult result = FlagEvaluator.evaluate(flag(true, 100), "user_" + i, ENV);
      assertThat(result.enabled()).isTrue();
      assertThat(result.reason()).isEqualTo(EvaluationReason.ROLLOUT_INCLUDED);
    }
  }

  @Test
  @DisplayName("a flag at zero rollout is off for every user, and says it was the bucket")
  void zeroRolloutExcludesEveryone() {
    for (int i = 0; i < 1_000; i++) {
      EvaluationResult result = FlagEvaluator.evaluate(flag(true, 0), "user_" + i, ENV);
      assertThat(result.enabled()).isFalse();
      assertThat(result.reason()).isEqualTo(EvaluationReason.ROLLOUT_EXCLUDED);
    }
  }

  @Test
  @DisplayName("a fifty percent rollout is on for about half the population")
  void halfRolloutSplitsRoughlyEvenly() {
    long enabled = 0;
    for (int i = 0; i < SAMPLE; i++) {
      if (FlagEvaluator.evaluate(flag(true, 50), "user_" + i, ENV).enabled()) {
        enabled++;
      }
    }
    assertThat((double) enabled / SAMPLE).isCloseTo(0.50, Offset.offset(0.02));
  }

  @Test
  @DisplayName("a disabled flag is off even at full rollout, and blames the kill switch")
  void disabledBeatsRollout() {
    EvaluationResult result = FlagEvaluator.evaluate(flag(false, 100), "user_1", ENV);

    assertThat(result.enabled()).isFalse();
    assertThat(result.reason())
        .as("the reason must name the kill switch, not the bucket")
        .isEqualTo(EvaluationReason.FLAG_DISABLED);
  }

  @Test
  @DisplayName("a production flag evaluated from development reports the mismatch")
  void environmentMismatchIsReported() {
    EvaluationResult result = FlagEvaluator.evaluate(flag(true, 100), "user_1", "development");

    assertThat(result.enabled()).isFalse();
    assertThat(result.reason()).isEqualTo(EvaluationReason.ENVIRONMENT_MISMATCH);
  }

  @Test
  @DisplayName("a missing flag returns not found rather than throwing")
  void missingFlagIsNotAnError() {
    EvaluationResult result = FlagEvaluator.evaluate(null, "user_1", ENV);

    assertThat(result.enabled()).isFalse();
    assertThat(result.reason()).isEqualTo(EvaluationReason.FLAG_NOT_FOUND);
  }

  @Test
  @DisplayName("raising the rollout never drops a user who was already included")
  void progressiveRolloutIsMonotonic() {
    int[] stages = {1, 5, 10, 25, 50, 75, 100};

    for (int i = 0; i < 2_000; i++) {
      String userId = "user_" + i;
      boolean wasIncluded = false;
      for (int stage : stages) {
        boolean included = FlagEvaluator.evaluate(flag(true, stage), userId, ENV).enabled();
        if (wasIncluded) {
          assertThat(included)
              .as(
                  "user %s was included at an earlier stage and must stay included at %d",
                  userId, stage)
              .isTrue();
        }
        wasIncluded = included;
      }
    }
  }

  @Test
  @DisplayName("the same user and flag decide identically on repeated calls")
  void evaluationIsDeterministic() {
    EvaluationResult first = FlagEvaluator.evaluate(flag(true, 37), "stable_user", ENV);
    for (int attempt = 0; attempt < 500; attempt++) {
      assertThat(FlagEvaluator.evaluate(flag(true, 37), "stable_user", ENV)).isEqualTo(first);
    }
  }

  @ParameterizedTest(name = "rollout {0} reports a bucket alongside the decision")
  @ValueSource(ints = {1, 25, 50, 99})
  @DisplayName("a bucketed decision carries the bucket, so it can be explained")
  void bucketIsReported(int rollout) {
    EvaluationResult result = FlagEvaluator.evaluate(flag(true, rollout), "user_7", ENV);

    assertThat(result.bucket()).isNotNull().isBetween(0, BucketHasher.BUCKET_COUNT - 1);
  }

  @Test
  @DisplayName("a decision blocked before bucketing carries no bucket")
  void shortCircuitedDecisionsHaveNoBucket() {
    assertThat(FlagEvaluator.evaluate(flag(false, 100), "user_1", ENV).bucket()).isNull();
    assertThat(FlagEvaluator.evaluate(flag(true, 100), "user_1", "staging").bucket()).isNull();
  }

  @Test
  @DisplayName("a flag with no environment set is treated as matching any environment")
  void nullEnvironmentMatchesAnything() {
    FlagContext unscoped = new FlagContext("unscoped", true, null, 100);

    assertThat(FlagEvaluator.evaluate(unscoped, "user_1", "anything").enabled()).isTrue();
  }
}
