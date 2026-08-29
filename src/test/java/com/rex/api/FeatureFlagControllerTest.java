package com.rex.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the flag endpoints over the full stack against a real Postgres. */
@AutoConfigureMockMvc
@Transactional
class FeatureFlagControllerTest extends PostgresIntegrationTest {

  private static final String BASE = "/api/v1/flags";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String body(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private static String createPayload(String name, int rollout) {
    return """
        {"name":"%s","description":"seeded by test","enabled":true,\
        "status":"ACTIVE","rolloutPercentage":%d,"environment":"test-env",\
        "createdBy":"suite@rex.com"}"""
        .formatted(name, rollout);
  }

  @Test
  @DisplayName("creating a flag returns 201 with a Location header and the created resource")
  void createReturnsCreated() throws Exception {
    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("create_me", 40)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.name").value("create_me"))
        .andExpect(jsonPath("$.rolloutPercentage").value(40))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  @DisplayName("listing returns the seeded flags")
  void listReturnsFlags() throws Exception {
    mockMvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("fetching a flag by name returns it")
  void getByName() throws Exception {
    mockMvc
        .perform(get(BASE + "/by-name/dark_mode"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("dark_mode"));
  }

  @Test
  @DisplayName("toggling flips enabled and returns the updated resource")
  void toggleFlipsEnabled() throws Exception {
    String created =
        mockMvc
            .perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("toggle_me", 10)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = objectMapper.readTree(created).get("id").asLong();

    mockMvc
        .perform(post(BASE + "/" + id + "/toggle"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  @DisplayName("updating the rollout percentage persists the new value")
  void updateRollout() throws Exception {
    String created =
        mockMvc
            .perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("rollout_me", 5)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = objectMapper.readTree(created).get("id").asLong();

    mockMvc
        .perform(
            put(BASE + "/" + id + "/rollout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(new java.util.HashMap<>(java.util.Map.of("rolloutPercentage", 65)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rolloutPercentage").value(65));
  }

  @Test
  @DisplayName("deleting archives the flag and disables it, rather than losing its history")
  void deleteArchivesFlag() throws Exception {
    String created =
        mockMvc
            .perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("delete_me", 0)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = objectMapper.readTree(created).get("id").asLong();

    mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  @DisplayName("an unknown flag returns a 404 problem detail, not a stack trace")
  void unknownFlagReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(get(BASE + "/999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value(containsString("not-found")))
        .andExpect(jsonPath("$.title").value("Resource not found"))
        .andExpect(jsonPath("$.detail").value(containsString("999999")))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  @Test
  @DisplayName("a duplicate name returns 409, not a 500 from a database constraint")
  void duplicateNameReturnsConflict() throws Exception {
    mockMvc
        .perform(
            post(BASE).contentType(MediaType.APPLICATION_JSON).content(createPayload("dupe_me", 0)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(BASE).contentType(MediaType.APPLICATION_JSON).content(createPayload("dupe_me", 0)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Resource already exists"));
  }

  @Test
  @DisplayName("an out of range rollout returns 400 with the offending field named")
  void invalidRolloutReturnsFieldError() throws Exception {
    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("bad_rollout", 150)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"))
        .andExpect(jsonPath("$.errors.rolloutPercentage").exists());
  }

  @Test
  @DisplayName("a malformed flag name returns 400 rather than creating an unusable key")
  void invalidNameReturnsFieldError() throws Exception {
    mockMvc
        .perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("Bad Name", 10)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
  }
}
