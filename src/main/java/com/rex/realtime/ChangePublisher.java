package com.rex.realtime;

import com.rex.event.FlagChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Translates a domain change into a broker message.
 *
 * <p>Listens after commit, not on publish. Broadcasting before the transaction commits would tell
 * clients about a change that could still roll back, leaving every connected SDK holding a value
 * the database never had.
 *
 * <p>Broker failures are logged and swallowed. A client that misses a message refetches on its next
 * reconnect, whereas letting the failure propagate would fail the mutation that triggered it, so a
 * broker outage would stop operators changing flags at exactly the moment they most need to.
 */
@Component
public class ChangePublisher {

  private static final Logger logger = LoggerFactory.getLogger(ChangePublisher.class);

  private final SimpMessagingTemplate messagingTemplate;

  public ChangePublisher(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFlagChanged(FlagChangedEvent event) {
    broadcast(event);
  }

  /** Direct broadcast for callers outside a transaction, such as the rollout scheduler. */
  public void broadcast(FlagChangedEvent event) {
    String destination = WebSocketConfig.FLAGS_DESTINATION + event.environment();
    try {
      messagingTemplate.convertAndSend(destination, event);
      logger.debug(
          "Broadcast {} for flag '{}' to {}", event.changeType(), event.flagName(), destination);
    } catch (RuntimeException exception) {
      logger.warn(
          "Failed to broadcast change for flag '{}'; clients will resynchronise on reconnect",
          event.flagName(),
          exception);
    }
  }
}
