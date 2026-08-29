package com.rex.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.api.dto.ExperimentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Boundary rules on the experiment request payload. */
class ExperimentMapperTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDownValidator() {
    validatorFactory.close();
  }

  private static ExperimentRequest request(Integer traffic, Double confidence) {
    return new ExperimentRequest(
        "checkout_button_colour",
        "Does a green button convert better",
        "Green converts better than blue",
        "conversion_rate",
        traffic,
        "blue",
        "green",
        confidence,
        1000,
        5.0,
        "production",
        "product@rex.com");
  }

  @Test
  @DisplayName("a fully populated request passes validation")
  void validRequestPasses() {
    assertThat(validator.validate(request(30, 99.0))).isEmpty();
  }

  @ParameterizedTest(name = "traffic {0} is accepted")
  @ValueSource(ints = {1, 50, 100})
  @DisplayName("traffic percentage accepts one through one hundred")
  void trafficBoundsAccepted(int traffic) {
    assertThat(validator.validate(request(traffic, 95.0))).isEmpty();
  }

  @ParameterizedTest(name = "traffic {0} is rejected")
  @ValueSource(ints = {0, -1, 101})
  @DisplayName("traffic percentage rejects zero, since an experiment with no traffic never enrols")
  void trafficBoundsRejected(int traffic) {
    assertThat(validator.validate(request(traffic, 95.0)))
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("trafficPercentage"));
  }

  @ParameterizedTest(name = "confidence {0} is rejected")
  @ValueSource(doubles = {79.9, 100.0, 0.95})
  @DisplayName("confidence level is bounded to a range that makes statistical sense")
  void confidenceBoundsRejected(double confidence) {
    assertThat(validator.validate(request(50, confidence)))
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("confidenceLevel"));
  }

  @Test
  @DisplayName("a blank name is rejected")
  void blankNameRejected() {
    ExperimentRequest blank =
        new ExperimentRequest(" ", null, null, null, 50, null, null, 95.0, null, null, null, null);

    assertThat(validator.validate(blank))
        .extracting(ConstraintViolation::getMessage)
        .contains("name is required");
  }
}
