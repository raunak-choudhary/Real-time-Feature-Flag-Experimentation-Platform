package com.rex.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import com.rex.support.PostgresIntegrationTest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the data the statistics engine and the guardrails depend on is actually produced.
 *
 * <p>Not transactional, because exposures are written on a separate thread in their own
 * transaction, and a test transaction would hide them.
 */
@AutoConfigureMockMvc
class TelemetryIntegrationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MetricsRepository metricsRepository;

  @Test
  @DisplayName("an evaluation records an exposure carrying the served decision and rollout")
  void evaluationRecordsExposure() throws Exception {
    String userId = "exposure_probe_" + System.nanoTime();

    mockMvc
        .perform(
            get("/api/v1/evaluate/dark_mode")
                .param("userId", userId)
                .param("environment", "production"))
        .andExpect(status().isOk());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<Metrics> recorded = metricsRepository.findByUserId(userId);
              assertThat(recorded).isNotEmpty();
              Metrics exposure = recorded.get(0);
              assertThat(exposure.getEventType()).isEqualTo(Metrics.EventType.FLAG_EXPOSURE);
              assertThat(exposure.getServedDecision())
                  .as("the decision served must be recorded, or guardrails cannot find the cohort")
                  .isNotNull();
              assertThat(exposure.getRolloutAtExposure()).isNotNull();
            });
  }

  @Test
  @DisplayName("exposures are queryable by served decision, which the guardrail sweep depends on")
  void exposuresAreFilterableByDecision() throws Exception {
    String userId = "cohort_probe_" + System.nanoTime();

    mockMvc
        .perform(
            get("/api/v1/evaluate/dark_mode")
                .param("userId", userId)
                .param("environment", "production"))
        .andExpect(status().isOk());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(metricsRepository.findByUserId(userId))
                    .isNotEmpty()
                    .allSatisfy(metric -> assertThat(metric.getServedDecision()).isNotNull()));
  }

  @Test
  @DisplayName(
      "a conversion from a user who was never enrolled is rejected, not attributed to control")
  void conversionWithoutAssignmentIsRejected() throws Exception {
    String payload =
        """
        {"userId":"never_enrolled_user","experimentId":1,"eventName":"purchase","value":49.99}""";

    mockMvc
        .perform(
            post("/api/v1/telemetry/conversions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("a conversion for an unknown experiment returns 404")
  void conversionForUnknownExperimentIsRejected() throws Exception {
    String payload =
        """
        {"userId":"user_001","experimentId":999999,"eventName":"purchase","value":1.0}""";

    mockMvc
        .perform(
            post("/api/v1/telemetry/conversions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("a conversion missing its user id is rejected with a field level message")
  void conversionMissingUserIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/telemetry/conversions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"experimentId\":1,\"value\":1.0}"))
        .andExpect(status().isBadRequest());
  }
}
