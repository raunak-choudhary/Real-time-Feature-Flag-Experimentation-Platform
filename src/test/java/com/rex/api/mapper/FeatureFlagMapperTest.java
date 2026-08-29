package com.rex.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.api.dto.FeatureFlagRequest;
import com.rex.api.dto.FeatureFlagResponse;
import com.rex.model.FeatureFlag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
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

  private static FeatureFlagRequest request(Integer rollout) {
    return new FeatureFlagRequest(
        "dark_mode",
        "Dark theme",
        true,
        FeatureFlag.FlagStatus.ACTIVE,
        rollout,
        "production",
        "admin@rex.com");
  }

  @Test
  @DisplayName("a valid request maps every field onto the entity")
  void mapsAllFields() {
    FeatureFlag flag = mapper.toEntity(request(25));

    assertThat(flag.getName()).isEqualTo("dark_mode");
    assertThat(flag.getDescription()).isEqualTo("Dark theme");
    assertThat(flag.getEnabled()).isTrue();
    assertThat(flag.getStatus()).isEqualTo(FeatureFlag.FlagStatus.ACTIVE);
    assertThat(flag.getRolloutPercentage()).isEqualTo(25);
    assertThat(flag.getEnvironment()).isEqualTo("production");
    assertThat(flag.getCreatedBy()).isEqualTo("admin@rex.com");
  }

  @Test
  @DisplayName("a null description maps without error, since the column is nullable")
  void nullDescriptionIsAllowed() {
    FeatureFlagRequest request =
        new FeatureFlagRequest("dark_mode", null, true, null, 10, null, null);

    FeatureFlag flag = mapper.toEntity(request);

    assertThat(flag.getDescription()).isNull();
    assertThat(flag.getEnvironment()).isEqualTo("development");
    assertThat(flag.getStatus()).isEqualTo(FeatureFlag.FlagStatus.INACTIVE);
  }

  @Test
  @DisplayName("an unset enabled flag defaults to off rather than null")
  void nullEnabledDefaultsToOff() {
    FeatureFlagRequest request =
        new FeatureFlagRequest("dark_mode", null, null, null, null, null, null);

    FeatureFlag flag = mapper.toEntity(request);

    assertThat(flag.getEnabled()).isFalse();
    assertThat(flag.getRolloutPercentage()).isZero();
  }

  @ParameterizedTest(name = "rollout {0} is accepted")
  @ValueSource(ints = {0, 1, 50, 99, 100})
  @DisplayName("rollout percentage accepts the whole inclusive range")
  void rolloutBoundsAccepted(int rollout) {
    Set<ConstraintViolation<FeatureFlagRequest>> violations = validator.validate(request(rollout));
    assertThat(violations).isEmpty();
  }

  @ParameterizedTest(name = "rollout {0} is rejected")
  @ValueSource(ints = {-1, 101, 1000})
  @DisplayName("rollout percentage rejects anything outside 0 to 100")
  void rolloutBoundsRejected(int rollout) {
    Set<ConstraintViolation<FeatureFlagRequest>> violations = validator.validate(request(rollout));
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("rolloutPercentage"));
  }

  @Test
  @DisplayName("a blank name is rejected at the boundary")
  void blankNameRejected() {
    FeatureFlagRequest request = new FeatureFlagRequest("  ", null, true, null, 0, null, null);

    assertThat(validator.validate(request))
        .extracting(ConstraintViolation::getMessage)
        .contains("name is required");
  }

  @Test
  @DisplayName("a name with spaces or capitals is rejected, keeping flag keys machine safe")
  void malformedNameRejected() {
    FeatureFlagRequest request =
        new FeatureFlagRequest("Dark Mode", null, true, null, 0, null, null);

    assertThat(validator.validate(request))
        .extracting(ConstraintViolation::getMessage)
        .anyMatch(message -> message.contains("lower case alphanumeric"));
  }

  @Test
  @DisplayName("the response carries the entity values back out")
  void mapsToResponse() {
    FeatureFlag flag = mapper.toEntity(request(75));
    flag.setId(7L);

    FeatureFlagResponse response = mapper.toResponse(flag);

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.name()).isEqualTo("dark_mode");
    assertThat(response.enabled()).isTrue();
    assertThat(response.rolloutPercentage()).isEqualTo(75);
  }

  @Test
  @DisplayName("applyTo updates an existing flag without replacing it")
  void applyToUpdatesInPlace() {
    FeatureFlag existing = mapper.toEntity(request(10));
    existing.setId(3L);

    mapper.applyTo(existing, request(90));

    assertThat(existing.getId()).as("identity is preserved").isEqualTo(3L);
    assertThat(existing.getRolloutPercentage()).isEqualTo(90);
  }
}
