package com.rex.event;

import java.time.Instant;

/**
 * Published whenever a flag's served behaviour changes.
 *
 * <p>Lives in a neutral package so the service layer can publish without depending on the
 * transport. Services know that something changed; they do not know a WebSocket exists.
 */
public record FlagChangedEvent(
    Long flagId,
    String flagName,
    String environment,
    boolean enabled,
    int rolloutPercentage,
    ChangeType changeType,
    Instant occurredAt) {

  public static FlagChangedEvent of(
      Long flagId,
      String flagName,
      String environment,
      boolean enabled,
      int rolloutPercentage,
      ChangeType changeType) {
    return new FlagChangedEvent(
        flagId, flagName, environment, enabled, rolloutPercentage, changeType, Instant.now());
  }

  /** What kind of change occurred, so a client can react differently to a kill switch. */
  public enum ChangeType {
    CREATED,
    TOGGLED,
    ROLLOUT_CHANGED,
    UPDATED,
    ARCHIVED
  }
}
