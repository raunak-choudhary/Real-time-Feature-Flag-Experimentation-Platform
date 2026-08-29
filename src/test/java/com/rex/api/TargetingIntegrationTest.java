package com.rex.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.evaluation.RuleOperator;
import com.rex.model.FeatureFlag;
import com.rex.model.TargetingRuleEntity;
import com.rex.repository.TargetingRuleRepository;
import com.rex.service.FeatureFlagService;
import com.rex.support.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Targeting rules end to end.
 *
 * <p>The engine has been tested since Phase 2, but nothing persisted rules or loaded them, so the
 * feature was unreachable from the API. These tests exist so that cannot recur silently.
 */
@AutoConfigureMockMvc
@Transactional
class TargetingIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureFlagService flagService;
  @Autowired private TargetingRuleRepository ruleRepository;

  private FeatureFlag flagAtZeroRollout() {
    return flagService.createFeatureFlag(
        "targeted_" + System.nanoTime(),
        "targeted flag",
        true,
        FeatureFlag.FlagStatus.ACTIVE,
        "production",
        0,
        "suite@rex.com");
  }

  @Test
  @DisplayName("a stored rule admits a segment the rollout percentage would exclude")
  void storedRuleAdmitsSegment() throws Exception {
    FeatureFlag flag = flagAtZeroRollout();
    ruleRepository.save(
        new TargetingRuleEntity(
            flag.getId(), 0, "country", RuleOperator.IN, List.of("CA", "US"), true));

    mockMvc
        .perform(
            get("/api/v1/evaluate/" + flag.getName())
                .param("userId", "user_1")
                .param("environment", "production")
                .param("country", "CA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.reason").value("TARGETING_RULE_MATCH"));
  }

  @Test
  @DisplayName("a user outside the rule falls through to the rollout percentage")
  void nonMatchingUserFallsThrough() throws Exception {
    FeatureFlag flag = flagAtZeroRollout();
    ruleRepository.save(
        new TargetingRuleEntity(flag.getId(), 0, "country", RuleOperator.IN, List.of("CA"), true));

    mockMvc
        .perform(
            get("/api/v1/evaluate/" + flag.getName())
                .param("userId", "user_1")
                .param("environment", "production")
                .param("country", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.reason").value("ROLLOUT_EXCLUDED"));
  }

  @Test
  @DisplayName("rules are applied in stored order, so the first match wins")
  void firstStoredRuleWins() throws Exception {
    FeatureFlag flag = flagAtZeroRollout();
    ruleRepository.save(
        new TargetingRuleEntity(
            flag.getId(), 0, "plan", RuleOperator.EQUALS, List.of("free"), false));
    ruleRepository.save(
        new TargetingRuleEntity(flag.getId(), 1, "country", RuleOperator.IN, List.of("CA"), true));

    mockMvc
        .perform(
            get("/api/v1/evaluate/" + flag.getName())
                .param("userId", "user_1")
                .param("environment", "production")
                .param("plan", "free")
                .param("country", "CA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reason").value("TARGETING_RULE_EXCLUDED"));
  }

  @Test
  @DisplayName("a flag with no stored rules behaves exactly as before targeting existed")
  void noRulesIsUnchanged() throws Exception {
    FeatureFlag flag =
        flagService.createFeatureFlag(
            "untargeted_" + System.nanoTime(),
            "plain flag",
            true,
            FeatureFlag.FlagStatus.ACTIVE,
            "production",
            100,
            "suite@rex.com");

    mockMvc
        .perform(
            get("/api/v1/evaluate/" + flag.getName())
                .param("userId", "user_1")
                .param("environment", "production"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reason").value("ROLLOUT_INCLUDED"));
  }
}
