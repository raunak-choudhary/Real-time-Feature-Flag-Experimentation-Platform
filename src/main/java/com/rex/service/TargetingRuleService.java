package com.rex.service;

import com.rex.evaluation.TargetingRule;
import com.rex.model.TargetingRuleEntity;
import com.rex.repository.TargetingRuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads a flag's targeting rules in the order that decides which one wins. */
@Service
@Transactional(readOnly = true)
public class TargetingRuleService {

  private final TargetingRuleRepository ruleRepository;

  public TargetingRuleService(TargetingRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  public List<TargetingRule> rulesFor(Long flagId) {
    return ruleRepository.findByFeatureFlagIdOrderByRuleOrderAsc(flagId).stream()
        .map(TargetingRuleEntity::toRule)
        .toList();
  }
}
