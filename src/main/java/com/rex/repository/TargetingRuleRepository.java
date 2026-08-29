package com.rex.repository;

import com.rex.model.TargetingRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Targeting rules, always read in order so first match wins deterministically. */
public interface TargetingRuleRepository extends JpaRepository<TargetingRuleEntity, Long> {

  List<TargetingRuleEntity> findByFeatureFlagIdOrderByRuleOrderAsc(Long featureFlagId);
}
