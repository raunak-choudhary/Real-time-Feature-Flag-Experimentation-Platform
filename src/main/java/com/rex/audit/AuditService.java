package com.rex.audit;

import com.rex.event.FlagChangedEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records configuration changes.
 *
 * <p>Listens on publish rather than after commit, and joins the caller's transaction. That is the
 * opposite choice to the broadcast listener and for the opposite reason: a broadcast about a change
 * that later rolls back is a lie told to clients, whereas an audit row must live or die with the
 * change it describes. A failed mutation therefore leaves no audit row, and a successful one can
 * never lack it.
 */
@Service
public class AuditService {

  private static final String FLAG_TARGET = "FEATURE_FLAG";

  private final AuditEventRepository auditRepository;

  public AuditService(AuditEventRepository auditRepository) {
    this.auditRepository = auditRepository;
  }

  @EventListener
  @Transactional(propagation = Propagation.REQUIRED)
  public void onFlagChanged(FlagChangedEvent event) {
    record(
        AuditEvent.builder(actorFor(event), event.changeType().name(), FLAG_TARGET)
            .target(event.flagId(), event.flagName())
            .change(null, describe(event))
            .environment(event.environment())
            .build());
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public AuditEvent record(AuditEvent event) {
    return auditRepository.save(event);
  }

  /** Records an action the platform took on its own, such as an automatic rollback. */
  @Transactional(propagation = Propagation.REQUIRED)
  public AuditEvent recordAutomated(
      String action, Long targetId, String targetName, String before, String after, String reason) {
    return record(
        AuditEvent.builder(AuditEvent.SYSTEM_ACTOR, action, FLAG_TARGET)
            .target(targetId, targetName)
            .change(before, after)
            .reason(reason)
            .build());
  }

  @Transactional(readOnly = true)
  public List<AuditEvent> historyFor(Long flagId) {
    return auditRepository.findByTargetTypeAndTargetIdOrderByOccurredAtDesc(FLAG_TARGET, flagId);
  }

  @Transactional(readOnly = true)
  public List<AuditEvent> recent(int limit) {
    return auditRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit));
  }

  private static String actorFor(FlagChangedEvent event) {
    return event.changeType() == FlagChangedEvent.ChangeType.ROLLOUT_CHANGED
        ? AuditEvent.SYSTEM_ACTOR
        : "operator";
  }

  private static String describe(FlagChangedEvent event) {
    return "enabled=%s rollout=%d%%".formatted(event.enabled(), event.rolloutPercentage());
  }
}
