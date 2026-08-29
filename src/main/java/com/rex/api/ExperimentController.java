package com.rex.api;

import com.rex.api.dto.AssignmentRequest;
import com.rex.api.dto.AssignmentResponse;
import com.rex.api.dto.ExperimentAnalysisResponse;
import com.rex.api.dto.ExperimentRequest;
import com.rex.api.dto.ExperimentResponse;
import com.rex.api.mapper.ExperimentMapper;
import com.rex.exception.ResourceNotFoundException;
import com.rex.model.Experiment;
import com.rex.service.ExperimentAnalysisService;
import com.rex.service.ExperimentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Experiment lifecycle and user enrolment. */
@RestController
@RequestMapping("/api/v1/experiments")
public class ExperimentController {

  private final ExperimentService experimentService;
  private final ExperimentAnalysisService analysisService;
  private final ExperimentMapper mapper;

  public ExperimentController(
      ExperimentService experimentService,
      ExperimentAnalysisService analysisService,
      ExperimentMapper mapper) {
    this.experimentService = experimentService;
    this.analysisService = analysisService;
    this.mapper = mapper;
  }

  /**
   * The statistical result.
   *
   * <p>Reports inconclusive when the sample is short, regardless of how small the p-value looks.
   */
  @GetMapping("/{id}/analysis")
  public ExperimentAnalysisResponse analysis(@PathVariable Long id) {
    Experiment experiment = requireExperiment(id);
    return mapper.toAnalysisResponse(experiment, analysisService.analyse(experiment));
  }

  @GetMapping
  public List<ExperimentResponse> list(@RequestParam(required = false) String environment) {
    List<Experiment> experiments =
        environment == null
            ? experimentService.getAllExperiments()
            : experimentService.getExperimentsByEnvironment(environment);
    return experiments.stream().map(mapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  public ExperimentResponse get(@PathVariable Long id) {
    return mapper.toResponse(requireExperiment(id));
  }

  @PostMapping
  public ResponseEntity<ExperimentResponse> create(@Valid @RequestBody ExperimentRequest request) {
    Experiment saved =
        experimentService.createExperiment(
            request.name(),
            request.description(),
            request.hypothesis(),
            request.controlVariantName(),
            request.testVariantName(),
            request.trafficPercentage(),
            request.environment(),
            request.createdBy());
    return ResponseEntity.created(URI.create("/api/v1/experiments/" + saved.getId()))
        .body(mapper.toResponse(saved));
  }

  // A draft cannot be started, and this is the only transition that leaves it. Without this route
  // an experiment created through the API could never run through the API.
  @PostMapping("/{id}/ready")
  public ExperimentResponse markReady(@PathVariable Long id) {
    requireExperiment(id);
    return mapper.toResponse(experimentService.markExperimentReady(id));
  }

  @PostMapping("/{id}/start")
  public ExperimentResponse start(@PathVariable Long id) {
    requireExperiment(id);
    return mapper.toResponse(experimentService.startExperiment(id));
  }

  @PostMapping("/{id}/pause")
  public ExperimentResponse pause(@PathVariable Long id) {
    requireExperiment(id);
    return mapper.toResponse(experimentService.pauseExperiment(id));
  }

  @PostMapping("/{id}/stop")
  public ExperimentResponse stop(@PathVariable Long id) {
    requireExperiment(id);
    return mapper.toResponse(experimentService.stopExperiment(id));
  }

  @PostMapping("/{id}/assignments")
  public AssignmentResponse assign(
      @PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
    requireExperiment(id);
    return mapper.toAssignmentResponse(
        experimentService.assignUserToExperiment(request.userId(), id, request.sessionId()));
  }

  @GetMapping("/{id}/assignments/{userId}")
  public AssignmentResponse getAssignment(@PathVariable Long id, @PathVariable String userId) {
    requireExperiment(id);
    return experimentService
        .getUserAssignment(userId, id)
        .map(mapper::toAssignmentResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Assignment for user", userId));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    requireExperiment(id);
    experimentService.deleteExperiment(id);
    return ResponseEntity.noContent().build();
  }

  private Experiment requireExperiment(Long id) {
    return experimentService
        .getExperimentById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Experiment", id));
  }
}
