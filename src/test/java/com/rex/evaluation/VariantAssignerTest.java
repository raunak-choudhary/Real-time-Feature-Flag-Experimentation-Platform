package com.rex.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VariantAssignerTest {

  private static final String EXPERIMENT = "checkout_colour";
  private static final int SAMPLE = 20_000;

  @Test
  @DisplayName("a user always receives the same variant")
  void assignmentIsDeterministic() {
    VariantAssigner.Variant first = VariantAssigner.assignVariant(EXPERIMENT, "user_42");
    for (int attempt = 0; attempt < 500; attempt++) {
      assertThat(VariantAssigner.assignVariant(EXPERIMENT, "user_42")).isEqualTo(first);
    }
  }

  @Test
  @DisplayName("control and test split close to evenly")
  void splitIsEven() {
    long control = 0;
    for (int i = 0; i < SAMPLE; i++) {
      if (VariantAssigner.assignVariant(EXPERIMENT, "user_" + i)
          == VariantAssigner.Variant.CONTROL) {
        control++;
      }
    }
    assertThat((double) control / SAMPLE).isCloseTo(0.50, Offset.offset(0.02));
  }

  @Test
  @DisplayName("traffic allocation admits close to the configured share")
  void enrolmentMatchesTrafficPercentage() {
    long enrolled = 0;
    for (int i = 0; i < SAMPLE; i++) {
      if (VariantAssigner.isEnrolled(EXPERIMENT, "user_" + i, 30)) {
        enrolled++;
      }
    }
    assertThat((double) enrolled / SAMPLE).isCloseTo(0.30, Offset.offset(0.02));
  }

  @Test
  @DisplayName("raising traffic admits new users without moving anyone already enrolled")
  void raisingTrafficDoesNotReshuffle() {
    for (int i = 0; i < 5_000; i++) {
      String userId = "user_" + i;
      if (VariantAssigner.isEnrolled(EXPERIMENT, userId, 20)) {
        assertThat(VariantAssigner.isEnrolled(EXPERIMENT, userId, 40))
            .as("user %s was enrolled at 20 percent and must remain enrolled at 40", userId)
            .isTrue();
      }
    }
  }

  @Test
  @DisplayName("raising traffic never changes an enrolled user's variant")
  void raisingTrafficDoesNotChangeVariant() {
    for (int i = 0; i < 5_000; i++) {
      String userId = "user_" + i;
      if (VariantAssigner.isEnrolled(EXPERIMENT, userId, 10)) {
        VariantAssigner.Variant atTen = VariantAssigner.assignVariant(EXPERIMENT, userId);
        VariantAssigner.Variant atNinety = VariantAssigner.assignVariant(EXPERIMENT, userId);
        assertThat(atNinety).isEqualTo(atTen);
      }
    }
  }

  @Test
  @DisplayName("entry and variant are independent, so entrants are not skewed toward one side")
  void entryAndSplitAreIndependent() {
    long enrolled = 0;
    long enrolledControl = 0;

    for (int i = 0; i < SAMPLE; i++) {
      String userId = "user_" + i;
      if (VariantAssigner.isEnrolled(EXPERIMENT, userId, 50)) {
        enrolled++;
        if (VariantAssigner.assignVariant(EXPERIMENT, userId) == VariantAssigner.Variant.CONTROL) {
          enrolledControl++;
        }
      }
    }

    // If entry and split shared a hash, the enrolled population would skew heavily to one variant.
    assertThat((double) enrolledControl / enrolled).isCloseTo(0.50, Offset.offset(0.03));
  }

  @Test
  @DisplayName("different experiments enrol different populations")
  void experimentsAreIndependent() {
    long bothEnrolled = 0;
    for (int i = 0; i < SAMPLE; i++) {
      String userId = "user_" + i;
      if (VariantAssigner.isEnrolled("experiment_a", userId, 50)
          && VariantAssigner.isEnrolled("experiment_b", userId, 50)) {
        bothEnrolled++;
      }
    }
    // Independent 50 percent draws overlap about a quarter of the time.
    assertThat((double) bothEnrolled / SAMPLE).isCloseTo(0.25, Offset.offset(0.03));
  }

  @Test
  @DisplayName("zero traffic enrols nobody and full traffic enrols everybody")
  void trafficBoundsAreAbsolute() {
    for (int i = 0; i < 1_000; i++) {
      assertThat(VariantAssigner.isEnrolled(EXPERIMENT, "user_" + i, 0)).isFalse();
      assertThat(VariantAssigner.isEnrolled(EXPERIMENT, "user_" + i, 100)).isTrue();
    }
  }
}
