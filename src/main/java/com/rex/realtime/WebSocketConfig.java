package com.rex.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket, with a SockJS fallback for clients behind proxies that mangle upgrades.
 *
 * <p>Topics are namespaced by environment so a development client never receives production
 * traffic. Allowed origins come from configuration for the same reason the REST layer does: the
 * dashboard is served from a different origin, and a wildcard here would accept a socket from
 * anywhere.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  /** Clients subscribe beneath this prefix; the in memory broker owns delivery. */
  public static final String TOPIC_PREFIX = "/topic";

  public static final String FLAGS_DESTINATION = TOPIC_PREFIX + "/flags/";

  private final String[] allowedOrigins;

  public WebSocketConfig(@Value("${rex.cors.allowed-origins}") String allowedOrigins) {
    this.allowedOrigins = allowedOrigins.split(",");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker(TOPIC_PREFIX);
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Raw WebSocket for the SDK, which does not need the fallback transports.
    registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    // SockJS on its own path for browsers behind proxies that mangle the upgrade. Registering
    // both on the same path shadows one of them.
    registry.addEndpoint("/ws-sockjs").setAllowedOrigins(allowedOrigins).withSockJS();
  }
}
