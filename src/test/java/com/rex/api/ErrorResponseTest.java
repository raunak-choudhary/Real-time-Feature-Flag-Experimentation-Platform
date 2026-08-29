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

/**
 * Pins the status codes the API returns for failures a client can cause.
 *
 * <p>These are easy to regress silently, because a catch-all exception handler will happily answer
 * every one of them with a 500 and the response still looks well formed.
 */
@AutoConfigureMockMvc
@Transactional
class ErrorResponseTest extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("an unmapped path is a client error, not a server fault")
  void unmappedPathReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Endpoint not found"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("an unmapped path outside the API namespace is also a 404")
  void unmappedPathOutsideApiNamespaceReturnsNotFound() throws Exception {
    mockMvc.perform(get("/totally/unknown")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("a missing resource under a mapped path stays a 404")
  void missingResourceReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/flags/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Resource not found"));
  }

  @Test
  @DisplayName("errors carry the RFC 7807 problem shape")
  void errorsUseProblemDetailShape() throws Exception {
    mockMvc
        .perform(get("/api/v1/flags/99999"))
        .andExpect(jsonPath("$.type").exists())
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.detail").exists());
  }
}
