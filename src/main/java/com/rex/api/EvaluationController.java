package com.rex.api;

import com.rex.api.dto.EvaluationResponse;
import com.rex.evaluation.EvaluationResult;
import com.rex.evaluation.FlagContext;
import com.rex.evaluation.FlagEvaluator;
import com.rex.model.FeatureFlag;
import com.rex.service.FeatureFlagService;
import com.rex.service.TargetingRuleService;
import com.rex.telemetry.ExposureRecorder;
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
  private final ExposureRecorder exposureRecorder;
  private final TargetingRuleService targetingRuleService;

  public EvaluationController(
      FeatureFlagService flagService,
      ExposureRecorder exposureRecorder,
      TargetingRuleService targetingRuleService) {
    this.flagService = flagService;
    this.exposureRecorder = exposureRecorder;
    this.targetingRuleService = targetingRuleService;
  }

  @GetMapping("/{flagName}")
  public EvaluationResponse evaluate(
      @PathVariable String flagName,
      @RequestParam String userId,
      @RequestParam(defaultValue = DEFAULT_ENVIRONMENT) String environment,
      @RequestParam(required = false) java.util.Map<String, String> attributes) {

    return flagService
        .getFlagByName(flagName)
        .map(flag -> decide(flag, flagName, userId, environment, userAttributes(attributes)))
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
        .map(flag -> decide(flag, flag.getName(), userId, environment, java.util.Map.of()))
        .toList();
  }

  /** Query parameters other than the reserved ones are treated as user attributes. */
  private static java.util.Map<String, String> userAttributes(
      java.util.Map<String, String> allParameters) {
    if (allParameters == null) {
      return java.util.Map.of();
    }
    java.util.Map<String, String> attributes = new java.util.HashMap<>(allParameters);
    attributes.remove("userId");
    attributes.remove("environment");
    return attributes;
  }

  private EvaluationResponse decide(
      FeatureFlag flag,
      String flagName,
      String userId,
      String environment,
      java.util.Map<String, String> attributes) {

    FlagContext context =
        new FlagContext(
            flag.getName(),
            Boolean.TRUE.equals(flag.getEnabled()),
            flag.getEnvironment(),
            flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0,
            targetingRuleService.rulesFor(flag.getId()));

    EvaluationResult result = FlagEvaluator.evaluate(context, userId, environment, attributes);
    exposureRecorder.recordFlagExposure(flag, userId, result.enabled(), environment);

    return new EvaluationResponse(
        flagName,
        result.enabled(),
        EvaluationResponse.EvaluationReason.valueOf(result.reason().name()),
        result.bucket());
  }
}
