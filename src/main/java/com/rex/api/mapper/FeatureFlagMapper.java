package com.rex.api.mapper;

import com.rex.api.dto.FeatureFlagResponse;
import com.rex.model.FeatureFlag;
import org.springframework.stereotype.Component;

/** Translates between the feature flag entity and its API representation. */
@Component
public class FeatureFlagMapper {

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
