package com.rex.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rex.event.FlagChangedEvent;
import com.rex.service.FeatureFlagService;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Measures how long a toggle takes to reach a connected client.
 *
 * <p>Tagged {@code performance} and excluded from the blocking gate. A shared CI runner under
 * contention produces timing noise, and a build that goes red for reasons unrelated to the change
 * teaches people to ignore the build. It runs in its own non blocking job instead, and the figure
 * quoted in the README is measured on known hardware and labelled as such.
 */
@Tag("performance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PropagationLatencyTest {

  private static final int TRIALS = 50;
  private static final long P95_CEILING_MILLIS = 1_000;

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("rexdb_latency")
          .withUsername("rex_test")
          .withPassword("rex_test");

  static {
    POSTGRES.start();
    System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
    System.setProperty("spring.datasource.username", POSTGRES.getUsername());
    System.setProperty("spring.datasource.password", POSTGRES.getPassword());
  }

  @LocalServerPort private int port;
  @Autowired private FeatureFlagService flagService;

  private WebSocketStompClient stompClient;
  private StompSession session;

  @BeforeEach
  void connect() throws Exception {
    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
    stompClient.setMessageConverter(converter);
    session =
        stompClient
            .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
            .get(10, TimeUnit.SECONDS);
  }

  @AfterEach
  void disconnect() {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
    stompClient.stop();
  }

  @Test
  @DisplayName("p95 toggle to client latency stays under the documented ceiling")
  void propagationLatency() throws Exception {
    BlockingQueue<Long> arrivals = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/flags/production",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return FlagChangedEvent.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            arrivals.add(System.nanoTime());
          }
        });
    Thread.sleep(500);

    Long flagId = flagService.getFlagByName("dark_mode").orElseThrow().getId();
    List<Long> latenciesMillis = new ArrayList<>(TRIALS);

    for (int trial = 0; trial < TRIALS; trial++) {
      arrivals.clear();
      long sentAt = System.nanoTime();
      flagService.toggleFlag(flagId);

      Long arrivedAt = arrivals.poll(5, TimeUnit.SECONDS);
      assertThat(arrivedAt).as("trial %d received no message", trial).isNotNull();
      latenciesMillis.add((arrivedAt - sentAt) / 1_000_000);
    }

    latenciesMillis.sort(Long::compareTo);
    long p50 = latenciesMillis.get(TRIALS / 2);
    long p95 = latenciesMillis.get((int) (TRIALS * 0.95) - 1);
    long max = latenciesMillis.get(TRIALS - 1);

    System.out.printf(
        "%n  propagation latency over %d trials: p50 %dms, p95 %dms, max %dms%n",
        TRIALS, p50, p95, max);

    assertThat(p95)
        .as("p95 propagation latency over %d trials", TRIALS)
        .isLessThan(P95_CEILING_MILLIS);
  }
}
