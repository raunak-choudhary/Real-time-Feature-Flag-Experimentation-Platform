package com.rex.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.api.dto.ExperimentRequest;
import com.rex.api.dto.ExperimentResponse;
import com.rex.model.Experiment;
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

class ExperimentMapperTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private final ExperimentMapper mapper = new ExperimentMapper();

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
  @DisplayName("a valid request maps every field onto the entity")
  void mapsAllFields() {
    Experiment experiment = mapper.toEntity(request(30, 99.0));

    assertThat(experiment.getName()).isEqualTo("checkout_button_colour");
    assertThat(experiment.getHypothesis()).isEqualTo("Green converts better than blue");
    assertThat(experiment.getSuccessMetric()).isEqualTo("conversion_rate");
    assertThat(experiment.getTrafficPercentage()).isEqualTo(30);
    assertThat(experiment.getControlVariantName()).isEqualTo("blue");
    assertThat(experiment.getTestVariantName()).isEqualTo("green");
    assertThat(experiment.getConfidenceLevel()).isEqualTo(99.0);
    assertThat(experiment.getMinimumSampleSize()).isEqualTo(1000);
  }

  @Test
  @DisplayName("optional fields fall back to sensible defaults rather than null")
  void defaultsApplied() {
    ExperimentRequest sparse =
        new ExperimentRequest(
            "minimal", null, null, null, null, null, null, null, null, null, null, null);

    Experiment experiment = mapper.toEntity(sparse);

    assertThat(experiment.getTrafficPercentage()).isEqualTo(50);
    assertThat(experiment.getConfidenceLevel()).isEqualTo(95.0);
    assertThat(experiment.getEnvironment()).isEqualTo("development");
    assertThat(experiment.getStatus()).isEqualTo(Experiment.ExperimentStatus.DRAFT);
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
  @DisplayName("the response carries the entity values back out")
  void mapsToResponse() {
    Experiment experiment = mapper.toEntity(request(40, 95.0));
    experiment.setId(11L);

    ExperimentResponse response = mapper.toResponse(experiment);

    assertThat(response.id()).isEqualTo(11L);
    assertThat(response.name()).isEqualTo("checkout_button_colour");
    assertThat(response.trafficPercentage()).isEqualTo(40);
    assertThat(response.status()).isEqualTo(Experiment.ExperimentStatus.DRAFT);
  }
}
