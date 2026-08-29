package com.rex.api.mapper;

import com.rex.api.dto.FeatureFlagRequest;
import com.rex.api.dto.FeatureFlagResponse;
import com.rex.model.FeatureFlag;
import org.springframework.stereotype.Component;

/** Translates between the feature flag entity and its API representation. */
@Component
public class FeatureFlagMapper {

  private static final String DEFAULT_ENVIRONMENT = "development";

  public FeatureFlag toEntity(FeatureFlagRequest request) {
    FeatureFlag flag = new FeatureFlag();
    applyTo(flag, request);
    return flag;
  }

  /** Copies request values onto an existing flag, leaving unset optional fields untouched. */
  public void applyTo(FeatureFlag flag, FeatureFlagRequest request) {
    flag.setName(request.name());
    flag.setDescription(request.description());
    flag.setEnabled(request.enabled() != null ? request.enabled() : Boolean.FALSE);
    if (request.status() != null) {
      flag.setStatus(request.status());
    }
    flag.setRolloutPercentage(
        request.rolloutPercentage() != null ? request.rolloutPercentage() : 0);
    flag.setEnvironment(
        request.environment() != null ? request.environment() : DEFAULT_ENVIRONMENT);
    if (request.createdBy() != null) {
      flag.setCreatedBy(request.createdBy());
    }
  }

  public FeatureFlagResponse toResponse(FeatureFlag flag) {
    return new FeatureFlagResponse(
        flag.getId(),
        flag.getName(),
        flag.getDescription(),
        Boolean.TRUE.equals(flag.getEnabled()),
        flag.getStatus(),
        flag.getRolloutPercentage() != null ? flag.getRolloutPercentage() : 0,
        flag.getEnvironment(),
        flag.getCreatedBy(),
        flag.getCreatedAt(),
        flag.getUpdatedAt());
  }
}
