package com.rex.api;

import com.rex.api.dto.FeatureFlagRequest;
import com.rex.api.dto.FeatureFlagResponse;
import com.rex.api.dto.RolloutUpdateRequest;
import com.rex.api.mapper.FeatureFlagMapper;
import com.rex.exception.ResourceNotFoundException;
import com.rex.model.FeatureFlag;
import com.rex.service.FeatureFlagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operator facing CRUD and toggle operations for feature flags. */
@RestController
@RequestMapping("/api/v1/flags")
public class FeatureFlagController {

  private final FeatureFlagService flagService;
  private final FeatureFlagMapper mapper;

  public FeatureFlagController(FeatureFlagService flagService, FeatureFlagMapper mapper) {
    this.flagService = flagService;
    this.mapper = mapper;
  }

  @GetMapping
  public List<FeatureFlagResponse> list(@RequestParam(required = false) String environment) {
    List<FeatureFlag> flags =
        environment == null
            ? flagService.getAllFlags()
            : flagService.getFlagsByEnvironment(environment);
    return flags.stream().map(mapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  public FeatureFlagResponse get(@PathVariable Long id) {
    return mapper.toResponse(requireFlag(id));
  }

  @GetMapping("/by-name/{name}")
  public FeatureFlagResponse getByName(@PathVariable String name) {
    return flagService
        .getFlagByName(name)
        .map(mapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Feature flag", name));
  }

  @PostMapping
  public ResponseEntity<FeatureFlagResponse> create(
      @Valid @RequestBody FeatureFlagRequest request) {
    FeatureFlag saved =
        flagService.createFeatureFlag(
            request.name(),
            request.description(),
            request.enabled(),
            request.status(),
            request.environment(),
            request.rolloutPercentage(),
            request.createdBy());
    return ResponseEntity.created(URI.create("/api/v1/flags/" + saved.getId()))
        .body(mapper.toResponse(saved));
  }

  /**
   * Updates the descriptive fields only. Enabling a flag and changing its rollout are separate
   * endpoints because they are operational actions with different blast radius, and folding them
   * into a general update makes an accidental production toggle a one field mistake.
   */
  @PutMapping("/{id}")
  public FeatureFlagResponse update(
      @PathVariable Long id, @Valid @RequestBody FeatureFlagRequest request) {
    requireFlag(id);
    return mapper.toResponse(
        flagService.updateFeatureFlag(
            id, request.name(), request.description(), request.environment()));
  }

  @PostMapping("/{id}/toggle")
  public FeatureFlagResponse toggle(@PathVariable Long id) {
    requireFlag(id);
    return mapper.toResponse(flagService.toggleFlag(id));
  }

  @PutMapping("/{id}/rollout")
  public FeatureFlagResponse updateRollout(
      @PathVariable Long id, @Valid @RequestBody RolloutUpdateRequest request) {
    requireFlag(id);
    return mapper.toResponse(flagService.updateRolloutPercentage(id, request.rolloutPercentage()));
  }

  /**
   * Archives the flag rather than removing the row.
   *
   * <p>A deleted flag would take its evaluation history with it, so the record is retained with
   * status ARCHIVED and disabled. The resource stays fetchable, which is deliberate: an operator
   * investigating an old incident needs to see the flag that caused it.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    requireFlag(id);
    flagService.deleteFeatureFlag(id);
    return ResponseEntity.noContent().build();
  }

  private FeatureFlag requireFlag(Long id) {
    return flagService
        .getFlagById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Feature flag", id));
  }
}
