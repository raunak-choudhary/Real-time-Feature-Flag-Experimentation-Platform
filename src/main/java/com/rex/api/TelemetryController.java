package com.rex.api;

import com.rex.api.dto.ConversionRequest;
import com.rex.exception.ResourceNotFoundException;
import com.rex.model.Experiment;
import com.rex.model.Metrics;
import com.rex.service.ExperimentService;
import com.rex.service.MetricsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestion for client reported events.
 *
 * <p>This is the only route by which conversions enter the system, so the statistics engine has no
 * input without it.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

  private final MetricsService metricsService;
  private final ExperimentService experimentService;

  public TelemetryController(MetricsService metricsService, ExperimentService experimentService) {
    this.metricsService = metricsService;
    this.experimentService = experimentService;
  }

  /**
   * Records a conversion against the user's assigned variant.
   *
   * <p>A conversion from a user who was never enrolled is rejected rather than attributed to
   * control. Silently defaulting would quietly bias every experiment result.
   */
  @PostMapping("/conversions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void recordConversion(@Valid @RequestBody ConversionRequest request) {
    Experiment experiment =
        experimentService
            .getExperimentById(request.experimentId())
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", request.experimentId()));

    String variant =
        experimentService
            .getUserAssignment(request.userId(), request.experimentId())
            .map(cohort -> cohort.getVariantName())
            .orElseThrow(
                () -> new ResourceNotFoundException("Assignment for user", request.userId()));

    Metrics conversion =
        metricsService.trackConversion(
            request.userId(),
            experiment,
            variant,
            request.value(),
            request.sessionId(),
            experiment.getEnvironment());

    if (conversion == null) {
      throw new IllegalStateException("Conversion could not be recorded");
    }
  }
}
