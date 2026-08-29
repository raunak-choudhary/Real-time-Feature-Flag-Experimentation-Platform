package com.rex.api;

import com.rex.api.dto.EvaluationResponse;
import com.rex.model.FeatureFlag;
import com.rex.service.FeatureFlagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The SDK entry point.
 *
 * <p>An unknown flag returns a documented off decision rather than a 404. A client asking about a
 * flag that has not been created yet is a normal condition during a rollout, and failing the call
 * would turn a configuration gap in this service into an error in the calling application.
 */
@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {

  private static final String DEFAULT_ENVIRONMENT = "development";

  private final FeatureFlagService flagService;

  public EvaluationController(FeatureFlagService flagService) {
    this.flagService = flagService;
  }

  @GetMapping("/{flagName}")
  public EvaluationResponse evaluate(
      @PathVariable String flagName,
      @RequestParam String userId,
      @RequestParam(defaultValue = DEFAULT_ENVIRONMENT) String environment) {

    return flagService
        .getFlagByName(flagName)
        .map(flag -> decide(flag, flagName, userId, environment))
        .orElseGet(
            () ->
                new EvaluationResponse(
                    flagName, false, EvaluationResponse.EvaluationReason.FLAG_NOT_FOUND, null));
  }

  /** Bulk evaluation, so an SDK can bootstrap its whole cache in one round trip. */
  @GetMapping
  public List<EvaluationResponse> evaluateAll(
      @RequestParam String userId,
      @RequestParam(defaultValue = DEFAULT_ENVIRONMENT) String environment) {

    return flagService.getFlagsByEnvironment(environment).stream()
        .map(flag -> decide(flag, flag.getName(), userId, environment))
        .toList();
  }

  private EvaluationResponse decide(
      FeatureFlag flag, String flagName, String userId, String environment) {

    if (!Boolean.TRUE.equals(flag.getEnabled())) {
      return new EvaluationResponse(
          flagName, false, EvaluationResponse.EvaluationReason.FLAG_DISABLED, null);
    }
    if (!environment.equals(flag.getEnvironment())) {
      return new EvaluationResponse(
          flagName, false, EvaluationResponse.EvaluationReason.ENVIRONMENT_MISMATCH, null);
    }

    boolean enabled = flagService.isFlagEnabledForUser(flagName, userId, environment);
    return new EvaluationResponse(
        flagName,
        enabled,
        enabled
            ? EvaluationResponse.EvaluationReason.ROLLOUT_INCLUDED
            : EvaluationResponse.EvaluationReason.ROLLOUT_EXCLUDED,
        null);
  }
}
