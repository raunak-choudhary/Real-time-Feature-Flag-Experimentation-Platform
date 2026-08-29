package com.rex.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The bucketing contract. These properties are the specification: everything downstream, from
 * progressive rollout to experiment validity, depends on them holding.
 */
class BucketHasherTest {

  private static final int SAMPLE_SIZE = 100_000;

  @Test
  @DisplayName("the same namespace and user always produce the same bucket")
  void isDeterministic() {
    int first = BucketHasher.bucketFor("checkout_flow", "user_12345");
    for (int attempt = 0; attempt < 1_000; attempt++) {
      assertThat(BucketHasher.bucketFor("checkout_flow", "user_12345")).isEqualTo(first);
    }
  }

  @Test
  @DisplayName("every bucket falls inside the valid range, including for hostile inputs")
  void bucketsAreAlwaysInRange() {
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      int bucket = BucketHasher.bucketFor("flag", "user_" + i);
      assertThat(bucket).isBetween(0, BucketHasher.BUCKET_COUNT - 1);
    }
  }

  @Test
  @DisplayName("regression: an input whose JDK hash is Integer.MIN_VALUE still buckets in range")
  void integerMinValueHashDoesNotEscapeTheRange() {
    // "polygenelubricants" is the canonical string whose String.hashCode() is Integer.MIN_VALUE.
    // Math.abs of that value is itself, and negative, which is the defect this class replaces.
    String pathological = "polygenelubricants";
    assertThat(pathological.hashCode()).isEqualTo(Integer.MIN_VALUE);
    assertThat(Math.abs(pathological.hashCode())).isNegative();

    int bucket = BucketHasher.bucketFor("flag", pathological);

    assertThat(bucket).isBetween(0, BucketHasher.BUCKET_COUNT - 1);
  }

  @ParameterizedTest(name = "user id ''{0}'' buckets without error")
  @ValueSource(strings = {"", " ", "a", "éèê", "😀"})
  @DisplayName("edge case user ids bucket without throwing")
  void edgeCaseInputs(String userId) {
    assertThat(BucketHasher.bucketFor("flag", userId)).isBetween(0, BucketHasher.BUCKET_COUNT - 1);
  }

  @Test
  @DisplayName("a very long user id buckets without error")
  void veryLongUserIdIsHandled() {
    assertThat(BucketHasher.bucketFor("flag", "a".repeat(4096)))
        .isBetween(0, BucketHasher.BUCKET_COUNT - 1);
  }

  @Test
  @DisplayName("100,000 users distribute uniformly across 100 buckets by chi-square")
  void distributionIsUniform() {
    int buckets = 100;
    int[] observed = new int[buckets];
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      observed[BucketHasher.percentileFor("uniformity_flag", "user_" + i)]++;
    }

    double expected = (double) SAMPLE_SIZE / buckets;
    double chiSquare = 0.0;
    for (int count : observed) {
      double delta = count - expected;
      chiSquare += delta * delta / expected;
    }

    // 99 degrees of freedom, critical value 148.23 at p = 0.001. A fair hash sits far below.
    assertThat(chiSquare)
        .as("chi-square across 100 buckets, lower is more uniform")
        .isLessThan(148.23);
  }

  @Test
  @DisplayName("sequential user ids do not cluster, which String.hashCode fails at")
  void sequentialIdsDoNotCluster() {
    int[] deciles = new int[10];
    for (int i = 0; i < 10_000; i++) {
      deciles[BucketHasher.percentileFor("clustering_flag", "user_" + i) / 10]++;
    }

    // Every decile should hold roughly a tenth. A hash with poor avalanche leaves some empty.
    for (int count : deciles) {
      assertThat(count).as("decile occupancy").isBetween(800, 1_200);
    }
  }

  @Test
  @DisplayName("a user's bucket in one namespace says nothing about another")
  void namespacesAreIndependent() {
    int agreements = 0;
    int trials = 5_000;

    for (int i = 0; i < trials; i++) {
      String userId = "user_" + i;
      int inFirst = BucketHasher.percentileFor("experiment_a", userId);
      int inSecond = BucketHasher.percentileFor("experiment_b", userId);
      if (inFirst == inSecond) {
        agreements++;
      }
    }

    // Independent draws agree about 1 percent of the time. Correlated ones agree far more.
    double agreementRate = (double) agreements / trials;
    assertThat(agreementRate).isLessThan(0.03);
  }

  @Test
  @DisplayName("rollout inclusion is monotonic, so raising a percentage never drops a user")
  void rolloutIsMonotonic() {
    for (int i = 0; i < 5_000; i++) {
      String userId = "user_" + i;
      boolean includedAtTen = BucketHasher.isInRollout("monotonic_flag", userId, 10);
      boolean includedAtTwenty = BucketHasher.isInRollout("monotonic_flag", userId, 20);

      if (includedAtTen) {
        assertThat(includedAtTwenty)
            .as("user %s was included at 10 percent and must remain included at 20", userId)
            .isTrue();
      }
    }
  }

  @Test
  @DisplayName("zero percent includes nobody and one hundred includes everybody")
  void rolloutBoundsAreAbsolute() {
    for (int i = 0; i < 1_000; i++) {
      String userId = "user_" + i;
      assertThat(BucketHasher.isInRollout("bounds_flag", userId, 0)).isFalse();
      assertThat(BucketHasher.isInRollout("bounds_flag", userId, 100)).isTrue();
    }
  }

  @Test
  @DisplayName("a fifty percent rollout includes close to half the population")
  void fiftyPercentIncludesAboutHalf() {
    int included = 0;
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      if (BucketHasher.isInRollout("half_flag", "user_" + i, 50)) {
        included++;
      }
    }

    double proportion = (double) included / SAMPLE_SIZE;
    assertThat(proportion).isCloseTo(0.50, org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  @DisplayName("bucket assignment is stable when unrelated namespaces are added")
  void addingNamespacesDoesNotReshuffle() {
    Map<String, Integer> before = new HashMap<>();
    for (int i = 0; i < 1_000; i++) {
      before.put("user_" + i, BucketHasher.bucketFor("stable_flag", "user_" + i));
    }

    for (int i = 0; i < 1_000; i++) {
      BucketHasher.bucketFor("some_other_experiment", "user_" + i);
    }

    before.forEach(
        (userId, bucket) ->
            assertThat(BucketHasher.bucketFor("stable_flag", userId)).isEqualTo(bucket));
  }
}
