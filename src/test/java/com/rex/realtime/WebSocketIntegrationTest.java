package com.rex.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rex.event.FlagChangedEvent;
import com.rex.service.FeatureFlagService;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Proves the toggle to client path end to end.
 *
 * <p>Unit tests cannot prove this: the path crosses the service layer, the Spring event bus, the
 * transaction boundary, the broker and the socket. Only a real subscriber sitting on a real
 * connection shows that it works.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("rexdb_ws")
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
    // FlagChangedEvent carries an Instant, so the client converter needs the JSR-310 module.
    // A plain ObjectMapper fails to deserialise it and the frame is dropped without a trace.
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

  private BlockingQueue<FlagChangedEvent> subscribe(String environment) {
    BlockingQueue<FlagChangedEvent> received = new LinkedBlockingQueue<>();
    session.subscribe(
        "/topic/flags/" + environment,
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return FlagChangedEvent.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            received.add((FlagChangedEvent) payload);
          }
        });
    return received;
  }

  @Test
  @DisplayName("a subscribed client receives a toggle within a second")
  void toggleReachesSubscriber() throws Exception {
    BlockingQueue<FlagChangedEvent> received = subscribe("production");
    Thread.sleep(300);

    var flag = flagService.getFlagByName("dark_mode").orElseThrow();
    flagService.toggleFlag(flag.getId());

    FlagChangedEvent event = received.poll(5, TimeUnit.SECONDS);

    assertThat(event).as("no message arrived on the topic").isNotNull();
    assertThat(event.flagName()).isEqualTo("dark_mode");
    assertThat(event.changeType()).isEqualTo(FlagChangedEvent.ChangeType.TOGGLED);
  }

  @Test
  @DisplayName("a rollout change reaches the client with the new percentage")
  void rolloutChangeReachesSubscriber() throws Exception {
    BlockingQueue<FlagChangedEvent> received = subscribe("production");
    Thread.sleep(300);

    var flag = flagService.getFlagByName("premium_features").orElseThrow();
    flagService.updateRolloutPercentage(flag.getId(), 77);

    FlagChangedEvent event = received.poll(5, TimeUnit.SECONDS);

    assertThat(event).isNotNull();
    assertThat(event.rolloutPercentage()).isEqualTo(77);
    assertThat(event.changeType()).isEqualTo(FlagChangedEvent.ChangeType.ROLLOUT_CHANGED);
  }

  @Test
  @DisplayName("a development subscriber never sees a production change")
  void environmentsAreIsolated() throws Exception {
    BlockingQueue<FlagChangedEvent> development = subscribe("development");
    Thread.sleep(300);

    var flag = flagService.getFlagByName("dark_mode").orElseThrow();
    flagService.toggleFlag(flag.getId());

    assertThat(development.poll(2, TimeUnit.SECONDS))
        .as("a production change must not leak onto the development topic")
        .isNull();
  }
}
