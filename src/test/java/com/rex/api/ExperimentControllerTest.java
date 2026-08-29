package com.rex.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rex.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Exercises the experiment endpoints over the full stack against a real Postgres. */
@AutoConfigureMockMvc
@Transactional
class ExperimentControllerTest extends PostgresIntegrationTest {

  private static final String BASE = "/api/v1/experiments";

  @Autowired private MockMvc mockMvc;

  private static String payload(String name, int traffic) {
    return """
        {"name":"%s","description":"created by the suite",\
        "hypothesis":"the test variant converts better","successMetric":"conversion",\
        "trafficPercentage":%d,"controlVariantName":"control","testVariantName":"test",\
        "confidenceLevel":95.0,"minimumSampleSize":100,"expectedImprovement":5.0,\
        "environment":"controller-test","createdBy":"suite@rex.com"}"""
        .formatted(name, traffic);
  }

  private String create(String name) throws Exception {
    return mockMvc
        .perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload(name, 100)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private long idOf(String body) {
    return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
  }

  @Test
  @DisplayName("a created experiment is returned with a location and starts as a draft")
  void createReturnsTheExperiment() throws Exception {
    mockMvc
        .perform(
            post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload("ctrl_create", 100)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("ctrl_create"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  @DisplayName("an invalid traffic percentage is rejected with field level detail")
  void invalidTrafficIsRejected() throws Exception {
    mockMvc
        .perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload("ctrl_bad", 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
  }

  @Test
  @DisplayName("listing filters by environment when one is given")
  void listFiltersByEnvironment() throws Exception {
    create("ctrl_list");

    mockMvc
        .perform(get(BASE).param("environment", "controller-test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].environment").value("controller-test"));
    mockMvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("a single experiment is retrievable and an unknown one is a 404")
  void getOneAndUnknown() throws Exception {
    long id = idOf(create("ctrl_get"));

    mockMvc
        .perform(get(BASE + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
    mockMvc.perform(get(BASE + "/999999")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("a draft cannot be started until it has been marked ready")
  void draftCannotBeStartedDirectly() throws Exception {
    long id = idOf(create("ctrl_draft_start"));

    // The reason the ready endpoint exists. Without it a draft created through the API could never
    // reach running through the API, because start refuses anything that is not ready or paused.
    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isConflict());
  }

  @Test
  @DisplayName("the lifecycle endpoints move the experiment through its states")
  void lifecycleEndpointsMoveTheExperiment() throws Exception {
    long id = idOf(create("ctrl_lifecycle"));

    mockMvc
        .perform(post(BASE + "/" + id + "/ready"))
        .andExpect(jsonPath("$.status").value("READY"));
    mockMvc
        .perform(post(BASE + "/" + id + "/start"))
        .andExpect(jsonPath("$.status").value("RUNNING"));
    mockMvc
        .perform(post(BASE + "/" + id + "/pause"))
        .andExpect(jsonPath("$.status").value("PAUSED"));
    mockMvc.perform(post(BASE + "/" + id + "/stop")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("starting an experiment that is already running is refused as a conflict")
  void doubleStartIsAConflict() throws Exception {
    long id = idOf(create("ctrl_double_start"));
    mockMvc.perform(post(BASE + "/" + id + "/ready")).andExpect(status().isOk());
    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isOk());

    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isConflict());
  }

  @Test
  @DisplayName("a user can be assigned and the assignment read back")
  void assignAndReadBack() throws Exception {
    long id = idOf(create("ctrl_assign"));
    mockMvc.perform(post(BASE + "/" + id + "/ready")).andExpect(status().isOk());
    mockMvc.perform(post(BASE + "/" + id + "/start")).andExpect(status().isOk());

    mockMvc
        .perform(
            post(BASE + "/" + id + "/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"user-1\",\"sessionId\":\"s\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.variantName").exists());
    mockMvc
        .perform(get(BASE + "/" + id + "/assignments/user-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("user-1"));
  }

  @Test
  @DisplayName("a request with no body is a client error, not a server fault")
  void missingBodyIsAClientError() throws Exception {
    long id = idOf(create("ctrl_no_body"));

    mockMvc
        .perform(post(BASE + "/" + id + "/assignments").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Malformed request body"));
  }

  @Test
  @DisplayName("a body that is not valid json is a client error")
  void unparseableBodyIsAClientError() throws Exception {
    long id = idOf(create("ctrl_bad_json"));

    mockMvc
        .perform(
            post(BASE + "/" + id + "/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("the analysis endpoint answers before there is enough data")
  void analysisAnswersWithoutEnoughData() throws Exception {
    long id = idOf(create("ctrl_analysis"));

    mockMvc
        .perform(get(BASE + "/" + id + "/analysis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("INSUFFICIENT_DATA"));
  }

  @Test
  @DisplayName("deleting returns no content")
  void deleteReturnsNoContent() throws Exception {
    long id = idOf(create("ctrl_delete"));

    mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
  }
}
