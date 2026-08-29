package com.rex.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.support.PostgresIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** The analysis endpoint over the full stack. */
@AutoConfigureMockMvc
@Transactional
class ExperimentAnalysisIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("an experiment with almost no data reports inconclusive rather than a verdict")
  void sparseExperimentIsInconclusive() throws Exception {
    mockMvc
        .perform(get("/api/v1/experiments/1/analysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.experimentId").value(1))
        .andExpect(jsonPath("$.canDeclareWinner").value(false))
        .andExpect(jsonPath("$.summary").value(Matchers.containsString("Inconclusive")));
  }

  @Test
  @DisplayName("the response carries the sample progress a dashboard needs, not just a boolean")
  void reportsProgressTowardTheRequiredSample() throws Exception {
    mockMvc
        .perform(get("/api/v1/experiments/1/analysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentPerVariant").exists())
        .andExpect(jsonPath("$.requiredPerVariant").exists())
        .andExpect(jsonPath("$.remainingPerVariant").exists())
        .andExpect(jsonPath("$.progress").exists())
        .andExpect(jsonPath("$.readyToConclude").value(false));
  }

  @Test
  @DisplayName("the response names both variants so a reader knows which side is which")
  void namesBothVariants() throws Exception {
    mockMvc
        .perform(get("/api/v1/experiments/1/analysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.controlVariant").isNotEmpty())
        .andExpect(jsonPath("$.testVariant").isNotEmpty());
  }

  @Test
  @DisplayName("analysis of an unknown experiment returns 404 as a problem detail")
  void unknownExperimentReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/v1/experiments/999999/analysis"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }
}
