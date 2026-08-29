package com.rex.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The SDK facing evaluation endpoint. */
@AutoConfigureMockMvc
@Transactional
class EvaluationControllerTest extends PostgresIntegrationTest {

  private static final String BASE = "/api/v1/evaluate";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("a flag at full rollout evaluates on and says why")
  void fullRolloutIsOn() throws Exception {
    mockMvc
        .perform(
            get(BASE + "/dark_mode").param("userId", "user_001").param("environment", "production"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.flagName").value("dark_mode"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.reason").value("ROLLOUT_INCLUDED"));
  }

  @Test
  @DisplayName("a disabled flag reports the kill switch rather than the bucket")
  void disabledFlagReportsDisabled() throws Exception {
    mockMvc
        .perform(
            get(BASE + "/new_checkout_flow")
                .param("userId", "user_001")
                .param("environment", "production"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.reason").value("FLAG_DISABLED"));
  }

  @Test
  @DisplayName("asking for a production flag from development reports the mismatch")
  void environmentMismatchIsReported() throws Exception {
    mockMvc
        .perform(
            get(BASE + "/dark_mode")
                .param("userId", "user_001")
                .param("environment", "development"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.reason").value("ENVIRONMENT_MISMATCH"));
  }

  @Test
  @DisplayName("an unknown flag returns a documented off decision, never a 404")
  void unknownFlagReturnsOffNotError() throws Exception {
    mockMvc
        .perform(get(BASE + "/never_created").param("userId", "user_001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.reason").value("FLAG_NOT_FOUND"));
  }

  @Test
  @DisplayName("the same user and flag evaluate identically across repeated calls")
  void evaluationIsDeterministic() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      mockMvc
          .perform(
              get(BASE + "/premium_features")
                  .param("userId", "stable_user")
                  .param("environment", "production"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.flagName").value("premium_features"));
    }
  }

  @Test
  @DisplayName("bulk evaluation lets an SDK bootstrap its whole cache in one call")
  void bulkEvaluationReturnsEveryFlagInEnvironment() throws Exception {
    mockMvc
        .perform(get(BASE).param("userId", "user_001").param("environment", "production"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].reason").exists());
  }
}
