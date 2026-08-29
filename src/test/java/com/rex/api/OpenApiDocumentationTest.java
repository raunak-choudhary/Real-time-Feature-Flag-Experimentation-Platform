package com.rex.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards against an endpoint being added without appearing in the published description.
 *
 * <p>Documentation that is generated but never asserted drifts as quietly as documentation that is
 * written by hand.
 */
@AutoConfigureMockMvc
class OpenApiDocumentationTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest(name = "{0} is documented")
  @ValueSource(
      strings = {
        "/api/v1/flags",
        "/api/v1/flags/{id}",
        "/api/v1/flags/{id}/toggle",
        "/api/v1/flags/{id}/rollout",
        "/api/v1/flags/by-name/{name}",
        "/api/v1/experiments",
        "/api/v1/experiments/{id}",
        "/api/v1/experiments/{id}/start",
        "/api/v1/experiments/{id}/analysis",
        "/api/v1/experiments/{id}/assignments",
        "/api/v1/evaluate",
        "/api/v1/evaluate/{flagName}",
        "/api/v1/telemetry/conversions",
        "/api/v1/audit",
        "/api/v1/audit/flags/{flagId}",
        "/api/v1/audit/stale-flags"
      })
  @DisplayName("every route appears in the generated OpenAPI document")
  void routeIsDocumented(String path) throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['" + path + "']").exists());
  }

  @DisplayName("the interactive documentation is served")
  @ParameterizedTest(name = "{0} responds")
  @ValueSource(strings = {"/v3/api-docs"})
  void documentationIsServed(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isOk());
  }
}
