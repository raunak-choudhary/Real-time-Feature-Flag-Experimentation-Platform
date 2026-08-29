package com.rex.api;

import com.rex.api.dto.EvaluationResponse;
import com.rex.evaluation.EvaluationResult;
import com.rex.evaluation.FlagContext;
import com.rex.evaluation.FlagEvaluator;
import com.rex.model.FeatureFlag;
import com.rex.service.FeatureFlagService;
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

  public EvaluationController(FeatureFlagService flagService, ExposureRecorder exposureRecorder) {
    this.flagService = flagService;
    this.exposureRecorder = exposureRecorder;
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

    FlagContext context =
        new FlagContext(
            flag.getName(),
            Boolean.TRUE.equals(flag.getEnabled()),
            flag.getEnvironment(),
            flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0);

    EvaluationResult result = FlagEvaluator.evaluate(context, userId, environment);
    exposureRecorder.recordFlagExposure(flag, userId, result.enabled(), environment);

    return new EvaluationResponse(
        flagName,
        result.enabled(),
        EvaluationResponse.EvaluationReason.valueOf(result.reason().name()),
        result.bucket());
  }
}
