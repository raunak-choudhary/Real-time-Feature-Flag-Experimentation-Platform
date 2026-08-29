package com.rex.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.api.dto.FeatureFlagRequest;
import com.rex.api.dto.FeatureFlagResponse;
import com.rex.model.FeatureFlag;
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

class FeatureFlagMapperTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private final FeatureFlagMapper mapper = new FeatureFlagMapper();

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDownValidator() {
    validatorFactory.close();
  }

  private static FeatureFlagRequest request(String name, Integer rollout) {
    return new FeatureFlagRequest(
        name,
        "Dark theme",
        true,
        FeatureFlag.FlagStatus.ACTIVE,
        rollout,
        "production",
        "admin@rex.com");
  }

  @ParameterizedTest(name = "rollout {0} is accepted")
  @ValueSource(ints = {0, 1, 50, 99, 100})
  @DisplayName("rollout percentage accepts the whole inclusive range")
  void rolloutBoundsAccepted(int rollout) {
    assertThat(validator.validate(request("dark_mode", rollout))).isEmpty();
  }

  @ParameterizedTest(name = "rollout {0} is rejected")
  @ValueSource(ints = {-1, 101, 1000})
  @DisplayName("rollout percentage rejects anything outside 0 to 100")
  void rolloutBoundsRejected(int rollout) {
    assertThat(validator.validate(request("dark_mode", rollout)))
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("rolloutPercentage"));
  }

  @Test
  @DisplayName("a blank name is rejected at the boundary")
  void blankNameRejected() {
    assertThat(validator.validate(request("  ", 0)))
        .extracting(ConstraintViolation::getMessage)
        .contains("name is required");
  }

  @ParameterizedTest(name = "name '{0}' is rejected")
  @ValueSource(strings = {"Dark Mode", "darkMode", "dark-mode", "DARK_MODE"})
  @DisplayName("flag names must stay machine safe, so spaces, capitals and hyphens are rejected")
  void malformedNameRejected(String name) {
    assertThat(validator.validate(request(name, 0)))
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("lower case alphanumeric"));
  }

  @Test
  @DisplayName("the entity maps out to a response with every field carried across")
  void mapsToResponse() {
    FeatureFlag flag = new FeatureFlag();
    flag.setId(7L);
    flag.setName("dark_mode");
    flag.setDescription("Dark theme");
    flag.setEnabled(true);
    flag.setStatus(FeatureFlag.FlagStatus.ACTIVE);
    flag.setRolloutPercentage(75);
    flag.setEnvironment("production");
    flag.setCreatedBy("admin@rex.com");

    FeatureFlagResponse response = mapper.toResponse(flag);

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.name()).isEqualTo("dark_mode");
    assertThat(response.enabled()).isTrue();
    assertThat(response.status()).isEqualTo(FeatureFlag.FlagStatus.ACTIVE);
    assertThat(response.rolloutPercentage()).isEqualTo(75);
    assertThat(response.environment()).isEqualTo("production");
  }

  @Test
  @DisplayName("null enabled and rollout become safe defaults rather than a null response")
  void nullsBecomeSafeDefaults() {
    FeatureFlag flag = new FeatureFlag();
    flag.setName("minimal");
    flag.setEnabled(null);
    flag.setRolloutPercentage(null);

    FeatureFlagResponse response = mapper.toResponse(flag);

    assertThat(response.enabled()).isFalse();
    assertThat(response.rolloutPercentage()).isZero();
  }
}
